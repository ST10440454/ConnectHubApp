package com.connecthub.app.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.domain.model.DeviceContactMatch
import com.connecthub.app.domain.usecase.GetDeviceContactMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NewChatState(
    val matches: List<DeviceContactMatch> = emptyList(),
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false,
    val hasLoadedOnce: Boolean = false
)

class NewChatViewModel(
    private val getDeviceContactMatchesUseCase: GetDeviceContactMatchesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NewChatState())
    val state: StateFlow<NewChatState> = _state

    /** Called once READ_CONTACTS has actually been granted, to load and match the phone book. */
    fun onPermissionGranted() {
        _state.value = _state.value.copy(isLoading = true, permissionDenied = false)
        viewModelScope.launch {
            val matches = getDeviceContactMatchesUseCase()
            _state.value = _state.value.copy(matches = matches, isLoading = false, hasLoadedOnce = true)
        }
    }

    fun onPermissionDenied() {
        _state.value = _state.value.copy(permissionDenied = true, isLoading = false, hasLoadedOnce = true)
    }
}
