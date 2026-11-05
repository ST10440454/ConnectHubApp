package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {

    fun observeRegisteredUsers(): Flow<List<Contact>>

    fun observeContacts(): Flow<List<Contact>>

    suspend fun isContact(uid: String): Boolean

    suspend fun replaceContacts(contacts: List<Contact>)
}
