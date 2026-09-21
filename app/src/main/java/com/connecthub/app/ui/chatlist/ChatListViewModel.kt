package com.connecthub.app.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.domain.model.ConversationPreview
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.domain.usecase.ObserveConversationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatListState(
    val conversations: List<ConversationPreview> = emptyList(),
    val currentUserName: String? = null,
    val isLoading: Boolean = true
)

class ChatListViewModel(
    observeConversationsUseCase: ObserveConversationsUseCase,
    authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatListState(currentUserName = authRepository.currentUser()?.displayName)
    )
    val state: StateFlow<ChatListState> = _state

    init {
        viewModelScope.launch {
            observeConversationsUseCase().collect { conversations ->
                _state.value = _state.value.copy(conversations = conversations, isLoading = false)
            }
        }
    }
}
