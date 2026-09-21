package com.connecthub.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.connecthub.app.AppContainer
import com.connecthub.app.ui.auth.AuthViewModel
import com.connecthub.app.ui.chatlist.ChatListViewModel
import com.connecthub.app.ui.settings.SettingsViewModel

/**
 * Simple factory that wires use cases from [AppContainer] into ViewModels without Hilt.
 * Note: ChatViewModel is NOT created here — it needs per-conversation arguments
 * (conversationId, receiverId, contactName) from navigation, so it has its own
 * dedicated ChatViewModelFactory instead.
 */
class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(container.registerUserUseCase, container.loginUserUseCase) as T
            modelClass.isAssignableFrom(ChatListViewModel::class.java) ->
                ChatListViewModel(container.observeConversationsUseCase, container.authRepository) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.settingsDataStore, container.authRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
