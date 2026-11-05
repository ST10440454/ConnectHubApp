package com.connecthub.app.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.domain.model.ConversationPreview
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.domain.usecase.GetDeviceContactMatchesUseCase
import com.connecthub.app.domain.usecase.ObserveConversationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ChatListState(
    val conversations: List<ConversationPreview> = emptyList(),
    val currentUserName: String? = null,
    val isLoading: Boolean = true
)

class ChatListViewModel(
    observeConversationsUseCase: ObserveConversationsUseCase,
    authRepository: AuthRepository,
    private val getDeviceContactMatchesUseCase: GetDeviceContactMatchesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatListState(currentUserName = authRepository.currentUser()?.displayName)
    )
    val state: StateFlow<ChatListState> = _state

    init {
        viewModelScope.launch {
            observeConversationsUseCase()
                .catch {
                    _state.value = _state.value.copy(isLoading = false)
                }
                .collect { conversations ->
                    _state.value = _state.value.copy(conversations = conversations, isLoading = false)
                }
        }
    }

    fun refreshContacts() {
        viewModelScope.launch {
            runCatching { getDeviceContactMatchesUseCase() }
        }
    }
}
