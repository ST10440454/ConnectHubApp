package com.connecthub.app.domain.usecase

import com.connecthub.app.domain.model.Contact
import com.connecthub.app.domain.model.DeviceContactMatch
import com.connecthub.app.domain.model.normalizePhoneNumber
import com.connecthub.app.domain.repository.ContactRepository
import com.connecthub.app.domain.repository.DeviceContactRepository
import kotlinx.coroutines.flow.first

/**
 * Reads the device's phone book once and matches each entry against the live list of
 * registered ConnectHub users (by normalized phone number), producing one row per
 * phone-book contact — tappable if they're already a user, invite-only if not.
 *
 * Contacts registered without a phone number (an optional field at registration)
 * can never be matched this way — this is a known, documented limitation rather
 * than a bug; see the README section on this feature.
 */
class GetDeviceContactMatchesUseCase(
    private val deviceContactRepository: DeviceContactRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): List<DeviceContactMatch> {
        val deviceContacts = deviceContactRepository.getDeviceContacts()
        val registeredContacts = contactRepository.observeContacts().first()

        val byNormalizedPhone: Map<String, Contact> = registeredContacts
            .mapNotNull { contact -> contact.phoneNumber?.let { normalizePhoneNumber(it) to contact } }
            .toMap()

        return deviceContacts
            .distinctBy { normalizePhoneNumber(it.phoneNumber) }
            .map { deviceContact ->
                val match = byNormalizedPhone[normalizePhoneNumber(deviceContact.phoneNumber)]
                DeviceContactMatch(deviceContact = deviceContact, matchedContact = match)
            }
            .sortedWith(compareByDescending<DeviceContactMatch> { it.matchedContact != null }
                .thenBy { it.deviceContact.name.lowercase() })
    }
}
