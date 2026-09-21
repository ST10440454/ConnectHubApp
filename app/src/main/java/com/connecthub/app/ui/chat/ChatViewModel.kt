package com.connecthub.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.domain.model.Message
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.domain.repository.MessageRepository
import com.connecthub.app.domain.usecase.ObserveMessagesUseCase
import com.connecthub.app.domain.usecase.SendMessageUseCase
import com.connecthub.app.util.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class ChatState(
    val contactName: String = "",
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val currentUserId: String? = null,
    val isSending: Boolean = false
)

sealed class ChatIntent {
    data class DraftChanged(val value: String) : ChatIntent()
    object SendMessage : ChatIntent()
}

/**
 * One instance of this ViewModel is scoped to a single 1:1 conversation —
 * [conversationId] and [receiverId] are fixed for its lifetime, supplied by
 * [ChatViewModelFactory] from the Fragment's navigation arguments.
 */
class ChatViewModel(
    private val conversationId: String,
    private val receiverId: String,
    contactName: String,
    private val sendMessageUseCase: SendMessageUseCase,
    observeMessagesUseCase: ObserveMessagesUseCase,
    authRepository: AuthRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatState(contactName = contactName, currentUserId = authRepository.currentUser()?.uid)
    )
    val state: StateFlow<ChatState> = _state

    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    init {
        // Mark any messages sent to us in this conversation as read the moment
        // it's opened — this is what drives the blue double-tick on the sender's side.
        viewModelScope.launch {
            messageRepository.markMessagesAsRead(conversationId)
        }

        viewModelScope.launch {
            observeMessagesUseCase(conversationId).collect { messages ->
                _state.value = _state.value.copy(messages = messages)
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.DraftChanged -> _state.value = _state.value.copy(draft = intent.value)
            ChatIntent.SendMessage -> sendMessage()
        }
    }

    private fun sendMessage() {
        val content = _state.value.draft
        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            when (val result = sendMessageUseCase(conversationId, receiverId, content)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(draft = "", isSending = false)
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(isSending = false)
                    _errors.send(result.message)
                }
            }
        }
    }
}
