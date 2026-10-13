package com.connecthub.app.data.repository

import com.connecthub.app.data.remote.DeviceContactsService
import com.connecthub.app.domain.model.DeviceContact
import com.connecthub.app.domain.repository.DeviceContactRepository

class DeviceContactRepositoryImpl(
    private val deviceContactsService: DeviceContactsService
) : DeviceContactRepository {
    override suspend fun getDeviceContacts(): List<DeviceContact> = deviceContactsService.getContacts()
}
