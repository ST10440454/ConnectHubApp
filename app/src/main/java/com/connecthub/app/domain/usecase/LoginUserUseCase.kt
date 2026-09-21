package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.User
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.util.AppResult
import com.connecthub.app.util.Validators

class LoginUserUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): AppResult<User> {
        val validation = Validators.validateLogin(email, password)
        if (!validation.isValid) {
            return AppResult.Error(validation.errorMessage ?: "Invalid input.")
        }
        return authRepository.login(email.trim(), password)
    }
}
