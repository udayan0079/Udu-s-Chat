package com.uduschat.app.core

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val displayName: String,
    val phoneNumberE164: String
)

class DeviceContactsHelper(private val context: Context) {

    suspend fun loadContacts(defaultCountry: CountryCode): List<DeviceContact> = withContext(Dispatchers.IO) {
        val results = linkedMapOf<String, DeviceContact>()
        runCatching {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex).orEmpty().trim()
                    val rawNumber = cursor.getString(numberIndex).orEmpty()
                    val normalized = PhoneNumberUtils.normalizeToE164(rawNumber, defaultCountry)
                    if (normalized != null && !results.containsKey(normalized)) {
                        results[normalized] = DeviceContact(
                            displayName = name.ifBlank { PhoneNumberUtils.formatDisplayPhone(normalized) },
                            phoneNumberE164 = normalized
                        )
                    }
                }
            }
        }
        results.values.toList()
    }
}
