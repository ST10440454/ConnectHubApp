package com.connecthub.app.data.remote

import com.connecthub.app.data.remote.dto.MessageDto
import com.connecthub.app.data.remote.dto.UserDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreService(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val usersCollection = db.collection("users")
    private val messagesCollection = db.collection("messages")

    suspend fun createUserProfile(userDto: UserDto) {
        usersCollection.document(userDto.uid).set(userDto).await()
    }

    suspend fun getUserProfile(uid: String): UserDto? {
        val snapshot = usersCollection.document(uid).get().await()
        return snapshot.toObject(UserDto::class.java)
    }

    fun observeUsers(): Flow<List<UserDto>> = callbackFlow {
        val registration = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { it.toObject(UserDto::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }

    private fun contactsOf(ownerUid: String) = usersCollection.document(ownerUid).collection("contacts")

    /** Live set of the uids saved as contacts by [ownerUid] (users/{ownerUid}/contacts/{contactUid}). */
    fun observeContactIds(ownerUid: String): Flow<Set<String>> = callbackFlow {
        val registration = contactsOf(ownerUid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptySet())
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.map { it.id }?.toSet() ?: emptySet())
        }
        awaitClose { registration.remove() }
    }

    suspend fun isContact(ownerUid: String, contactUid: String): Boolean =
        contactsOf(ownerUid).document(contactUid).get().await().exists()

    /** Makes the saved contact list equal to [contactUids]: adds the missing, deletes the rest. */
    suspend fun replaceContacts(ownerUid: String, contactUids: Set<String>) {
        val collection = contactsOf(ownerUid)
        val existing = collection.get().await().documents.map { it.id }.toSet()
        val toDelete = (existing - contactUids).toList()
        val toAdd = (contactUids - existing).toList()
        val now = System.currentTimeMillis()

        // A Firestore batch holds at most 500 writes, so work in chunks.
        toDelete.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(collection.document(it)) }
            batch.commit().await()
        }
        toAdd.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(collection.document(it), mapOf("uid" to it, "addedAt" to now)) }
            batch.commit().await()
        }
    }

    /** Writes the message and returns its new document id. */
    suspend fun sendMessage(messageDto: MessageDto): String {
        val docRef = messagesCollection.document()
        messagesCollection.document(docRef.id).set(messageDto.copy(id = docRef.id)).await()
        return docRef.id
    }

    /** Real-time listener scoped to one 1:1 conversation, ordered oldest-first. */
    fun observeMessages(conversationId: String): Flow<List<MessageDto>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents
                    ?.mapNotNull { it.toObject(MessageDto::class.java) }
                    ?.sortedBy { it.timestamp }
                    ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Every message where the current user is either sender or receiver, across all
     * conversations — used to compute last-message previews for the Chats List.
     * Two listeners are merged client-side since Firestore doesn't support OR queries
     * across different fields in a single query without a composite index setup.
     */
    fun observeAllMessagesForUser(userId: String): Flow<List<MessageDto>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val results = mutableMapOf<String, List<MessageDto>>()

        fun emitCombined() {
            trySend(results.values.flatten().distinctBy { it.id })
        }

        val sentRegistration = messagesCollection
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                results["sent"] = snapshot?.documents?.mapNotNull { it.toObject(MessageDto::class.java) } ?: emptyList()
                emitCombined()
            }

        val receivedRegistration = messagesCollection
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                results["received"] = snapshot?.documents?.mapNotNull { it.toObject(MessageDto::class.java) } ?: emptyList()
                emitCombined()
            }

        awaitClose {
            sentRegistration.remove()
            receivedRegistration.remove()
        }
    }

    /** Batch-updates every unread message addressed to [userId] in [conversationId] to READ. */
    suspend fun markMessagesAsRead(conversationId: String, userId: String) {
        if (conversationId.isBlank() || userId.isBlank()) return
        try {
            val unread = messagesCollection
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("receiverId", userId)
                .get()
                .await()

            val toUpdate = unread.documents.filter { it.getString("status") != "READ" }
            if (toUpdate.isEmpty()) return

            val batch = db.batch()
            toUpdate.forEach { doc -> batch.update(doc.reference, "status", "READ") }
            batch.commit().await()
        } catch (_: Exception) {
            // Best-effort — a failed read-receipt update shouldn't disrupt the chat UI.
        }
    }
}
