package com.connecthub.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.data.local.AppSettings
import com.connecthub.app.data.local.SettingsDataStore
import com.connecthub.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val userEmail: String? = null,
    val loggedOut: Boolean = false
)

sealed class SettingsIntent {
    data class ToggleDarkMode(val enabled: Boolean) : SettingsIntent()
    data class ToggleNotifications(val enabled: Boolean) : SettingsIntent()
    object LogOut : SettingsIntent()
}

/**
 * Note: language selection is NOT handled here. The UI language is owned by
 * LanguageManager (DeepL runtime translation, cached on the device and persisted in its own
 * SharedPreferences), which SettingsFragment calls directly.
 */
class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsState(userEmail = authRepository.currentUser()?.email)
    )
    val state: StateFlow<SettingsState> = _state

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleDarkMode -> viewModelScope.launch {
                settingsDataStore.setDarkMode(intent.enabled)
            }
            is SettingsIntent.ToggleNotifications -> viewModelScope.launch {
                settingsDataStore.setNotifications(intent.enabled)
            }
            SettingsIntent.LogOut -> {
                authRepository.logout()
                _state.value = _state.value.copy(loggedOut = true)
            }
        }
    }
}
