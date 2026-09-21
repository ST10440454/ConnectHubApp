package com.connecthub.app.data.repository

import com.connecthub.app.data.remote.FirebaseAuthService
import com.connecthub.app.data.remote.FirestoreService
import com.connecthub.app.data.remote.dto.UserDto
import com.connecthub.app.domain.model.User
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.util.AppResult
import com.connecthub.app.util.toFriendlyMessage

class AuthRepositoryImpl(
    private val authService: FirebaseAuthService,
    private val firestoreService: FirestoreService
) : AuthRepository {

    override suspend fun register(displayName: String, email: String, password: String): AppResult<User> {
        return try {
            val uid = authService.register(email, password)
            val createdAt = System.currentTimeMillis()
            val userDto = UserDto(uid = uid, displayName = displayName, email = email, createdAt = createdAt)
            firestoreService.createUserProfile(userDto)
            AppResult.Success(User(uid, displayName, email, createdAt))
        } catch (e: Exception) {
            AppResult.Error(e.toFriendlyMessage())
        }
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        return try {
            val uid = authService.login(email, password)
            val profile = firestoreService.getUserProfile(uid)
            val user = User(
                uid = uid,
                displayName = profile?.displayName ?: email.substringBefore("@"),
                email = email,
                createdAtMillis = profile?.createdAt ?: 0L
            )
            AppResult.Success(user)
        } catch (e: Exception) {
            AppResult.Error(e.toFriendlyMessage())
        }
    }

    override fun logout() = authService.logout()

    override fun currentUser(): User? {
        val uid = authService.currentUid() ?: return null
        val email = authService.currentEmail() ?: return null
        return User(uid, email.substringBefore("@"), email, 0L)
    }
}
