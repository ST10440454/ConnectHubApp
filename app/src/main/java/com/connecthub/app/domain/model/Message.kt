package com.connecthub.app.domain.model

enum class MessageStatus { SENT, DELIVERED, READ }

enum class MessageType { TEXT, IMAGE }

data class Message(
    val id: String = "",
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val timestampMillis: Long,
    val status: MessageStatus = MessageStatus.SENT
)

/**
 * Deterministic conversation ID for a 1:1 chat between two users — the two UIDs
 * sorted and joined, so both participants always compute the same ID regardless
 * of who initiated the conversation.
 */
fun conversationIdFor(uidA: String, uidB: String): String {
    return listOf(uidA, uidB).sorted().joinToString("_")
}
