package com.connecthub.app.domain.model

/** Another registered ConnectHub user, as shown in the Chats List / New Chat screens. */
data class Contact(
    val uid: String,
    val displayName: String,
    val email: String,
    val phoneNumber: String? = null
)

/**
 * One row in the Chats List screen: a contact plus a preview of the most recent
 * message exchanged with them, if any conversation has started yet.
 */
data class ConversationPreview(
    val contact: Contact,
    val conversationId: String,
    val lastMessage: String? = null,
    val lastMessageIsImage: Boolean = false,
    val lastMessageTimestampMillis: Long? = null,
    val lastMessageIsOwn: Boolean = false
)

/** A contact read from the device's own phone book — not necessarily a ConnectHub user. */
data class DeviceContact(
    val name: String,
    val phoneNumber: String
)

/**
 * One row on the "New Chat" screen: a device contact, plus — if they're a registered
 * ConnectHub user — the matching [Contact] so the row can be tapped to start chatting.
 * [matchedContact] is null when this phone-book entry hasn't registered on ConnectHub yet.
 */
data class DeviceContactMatch(
    val deviceContact: DeviceContact,
    val matchedContact: Contact?
)

/**
 * Normalizes a phone number for matching: strips everything but digits, then keeps
 * only the last 9 digits. This deliberately ignores country code / leading zero
 * differences (e.g. "+27 71 234 5678", "0712345678", and "71 234 5678" all normalize
 * to "712345678"), which is the simplest robust approach for South African mobile
 * numbers without a full libphonenumber-style parser.
 */
fun normalizePhoneNumber(raw: String): String {
    val digitsOnly = raw.filter { it.isDigit() }
    return if (digitsOnly.length >= 9) digitsOnly.takeLast(9) else digitsOnly
}
