package com.connecthub.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connecthub.app.AppContainer

class ChatViewModelFactory(
    private val container: AppContainer,
    private val conversationId: String,
    private val receiverId: String,
    private val contactName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            "ChatViewModelFactory can only create ChatViewModel"
        }
        return ChatViewModel(
            conversationId = conversationId,
            receiverId = receiverId,
            contactName = contactName,
            sendMessageUseCase = container.sendMessageUseCase,
            observeMessagesUseCase = container.observeMessagesUseCase,
            authRepository = container.authRepository,
            messageRepository = container.messageRepository
        ) as T
    }
}
