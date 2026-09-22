package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.Message
import com.connecthub.app.util.AppResult
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(conversationId: String, receiverId: String, content: String): AppResult<Unit>

    /**
     * [imageBytes] is a plain ByteArray rather than an android.net.Uri deliberately —
     * the domain layer stays free of Android framework types. The presentation layer
     * (which IS allowed to touch Uri/ContentResolver) reads the picked image into
     * bytes before calling this, keeping this interface testable with plain fakes.
     */
    suspend fun sendImageMessage(
        conversationId: String,
        receiverId: String,
        imageBytes: ByteArray,
        mimeType: String
    ): AppResult<Unit>

    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * All messages across every conversation the current user participates in,
     * used to build last-message previews for the Chats List screen.
     */
    fun observeAllMessagesForCurrentUser(): Flow<List<Message>>

    /** Marks every unread message addressed to the current user in [conversationId] as READ. */
    suspend fun markMessagesAsRead(conversationId: String)
}
