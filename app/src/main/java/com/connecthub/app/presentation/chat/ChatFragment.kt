package com.connecthub.app.presentation.chat

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.databinding.FragmentChatBinding
import com.connecthub.app.presentation.common.addTextChangedListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val conversationId: String by lazy { arguments?.getString("conversationId").orEmpty() }
    private val contactId: String by lazy { arguments?.getString("contactId").orEmpty() }
    private val contactName: String by lazy { arguments?.getString("contactName").orEmpty() }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            (requireActivity().application as ConnectHubApp).container,
            conversationId,
            contactId,
            contactName
        )
    }

    private lateinit var adapter: MessageAdapter

    // Android's built-in Photo Picker — needs NO storage/media permission on any
    // supported API level (it runs in a separate, system-owned process and hands
    // back only the specific image the user picked).
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            readAndSendImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.title = contactName
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        adapter = MessageAdapter(currentUserId = viewModel.state.value.currentUserId)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter

        binding.editMessage.addTextChangedListener {
            viewModel.onIntent(ChatIntent.DraftChanged(it))
        }
        binding.buttonSend.setOnClickListener {
            viewModel.onIntent(ChatIntent.SendMessage)
        }
        binding.buttonAttachImage.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect { state ->
                        val b = _binding ?: return@collect
                        adapter.submitList(state.messages) {
                            _binding?.let { validBinding ->
                                if (state.messages.isNotEmpty()) {
                                    validBinding.recyclerMessages.scrollToPosition(state.messages.size - 1)
                                }
                            }
                        }
                        if (b.editMessage.text.toString() != state.draft) {
                            b.editMessage.setText(state.draft)
                            val safeLen = state.draft.length.coerceAtMost(b.editMessage.text?.length ?: 0)
                            b.editMessage.setSelection(safeLen)
                        }
                        b.buttonSend.isEnabled = !state.isSending && state.draft.isNotBlank()
                        b.buttonAttachImage.isEnabled = !state.isSending
                    }
                }
                launch {
                    viewModel.errors.collect { message ->
                        val b = _binding ?: return@collect
                        Snackbar.make(b.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun readAndSendImage(uri: Uri) {
        val mimeType = requireContext().contentResolver.getType(uri) ?: "image/jpeg"
        viewLifecycleOwner.lifecycleScope.launch {
            val bytes = try {
                requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }
            if (bytes == null) {
                Snackbar.make(binding.root, "Couldn't read that image.", Snackbar.LENGTH_LONG).show()
                return@launch
            }
            viewModel.onIntent(ChatIntent.SendImage(bytes, mimeType))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
