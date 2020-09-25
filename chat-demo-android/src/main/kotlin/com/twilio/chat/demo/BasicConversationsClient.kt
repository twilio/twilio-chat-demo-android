package com.twilio.conversations.demo

import ToastStatusListener
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import com.twilio.conversations.CallbackListener
import com.twilio.conversations.Conversation
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.ConversationsClientListener
import com.twilio.conversations.ErrorInfo
import com.twilio.conversations.User
import com.twilio.conversations.internal.HandlerUtil
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.warn
import java.util.*

class BasicConversationsClient(private val context: Context)
    : CallbackListener<ConversationsClient>
    , ConversationsClientListener
    , AnkoLogger
{
    private var accessToken: String? = null
    private var fcmToken: String? = null

    var conversationsClient: ConversationsClient? = null
        private set

    private var loginListener: LoginListener? = null
    private var loginListenerHandler: Handler? = null

    private var urlString: String? = null
    private var username: String? = null
    private var pinCerts: Boolean = true
    private var realm: String? = null

    init {
        if (BuildConfig.DEBUG) {
            warn { "Enabling DEBUG logging" }
            ConversationsClient.setLogLevel(ConversationsClient.LogLevel.VERBOSE)
        }
    }

    interface LoginListener {
        fun onLoginStarted()
        fun onLoginFinished()
        fun onLoginError(errorMessage: String)
        fun onLogoutFinished()
    }

    private fun notifyLoginStarted() { // Called before getting access token
        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLoginStarted()
            }
        }
    }
    private fun notifyLoginFinished() { // Called after successful creation of ChatClient
        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLoginFinished()
            }
        }
    }
    private fun notifyLoginError(errorMessage: String) {
        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLoginError(errorMessage)
            }
        }
    }
    private fun notifyLogoutFinished() {
        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLogoutFinished()
            }
        }
    }

    fun setFCMToken(fcmToken: String) {
        warn { "setFCMToken $fcmToken" }
        this.fcmToken = fcmToken
        if (conversationsClient != null) {
            setupFcmToken()
        }
    }

    fun login(username: String, pinCerts: Boolean, realm: String, url: String, listener: LoginListener) {
        /*
        if (username == this.username
                && pinCerts == this.pinCerts
                && realm == this.realm
                && url == this.urlString
                && listener === loginListener
                && chatClient != null) {
            onSuccess(chatClient!!)
            return
        }
         */
        assert(conversationsClient == null) { "ChatClient object is to be created on login, should be null before login" }

        this.username = username
        this.pinCerts = pinCerts
        this.realm = realm
        urlString = url

        loginListenerHandler = HandlerUtil.setupListenerHandler()
        loginListener = listener

        getAccessToken()
    }

    fun updateToken() {
        getAccessToken()
    }

    private fun setupFcmToken() {
        conversationsClient!!.registerFCMToken(ConversationsClient.FCMToken(fcmToken),
                ToastStatusListener(
                        "Firebase Messaging registration successful",
                        "Firebase Messaging registration not successful"))
    }

    fun unregisterFcmToken() {
        conversationsClient!!.unregisterFCMToken(ConversationsClient.FCMToken(fcmToken),
                ToastStatusListener(
                        "Firebase Messaging unregistration successful",
                        "Firebase Messaging unregistration not successful"))
    }

    private fun createClient() {
        assert(conversationsClient == null)

        val props = ConversationsClient.Properties.Builder()
                .setRegion(realm)
                .setDeferCertificateTrustToPlatform(!pinCerts)
                .createProperties()

        ConversationsClient.create(context.applicationContext,
                accessToken!!,
                props,
                this)
    }

    fun shutdown() {
        conversationsClient!!.shutdown()
        conversationsClient = null // Client no longer usable after shutdown()
        notifyLogoutFinished()
    }

    // Client created, remember the reference and set up UI
    override fun onSuccess(client: ConversationsClient) {
        debug { "Received completely initialized ChatClient" }
        conversationsClient = client

        if (fcmToken != null) {
            setupFcmToken()
        }

        notifyLoginFinished()
    }

    // Client not created, fail
    override fun onError(errorInfo: ErrorInfo?) {
        TwilioApplication.instance.logErrorInfo("Login error", errorInfo!!)
        conversationsClient = null

        notifyLoginError(errorInfo.toString())
    }

    // Token expiration events

    override fun onTokenAboutToExpire() {
        if (conversationsClient != null) {
            TwilioApplication.instance.showToast("Token will expire in 3 minutes. Getting new token.")
            getAccessToken()
        }
    }

    override fun onTokenExpired() {
        accessToken = null
        if (conversationsClient != null) {
            TwilioApplication.instance.showToast("Token expired. Getting new token.")
            getAccessToken()
        }
    }

    private fun getAccessToken() {
        GetAccessTokenAsyncTask().execute(urlString)
    }

    /**
     * Modify this method if you need to provide more information to your Access Token Service.
     */
    //TODO coroutines
    private inner class GetAccessTokenAsyncTask : AsyncTask<String, Void, Optional<String>>() {
        override fun onPreExecute() {
            super.onPreExecute()
            if (conversationsClient == null) {
                notifyLoginStarted()
            }
        }

        override fun doInBackground(vararg params: String): Optional<String> {
            var result: Optional<String> = Optional.empty();
            try {
                result = Optional.of(HttpHelper.httpGet(params[0]))
            } catch (e: Exception) {
                System.err.println("getAccessToken() error:")
                e.printStackTrace()
                notifyLoginError(e.message.orEmpty())
            }

            return result
        }

        override fun onPostExecute(result: Optional<String>) {
            if (!result.isPresent) return

            accessToken = result.get();

            super.onPostExecute(result)

            applyAccessToken()
        }

        private fun applyAccessToken() {
            if (conversationsClient == null) {
                // Create client with accessToken
                createClient()
            } else {
                // Client already exists, so set accessToken to it
                conversationsClient!!.updateToken(accessToken, ToastStatusListener(
                        "Client Update Token was successfull",
                        "Client Update Token failed"))
            }
        }
    }

    override fun onConversationAdded(p0: Conversation?) {}
    override fun onConversationDeleted(p0: Conversation?) {}
    override fun onConversationSynchronizationChange(p0: Conversation?) {}
    override fun onConversationUpdated(p0: Conversation?, p1: Conversation.UpdateReason?) {}
    override fun onClientSynchronization(p0: ConversationsClient.SynchronizationStatus?) {}
    override fun onConnectionStateChange(p0: ConversationsClient.ConnectionState?) {}
    override fun onUserSubscribed(p0: User?) {}
    override fun onUserUnsubscribed(p0: User?) {}
    override fun onUserUpdated(p0: User?, p1: User.UpdateReason?) {}
    override fun onAddedToConversationNotification(p0: String?) {}
    override fun onRemovedFromConversationNotification(p0: String?) {}
    override fun onNewMessageNotification(p0: String?, p1: String?, p2: Long) {}
    override fun onNotificationFailed(p0: ErrorInfo?) {}
    override fun onNotificationSubscribed() {}
}
