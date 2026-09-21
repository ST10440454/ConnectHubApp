package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.User
import com.connecthub.app.util.AppResult

interface AuthRepository {
    suspend fun register(displayName: String, email: String, password: String): AppResult<User>
    suspend fun login(email: String, password: String): AppResult<User>
    fun logout()
    fun currentUser(): User?
}
