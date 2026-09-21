package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.User
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.util.AppResult

/** In-memory fake so unit tests never touch real Firebase. */
class FakeAuthRepository : AuthRepository {

    var shouldFail = false
    var failureMessage = "Simulated failure"
    private var loggedInUser: User? = null

    override suspend fun register(displayName: String, email: String, password: String): AppResult<User> {
        if (shouldFail) return AppResult.Error(failureMessage)
        val user = User(uid = "fake-uid", displayName = displayName, email = email, createdAtMillis = 0L)
        loggedInUser = user
        return AppResult.Success(user)
    }

    override suspend fun login(email: String, password: String): AppResult<User> {
        if (shouldFail) return AppResult.Error(failureMessage)
        val user = User(uid = "fake-uid", displayName = email.substringBefore("@"), email = email, createdAtMillis = 0L)
        loggedInUser = user
        return AppResult.Success(user)
    }

    override fun logout() {
        loggedInUser = null
    }

    override fun currentUser(): User? = loggedInUser
}
