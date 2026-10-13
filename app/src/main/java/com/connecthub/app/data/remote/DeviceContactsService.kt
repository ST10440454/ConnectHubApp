package com.connecthub.app.data.remote

import android.content.Context
import android.provider.ContactsContract
import com.connecthub.app.domain.model.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's own phone book. Requires READ_CONTACTS to have already been
 * granted — callers (the presentation layer) are responsible for the runtime
 * permission request before calling [getContacts]; this class does not check itself,
 * since checking permissions is a UI-layer concern (deciding what to show while
 * waiting on the request).
 */
class DeviceContactsService(private val context: Context) {

    suspend fun getContacts(): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<DeviceContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (nameIndex < 0 || numberIndex < 0) return@use

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)?.trim().orEmpty()
                val number = cursor.getString(numberIndex)?.trim().orEmpty()
                if (name.isNotBlank() && number.isNotBlank()) {
                    contacts.add(DeviceContact(name = name, phoneNumber = number))
                }
            }
        }

        contacts
    }
}
