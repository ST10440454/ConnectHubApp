package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.Contact
import com.connecthub.app.domain.model.DeviceContactMatch
import com.connecthub.app.domain.model.normalizePhoneNumber
import com.connecthub.app.domain.repository.ContactRepository
import com.connecthub.app.domain.repository.DeviceContactRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class GetDeviceContactMatchesUseCase(
    private val deviceContactRepository: DeviceContactRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): List<DeviceContactMatch> {
        val deviceContacts = deviceContactRepository.getDeviceContacts()
        val registeredUsers = contactRepository.observeRegisteredUsers().first()

        val byNormalizedPhone: Map<String, Contact> = registeredUsers
            .mapNotNull { contact -> contact.phoneNumber?.let { normalizePhoneNumber(it) to contact } }
            .toMap()

        val matches = deviceContacts
            .distinctBy { normalizePhoneNumber(it.phoneNumber) }
            .map { deviceContact ->
                val match = byNormalizedPhone[normalizePhoneNumber(deviceContact.phoneNumber)]
                DeviceContactMatch(deviceContact = deviceContact, matchedContact = match)
            }
            .sortedWith(compareByDescending<DeviceContactMatch> { it.matchedContact != null }
                .thenBy { it.deviceContact.name.lowercase() })

        if (deviceContacts.isNotEmpty()) {
            try {
                contactRepository.replaceContacts(matches.mapNotNull { it.matchedContact })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
            }
        }
        return matches
    }
}
