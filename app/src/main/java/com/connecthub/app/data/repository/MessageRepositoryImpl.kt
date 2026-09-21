package com.connecthub.app.data.repository

import com.connecthub.app.data.remote.FirebaseAuthService
import com.connecthub.app.data.remote.FirestoreService
import com.connecthub.app.data.remote.dto.MessageDto
import com.connecthub.app.domain.model.Message
import com.connecthub.app.domain.model.MessageStatus
import com.connecthub.app.domain.repository.MessageRepository
import com.connecthub.app.util.AppResult
import com.connecthub.app.util.toFriendlyMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService
) : MessageRepository {

    override suspend fun sendMessage(conversationId: String, receiverId: String, content: String): AppResult<Unit> {
        val uid = authService.currentUid() ?: return AppResult.Error("You must be logged in to send messages.")
        val senderName = authService.currentEmail()?.substringBefore("@") ?: "Unknown"
        return try {
            val dto = MessageDto(
                conversationId = conversationId,
                senderId = uid,
                senderName = senderName,
                receiverId = receiverId,
                content = content,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT.name
            )
            firestoreService.sendMessage(dto)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(e.toFriendlyMessage())
        }
    }

    override fun observeMessages(conversationId: String): Flow<List<Message>> {
        return firestoreService.observeMessages(conversationId).map { dtos -> dtos.map { it.toDomain() } }
    }

    override fun observeAllMessagesForCurrentUser(): Flow<List<Message>> {
        val uid = authService.currentUid().orEmpty()
        return firestoreService.observeAllMessagesForUser(uid).map { dtos -> dtos.map { it.toDomain() } }
    }

    override suspend fun markMessagesAsRead(conversationId: String) {
        val uid = authService.currentUid() ?: return
        try {
            firestoreService.markMessagesAsRead(conversationId, uid)
        } catch (_: Exception) {
            // Best-effort — a failed read-receipt update shouldn't disrupt the chat UI.
        }
    }

    private fun MessageDto.toDomain(): Message = Message(
        id = id,
        conversationId = conversationId,
        senderId = senderId,
        senderName = senderName,
        receiverId = receiverId,
        content = content,
        timestampMillis = timestamp,
        status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.SENT)
    )
}
