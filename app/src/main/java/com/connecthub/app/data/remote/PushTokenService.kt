package com.connecthub.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/** Keeps the signed-in user's FCM token on users/{uid}.fcmToken so the backend can push to them. */
class PushTokenService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {
    suspend fun registerCurrentToken() = saveToken(messaging.token.await())

    /** merge = true so the rest of the profile (name, email, phone) is never overwritten. */
    fun saveToken(token: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    /** Call before logging out so the next person using this phone doesn't get your pushes. */
    suspend fun clearToken() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).update("fcmToken", FieldValue.delete()).await()
    }
}