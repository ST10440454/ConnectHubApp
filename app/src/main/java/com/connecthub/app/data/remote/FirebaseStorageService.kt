package com.connecthub.app.data.remote

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageService(private val storage: FirebaseStorage = FirebaseStorage.getInstance()) {

    suspend fun uploadChatImage(conversationId: String, imageBytes: ByteArray, mimeType: String): String {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        val ref = storage.reference.child("chat_images/$conversationId/$fileName")

        ref.putBytes(imageBytes).await()
        return ref.downloadUrl.await().toString()
    }
}
