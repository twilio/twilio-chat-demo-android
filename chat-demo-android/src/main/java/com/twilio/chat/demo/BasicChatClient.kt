package com.twilio.chat.demo

import com.twilio.chat.internal.HandlerUtil
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import ToastStatusListener
import com.twilio.chat.*
import org.jetbrains.anko.*
import java.util.Optional

class BasicChatClient(private val context: Context) : CallbackListener<ChatClient>(), ChatClientListener, AnkoLogger {
    private var accessToken: String? = null
    private var fcmToken: String? = null

    var chatClient: ChatClient? = null
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
            ChatClient.setLogLevel(android.util.Log.VERBOSE)
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
        if (chatClient != null) {
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
        assert(chatClient == null) { "ChatClient object is to be created on login, should be null before login" }

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
        chatClient!!.registerFCMToken(fcmToken,
                ToastStatusListener(
                        "Firebase Messaging registration successful",
                        "Firebase Messaging registration not successful"))
    }

    fun unregisterFcmToken() {
        chatClient!!.unregisterFCMToken(fcmToken,
                ToastStatusListener(
                        "Firebase Messaging unregistration successful",
                        "Firebase Messaging unregistration not successful"))
    }

    private fun createClient() {
        assert(chatClient == null)

        val props = ChatClient.Properties.Builder()
                .setRegion(realm)
                .setDeferCertificateTrustToPlatform(!pinCerts)
                .createProperties()

        ChatClient.create(context.applicationContext,
                accessToken!!,
                props,
                this)
    }

    fun shutdown() {
        chatClient!!.shutdown()
        chatClient = null // Client no longer usable after shutdown()
        notifyLogoutFinished()
    }

    // Client created, remember the reference and set up UI
    override fun onSuccess(client: ChatClient) {
        debug { "Received completely initialized ChatClient" }
        chatClient = client

        if (fcmToken != null) {
            setupFcmToken()
        }

        notifyLoginFinished()
    }

    // Client not created, fail
    override fun onError(errorInfo: ErrorInfo?) {
        TwilioApplication.instance.logErrorInfo("Login error", errorInfo!!)
        chatClient = null

        notifyLoginError(errorInfo.toString())
    }

    // Token expiration events

    override fun onTokenAboutToExpire() {
        if (chatClient != null) {
            TwilioApplication.instance.showToast("Token will expire in 3 minutes. Getting new token.")
            getAccessToken()
        }
    }

    override fun onTokenExpired() {
        accessToken = null
        if (chatClient != null) {
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
            if (chatClient == null) {
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
            if (chatClient == null) {
                // Create client with accessToken
                createClient()
            } else {
                // Client already exists, so set accessToken to it
                chatClient!!.updateToken(accessToken, ToastStatusListener(
                        "Client Update Token was successfull",
                        "Client Update Token failed"))
            }
        }
    }

    override fun onChannelAdded(p0: Channel?) {}
    override fun onChannelDeleted(p0: Channel?) {}
    override fun onChannelInvited(p0: Channel?) {}
    override fun onChannelJoined(p0: Channel?) {}
    override fun onChannelSynchronizationChange(p0: Channel?) {}
    override fun onChannelUpdated(p0: Channel?, p1: Channel.UpdateReason?) {}
    override fun onClientSynchronization(p0: ChatClient.SynchronizationStatus?) {}
    override fun onConnectionStateChange(p0: ChatClient.ConnectionState?) {}
    override fun onRemovedFromChannelNotification(p0: String?) {}
    override fun onUserSubscribed(p0: User?) {}
    override fun onUserUnsubscribed(p0: User?) {}
    override fun onUserUpdated(p0: User?, p1: User.UpdateReason?) {}
    override fun onAddedToChannelNotification(p0: String?) {}
    override fun onInvitedToChannelNotification(p0: String?) {}
    override fun onNewMessageNotification(p0: String?, p1: String?, p2: Long) {}
    override fun onNotificationFailed(p0: ErrorInfo?) {}
    override fun onNotificationSubscribed() {}
}
