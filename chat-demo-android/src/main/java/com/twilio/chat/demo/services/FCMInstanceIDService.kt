package com.twilio.chat.demo.services

import com.google.firebase.iid.FirebaseInstanceIdService
import org.jetbrains.anko.*

class FCMInstanceIDService : FirebaseInstanceIdService(), AnkoLogger {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. This call is initiated by the
     * InstanceID provider.
     */
    override fun onTokenRefresh() {
        debug { "onTokenRefresh" }

        // Fetch updated Instance ID token and notify our app's server of any changes.
        startService<RegistrationIntentService>()
    }
}
