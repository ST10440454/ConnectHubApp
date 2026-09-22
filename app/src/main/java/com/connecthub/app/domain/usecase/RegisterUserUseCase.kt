package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.User
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.util.AppResult
import com.connecthub.app.util.Validators

class RegisterUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        displayName: String,
        email: String,
        password: String,
        phoneNumber: String? = null
    ): AppResult<User> {
        val validation = Validators.validateRegistration(displayName, email, password)
        if (!validation.isValid) {
            return AppResult.Error(validation.errorMessage ?: "Invalid input.")
        }
        val trimmedPhone = phoneNumber?.trim()?.takeIf { it.isNotBlank() }
        return authRepository.register(displayName.trim(), email.trim(), password, trimmedPhone)
    }
}
