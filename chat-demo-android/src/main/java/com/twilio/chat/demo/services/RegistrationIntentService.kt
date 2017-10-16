package com.twilio.chat.demo.services

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.iid.FirebaseInstanceId
import com.twilio.chat.demo.FCMPreferences
import com.twilio.chat.demo.TwilioApplication
import timber.log.Timber

/**
 * Registration intent handles receiving and updating the FCM token lifecycle events.
 */
class RegistrationIntentService : IntentService("RegistrationIntentService") {
    init {
        Timber.i("Started")
    }

    override fun onHandleIntent(intent: Intent?) {
        Timber.i("onHandleIntent")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        try {
            val token = FirebaseInstanceId.getInstance().token
            Timber.i("FCM Registration Token: " + token!!)

            /**
             * Persist registration to Twilio servers.
             */
            TwilioApplication.instance.basicClient.setFCMToken(token)

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(FCMPreferences.SENT_TOKEN_TO_SERVER, true).apply()
        } catch (e: Exception) {
            Timber.e("Failed to complete token refresh", e)
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
