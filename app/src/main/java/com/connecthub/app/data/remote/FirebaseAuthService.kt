package com.connecthub.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around the Firebase Auth SDK.
 * Firebase handles password hashing/encryption server-side (scrypt-based) —
 * the raw password is only ever sent over TLS and never stored by the app.
 */
class FirebaseAuthService(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("Registration failed: no UID returned.")
    }

    suspend fun login(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("Login failed: no UID returned.")
    }

    fun logout() = auth.signOut()

    fun currentUid(): String? = auth.currentUser?.uid

    fun currentEmail(): String? = auth.currentUser?.email
}
