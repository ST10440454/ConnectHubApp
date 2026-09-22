package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.ConversationPreview
import com.connecthub.app.domain.model.Message
import com.connecthub.app.domain.model.MessageType
import com.connecthub.app.domain.model.conversationIdFor
import com.connecthub.app.domain.repository.AuthRepository
import com.connecthub.app.domain.repository.ContactRepository
import com.connecthub.app.domain.repository.MessageRepository
import com.connecthub.app.util.AppResult
import com.connecthub.app.util.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SendMessageUseCase(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(conversationId: String, receiverId: String, content: String): AppResult<Unit> {
        if (!Validators.isValidMessageContent(content)) {
            return AppResult.Error("Message cannot be empty.")
        }
        return messageRepository.sendMessage(conversationId, receiverId, content.trim())
    }
}

class SendImageMessageUseCase(private val messageRepository: MessageRepository) {
    suspend operator fun invoke(
        conversationId: String,
        receiverId: String,
        imageBytes: ByteArray,
        mimeType: String
    ): AppResult<Unit> {
        if (imageBytes.isEmpty()) {
            return AppResult.Error("Couldn't read that image. Please try a different one.")
        }
        if (imageBytes.size > MAX_IMAGE_BYTES) {
            return AppResult.Error("That image is too large to send (max 8 MB).")
        }
        return messageRepository.sendImageMessage(conversationId, receiverId, imageBytes, mimeType)
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024 // 8 MB
    }
}

class ObserveMessagesUseCase(private val messageRepository: MessageRepository) {
    operator fun invoke(conversationId: String): Flow<List<Message>> =
        messageRepository.observeMessages(conversationId)
}

/**
 * Combines the live contact list with the current user's messages across all
 * conversations to build one preview row per contact, most-recently-active first.
 */
class ObserveConversationsUseCase(
    private val contactRepository: ContactRepository,
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<List<ConversationPreview>> {
        val currentUserId = authRepository.currentUser()?.uid
        return combine(
            contactRepository.observeContacts(),
            messageRepository.observeAllMessagesForCurrentUser()
        ) { contacts, messages ->
            contacts.map { contact ->
                val conversationId = conversationIdFor(currentUserId.orEmpty(), contact.uid)
                val lastMessage = messages
                    .filter { it.conversationId == conversationId }
                    .maxByOrNull { it.timestampMillis }

                ConversationPreview(
                    contact = contact,
                    conversationId = conversationId,
                    lastMessage = lastMessage?.content,
                    lastMessageIsImage = lastMessage?.type == MessageType.IMAGE,
                    lastMessageTimestampMillis = lastMessage?.timestampMillis,
                    lastMessageIsOwn = lastMessage?.senderId == currentUserId
                )
            }.sortedByDescending { it.lastMessageTimestampMillis ?: 0L }
        }
    }
}
