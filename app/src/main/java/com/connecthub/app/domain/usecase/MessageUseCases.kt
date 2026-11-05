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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private const val NOT_A_CONTACT_MESSAGE =
    "You can only message people whose phone number is in your contacts. " +
        "Save their number in your phone contacts to message them."
private const val CONTACT_CHECK_FAILED_MESSAGE =
    "Couldn't check your contacts. Check your connection and try again."

/** Null if [receiverId] is one of the user's contacts, otherwise the error to show. */
private suspend fun ContactRepository.contactCheckError(receiverId: String): AppResult.Error? {
    val allowed = try {
        isContact(receiverId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return AppResult.Error(CONTACT_CHECK_FAILED_MESSAGE)
    }
    return if (allowed) null else AppResult.Error(NOT_A_CONTACT_MESSAGE)
}

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(conversationId: String, receiverId: String, content: String): AppResult<Unit> {
        if (!Validators.isValidMessageContent(content)) {
            return AppResult.Error("Message cannot be empty.")
        }
        contactRepository.contactCheckError(receiverId)?.let { return it }
        return messageRepository.sendMessage(conversationId, receiverId, content.trim())
    }
}

class SendImageMessageUseCase(
    private val messageRepository: MessageRepository,
    private val contactRepository: ContactRepository
) {
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
        contactRepository.contactCheckError(receiverId)?.let { return it }
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
 * Builds one preview row per person, most-recently-active first: the user's contacts, plus
 * anyone who has messaged the user but isn't a contact (so incoming messages are never
 * hidden; the user just can't reply until they save that person's number).
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
            contactRepository.observeRegisteredUsers(),
            messageRepository.observeAllMessagesForCurrentUser()
        ) { contacts, registeredUsers, messages ->
            val contactIds = contacts.map { it.uid }.toSet()
            val strangerIds = messages
                .filter { it.senderId != currentUserId && it.senderId !in contactIds }
                .map { it.senderId }
                .toSet()
            val people = contacts + registeredUsers.filter { it.uid in strangerIds }

            people.map { contact ->
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
