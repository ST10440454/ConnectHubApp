package com.connecthub.app.data.repository

import com.connecthub.app.data.remote.FirebaseAuthService
import com.connecthub.app.data.remote.FirestoreService
import com.connecthub.app.domain.model.Contact
import com.connecthub.app.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService
) : ContactRepository {

    override fun observeRegisteredUsers(): Flow<List<Contact>> {
        val currentUid = authService.currentUid()
        return firestoreService.observeUsers().map { userDtos ->
            userDtos
                .filter { it.uid != currentUid && it.uid.isNotBlank() }
                .map {
                    Contact(
                        uid = it.uid,
                        displayName = it.displayName,
                        email = it.email,
                        phoneNumber = it.phoneNumber.takeIf { phone -> phone.isNotBlank() }
                    )
                }
                .sortedBy { it.displayName.lowercase() }
        }
    }

    override fun observeContacts(): Flow<List<Contact>> {
        val currentUid = authService.currentUid()
        if (currentUid.isNullOrBlank()) return flowOf(emptyList())
        return combine(
            firestoreService.observeContactIds(currentUid),
            observeRegisteredUsers()
        ) { contactIds, users -> users.filter { it.uid in contactIds } }
    }

    override suspend fun isContact(uid: String): Boolean {
        val currentUid = authService.currentUid()
        if (currentUid.isNullOrBlank() || uid.isBlank()) return false
        return firestoreService.isContact(currentUid, uid)
    }

    override suspend fun replaceContacts(contacts: List<Contact>) {
        val currentUid = authService.currentUid()
        if (currentUid.isNullOrBlank()) return
        firestoreService.replaceContacts(currentUid, contacts.map { it.uid }.toSet())
    }
}
