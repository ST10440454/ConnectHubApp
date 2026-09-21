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

    /** Live list of every registered user, for building the Contacts / Chats List screen. */
    fun observeUsers(): Flow<List<UserDto>> = callbackFlow {
        val registration = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val users = snapshot?.documents?.mapNotNull { it.toObject(UserDto::class.java) } ?: emptyList()
            trySend(users)
        }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(messageDto: MessageDto) {
        val docRef = messagesCollection.document()
        messagesCollection.document(docRef.id).set(messageDto.copy(id = docRef.id)).await()
    }

    /** Real-time listener scoped to one 1:1 conversation, ordered oldest-first. */
    fun observeMessages(conversationId: String): Flow<List<MessageDto>> = callbackFlow {
        val registration = messagesCollection
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { it.toObject(MessageDto::class.java) }
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
        val results = mutableMapOf<String, List<MessageDto>>()

        fun emitCombined() {
            trySend(results.values.flatten().distinctBy { it.id })
        }

        val sentRegistration = messagesCollection
            .whereEqualTo("senderId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                results["sent"] = snapshot?.documents?.mapNotNull { it.toObject(MessageDto::class.java) } ?: emptyList()
                emitCombined()
            }

        val receivedRegistration = messagesCollection
            .whereEqualTo("receiverId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
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
    }
}
