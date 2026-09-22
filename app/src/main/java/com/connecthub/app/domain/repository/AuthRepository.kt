package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.User
import com.connecthub.app.util.AppResult

interface AuthRepository {
    /**
     * [phoneNumber] is optional. It exists so a registered user's phone number can be
     * matched against the device's phone-book contacts on the "New Chat" screen — an
     * account registered without one simply won't be discoverable that way.
     */
    suspend fun register(displayName: String, email: String, password: String, phoneNumber: String?): AppResult<User>
    suspend fun login(email: String, password: String): AppResult<User>
    fun logout()
    fun currentUser(): User?
}
