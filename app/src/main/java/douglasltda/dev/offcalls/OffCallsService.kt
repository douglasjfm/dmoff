package douglasltda.dev.offcalls

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService

class OffCallsService : CallScreeningService() {

    private val prefs by lazy { getSharedPreferences("off_calls_prefs", Context.MODE_PRIVATE) }

    override fun onScreenCall(callDetails: Call.Details) {
        // Only handle INCOMING calls
        if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
            return
        }

        val prefEnabled = prefs.getBoolean(getString(R.string.str_apppref_key_blocker), false)
        val prefsCounter = prefs.getInt(getString(R.string.str_apppref_key_counter), 0)

        // Get the incoming phone number
        val rawHandle = callDetails.handle
        val phoneNumber = rawHandle?.schemeSpecificPart

        if (!prefEnabled) {
            return
        }


        if (phoneNumber != null) {
            val isContact = checkContact(phoneNumber)

            if (!isContact) {
                // Number not in contacts -> BLOCK THE CALL
                val response = CallScreeningService.CallResponse.Builder()
                    .setDisallowCall(true)      // Rejects the call
                    .setRejectCall(true)        // Sends to voicemail / hang up
                    .setSkipCallLog(false)      // Keep it in the log so user knows it was blocked
                    .setSkipNotification(true)  // Don't ring or notify the user
                    .build()

                respondToCall(callDetails, response)
                prefs.edit().putInt(getString(R.string.str_apppref_key_counter), prefsCounter + 1).apply()
                return
            }
        }

        // Number is in contacts -> ALLOW THE CALL
        val allowResponse = CallScreeningService.CallResponse.Builder().build()
        respondToCall(callDetails, allowResponse)
    }

    // Helper function to query the Android Contacts Provider with fallback suffix matching
    private fun checkContact(phoneNumber: String): Boolean {
        // 1. Try standard PhoneLookup first
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.count > 0) {
                return true // Number found in contacts
            }
        }

        // 2. Fallback: Suffix matching against all contacts (handling numbers saved without DDD or prefixes)
        val incomingDigits = phoneNumber.filter { it.isDigit() }
        if (incomingDigits.length < 8) {
            return false
        }

        // Take the last 8 digits to match local numbers
        val incomingSuffix = incomingDigits.takeLast(8)

        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val phoneProjection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

        contentResolver.query(phoneUri, phoneProjection, null, null, null)?.use { cursor ->
            val numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (numberCol != -1) {
                while (cursor.moveToNext()) {
                    val storedNumber = cursor.getString(numberCol) ?: continue
                    val storedDigits = storedNumber.filter { it.isDigit() }

                    if (storedDigits.length >= 8) {
                        val storedSuffix = storedDigits.takeLast(8)
                        if (incomingSuffix == storedSuffix) {
                            return true
                        }
                    }
                }
            }
        }

        return false // Number not found
    }
}