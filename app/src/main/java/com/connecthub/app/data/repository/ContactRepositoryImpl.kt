package com.connecthub.app.data.repository

import com.connecthub.app.data.remote.FirebaseAuthService
import com.connecthub.app.data.remote.FirestoreService
import com.connecthub.app.domain.model.Contact
import com.connecthub.app.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val firestoreService: FirestoreService,
    private val authService: FirebaseAuthService
) : ContactRepository {

    override fun observeContacts(): Flow<List<Contact>> {
        val currentUid = authService.currentUid()
        return firestoreService.observeUsers().map { userDtos ->
            userDtos
                .filter { it.uid != currentUid && it.uid.isNotBlank() }
                .map { Contact(uid = it.uid, displayName = it.displayName, email = it.email) }
                .sortedBy { it.displayName.lowercase() }
        }
    }
}
