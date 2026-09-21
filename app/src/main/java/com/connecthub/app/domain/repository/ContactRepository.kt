package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.Contact
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    /** Live list of every other registered user (excludes the current user). */
    fun observeContacts(): Flow<List<Contact>>
}
