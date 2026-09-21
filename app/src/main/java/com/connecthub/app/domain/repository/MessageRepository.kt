package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.Message
import com.connecthub.app.util.AppResult
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendMessage(conversationId: String, receiverId: String, content: String): AppResult<Unit>
    fun observeMessages(conversationId: String): Flow<List<Message>>

    /**
     * All messages across every conversation the current user participates in,
     * used to build last-message previews for the Chats List screen.
     */
    fun observeAllMessagesForCurrentUser(): Flow<List<Message>>

    /** Marks every unread message addressed to the current user in [conversationId] as READ. */
    suspend fun markMessagesAsRead(conversationId: String)
}
