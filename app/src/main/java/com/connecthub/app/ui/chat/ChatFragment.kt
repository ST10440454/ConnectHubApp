package com.connecthub.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.connecthub.app.ConnectHubApp
import com.connecthub.app.databinding.FragmentChatBinding
import com.connecthub.app.ui.common.addTextChangedListener
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
