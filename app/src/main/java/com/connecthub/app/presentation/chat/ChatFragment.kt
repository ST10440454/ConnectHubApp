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

    private val conversationId: String by lazy { requireArguments().getString("conversationId").orEmpty() }
    private val contactId: String by lazy { requireArguments().getString("contactId").orEmpty() }
    private val contactName: String by lazy { requireArguments().getString("contactName").orEmpty() }

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
            findNavController().popBackStack()
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
                        adapter.submitList(state.messages) {
                            if (state.messages.isNotEmpty()) {
                                binding.recyclerMessages.scrollToPosition(state.messages.size - 1)
                            }
                        }
                        if (binding.editMessage.text.toString() != state.draft) {
                            binding.editMessage.setText(state.draft)
                            binding.editMessage.setSelection(state.draft.length)
                        }
                        binding.buttonSend.isEnabled = !state.isSending && state.draft.isNotBlank()
                        binding.buttonAttachImage.isEnabled = !state.isSending
                    }
                }
                launch {
                    viewModel.errors.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
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
