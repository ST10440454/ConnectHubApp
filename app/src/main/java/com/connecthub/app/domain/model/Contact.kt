package com.connecthub.app.domain.model

/** Another registered ConnectHub user, as shown in the Chats List / New Chat screens. */
data class Contact(
    val uid: String,
    val displayName: String,
    val email: String
)

/**
 * One row in the Chats List screen: a contact plus a preview of the most recent
 * message exchanged with them, if any conversation has started yet.
 */
data class ConversationPreview(
    val contact: Contact,
    val conversationId: String,
    val lastMessage: String? = null,
    val lastMessageTimestampMillis: Long? = null,
    val lastMessageIsOwn: Boolean = false
)
