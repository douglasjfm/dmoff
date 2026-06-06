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

        val prefEnabled = prefs.getBoolean("blocker_enabled", false)
        val prefsCounter = prefs.getInt("blocker_counter", 0)

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
                prefs.edit().putInt("blocker_counter", prefsCounter + 1).apply()
                return
            }
        }

        // Number is in contacts -> ALLOW THE CALL
        val allowResponse = CallScreeningService.CallResponse.Builder().build()
        respondToCall(callDetails, allowResponse)
    }

    // Helper function to query the Android Contacts Provider
    private fun checkContact(phoneNumber: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        contentResolver.query(uri, projection, null, null, null).use { cursor ->
            if (cursor != null && cursor.count > 0) {
                return true // Number found in contacts
            }
        }
        return false // Number not found
    }
}