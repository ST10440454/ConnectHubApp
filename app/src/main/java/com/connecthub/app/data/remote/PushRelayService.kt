package com.connecthub.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Asks the push relay (relay/ folder, a free Cloudflare Worker) to notify the receiver of a
 * message that was just written to Firestore. Fire-and-forget: it runs on its own scope so
 * leaving the chat screen right after sending doesn't cancel it, and a failure never
 * affects sending the message itself.
 */
class PushRelayService(
    private val relayUrl: String,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun notify(messageId: String) {
        if (relayUrl.isBlank() || messageId.isBlank()) return
        scope.launch { runCatching { post(messageId) } }
    }

    private suspend fun post(messageId: String) {
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token ?: return
        val connection = (URL(relayUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $idToken")
            setRequestProperty("Content-Type", "application/json")
        }
        try {
            connection.outputStream.use {
                it.write(JSONObject().put("messageId", messageId).toString().toByteArray(Charsets.UTF_8))
            }
            connection.responseCode // sends the request; the result is intentionally ignored
        } finally {
            connection.disconnect()
        }
    }
}
