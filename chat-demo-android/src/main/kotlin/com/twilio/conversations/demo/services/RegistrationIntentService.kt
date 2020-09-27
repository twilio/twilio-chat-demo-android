package com.twilio.conversations.demo.services

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.iid.FirebaseInstanceId
import com.twilio.conversations.demo.FCMPreferences
import com.twilio.conversations.demo.TwilioApplication
import org.jetbrains.anko.*

/**
 * Registration intent handles receiving and updating the FCM token lifecycle events.
 */
class RegistrationIntentService : IntentService("RegistrationIntentService"), AnkoLogger {
    init {
        info { "Started" }
    }

    override fun onHandleIntent(intent: Intent?) {
        info { "onHandleIntent" }
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        try {
            val token = FirebaseInstanceId.getInstance().token
            info { "FCM Registration Token: ${token!!}" }

            /**
             * Persist registration to Twilio servers.
             */
            TwilioApplication.instance.basicClient.setFCMToken(token!!)

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(FCMPreferences.SENT_TOKEN_TO_SERVER, true).apply()
        } catch (e: Exception) {
            error { "Failed to complete token refresh: $e" }
            // If an exception happens while fetching the new token or updating our registration
            // data, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(FCMPreferences.SENT_TOKEN_TO_SERVER, false).apply()
        }

        // Notify UI that registration has completed, so the progress indicator can be hidden.
        val registrationComplete = Intent(FCMPreferences.REGISTRATION_COMPLETE)
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete)
    }

    companion object {
        private val TOPICS = arrayOf("global")
    }
}
