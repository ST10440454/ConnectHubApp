package com.connecthub.app.domain.repository

import com.connecthub.app.domain.model.DeviceContact

interface DeviceContactRepository {
    /**
     * Reads every contact with a phone number from the device's own phone book.
     * Implementations live in the data layer since reading the phone book is an
     * Android-framework concern (ContentResolver) — this interface itself stays
     * framework-free, matching every other repository in this app.
     */
    suspend fun getDeviceContacts(): List<DeviceContact>
}
