package com.connecthub.app.ui.settings

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
 * Note: language selection is NOT handled here. Unlike dark mode/notifications,
 * which are app-specific preferences this ViewModel owns via SettingsDataStore,
 * the UI language is a system-level concern handled directly by SettingsFragment
 * through AppCompatDelegate.setApplicationLocales() — Android's per-app language
 * preferences API persists and applies it automatically, so there's no app-level
 * state to manage here.
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
