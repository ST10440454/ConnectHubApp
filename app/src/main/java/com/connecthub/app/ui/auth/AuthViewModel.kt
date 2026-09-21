package com.connecthub.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.connecthub.app.domain.usecase.LoginUserUseCase
import com.connecthub.app.domain.usecase.RegisterUserUseCase
import com.connecthub.app.util.AppResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AuthState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class AuthIntent {
    data class NameChanged(val value: String) : AuthIntent()
    data class EmailChanged(val value: String) : AuthIntent()
    data class PasswordChanged(val value: String) : AuthIntent()
    object SubmitRegister : AuthIntent()
    object SubmitLogin : AuthIntent()
    object DismissError : AuthIntent()
}

sealed class AuthEffect {
    object NavigateToChat : AuthEffect()
}

/**
 * Same MVI contract used by both LoginFragment and RegisterFragment.
 * Each fragment gets its own instance via the ViewModelFactory (fragment-scoped),
 * so login state and register state never bleed into each other.
 */
class AuthViewModel(
    private val registerUserUseCase: RegisterUserUseCase,
    private val loginUserUseCase: LoginUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.NameChanged -> _state.value = _state.value.copy(displayName = intent.value)
            is AuthIntent.EmailChanged -> _state.value = _state.value.copy(email = intent.value)
            is AuthIntent.PasswordChanged -> _state.value = _state.value.copy(password = intent.value)
            AuthIntent.DismissError -> _state.value = _state.value.copy(errorMessage = null)
            AuthIntent.SubmitRegister -> submitRegister()
            AuthIntent.SubmitLogin -> submitLogin()
        }
    }

    private fun submitRegister() {
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, errorMessage = null)
            when (val result = registerUserUseCase(current.displayName, current.email, current.password)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.send(AuthEffect.NavigateToChat)
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    private fun submitLogin() {
        val current = _state.value
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, errorMessage = null)
            when (val result = loginUserUseCase(current.email, current.password)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(isLoading = false)
                    _effects.send(AuthEffect.NavigateToChat)
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}
