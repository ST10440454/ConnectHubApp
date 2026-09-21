package com.connecthub.app.data.remote.dto

/** Plain data holders matching Firestore document shape (no-arg ctor required by Firestore). */
data class UserDto(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val createdAt: Long = 0L
)

data class MessageDto(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0L,
    val status: String = "SENT"
)
