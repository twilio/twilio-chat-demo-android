package com.twilio.chat.demo

import com.twilio.accessmanager.AccessManager
import com.twilio.chat.CallbackListener
import com.twilio.chat.ChatClient
import com.twilio.chat.ErrorInfo
import com.twilio.chat.internal.HandlerUtil
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import ToastStatusListener
import org.jetbrains.anko.*

class BasicChatClient(private val context: Context) : CallbackListener<ChatClient>(), AccessManager.Listener, AccessManager.TokenUpdateListener, AnkoLogger {
    private var accessToken: String? = null
    private var fcmToken: String? = null

    var chatClient: ChatClient? = null
        private set
    private var accessManager: AccessManager? = null

    private var loginListener: LoginListener? = null
    private var loginListenerHandler: Handler? = null

    private var urlString: String? = null
    private var username: String? = null

    init {

        if (BuildConfig.DEBUG) {
            warn { "Enabling DEBUG logging" }
            ChatClient.setLogLevel(android.util.Log.DEBUG)
        }
    }

    interface LoginListener {
        fun onLoginStarted()

        fun onLoginFinished()

        fun onLoginError(errorMessage: String)

        fun onLogoutFinished()
    }

    fun setFCMToken(fcmToken: String) {
        warn { "setFCMToken $fcmToken" }
        this.fcmToken = fcmToken
        if (chatClient != null) {
            setupFcmToken()
        }
    }

    fun login(username: String, url: String, listener: LoginListener) {
        if (username === this.username
                && urlString === url
                && loginListener === listener
                && chatClient != null
                && accessManager != null
                && !accessManager!!.isTokenExpired) {
            onSuccess(chatClient!!)
            return
        }

        this.username = username
        urlString = url

        loginListenerHandler = HandlerUtil.setupListenerHandler()
        loginListener = listener

        GetAccessTokenAsyncTask().execute(urlString)
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

    private fun createAccessManager() {
        if (accessManager != null) return

        accessManager = AccessManager(accessToken, this)
        accessManager!!.addTokenUpdateListener(this)
    }

    private fun createClient() {
        if (chatClient != null) return

        val props = ChatClient.Properties.Builder()
                .setRegion("us1")
                .createProperties()

        ChatClient.create(context.applicationContext,
                accessToken!!,
                props,
                this)
    }

    fun shutdown() {
        chatClient!!.shutdown()
        chatClient = null // Client no longer usable after shutdown()
    }

    // Client created, remember the reference and set up UI
    override fun onSuccess(client: ChatClient) {
        debug { "Received completely initialized ChatClient" }
        chatClient = client

        if (fcmToken != null) {
            setupFcmToken()
        }

        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLoginFinished()
            }
        }
    }

    // Client not created, fail
    override fun onError(errorInfo: ErrorInfo?) {
        TwilioApplication.instance.logErrorInfo("Login error", errorInfo!!)

        loginListenerHandler!!.post {
            if (loginListener != null) {
                loginListener!!.onLoginError(errorInfo.toString())
            }
        }
    }

    // AccessManager.Listener

    override fun onTokenWillExpire(accessManager: AccessManager) {
        TwilioApplication.instance.showToast("Token will expire in 3 minutes. Getting new token.")
        GetAccessTokenAsyncTask().execute(urlString)
    }

    override fun onTokenExpired(accessManager: AccessManager) {
        TwilioApplication.instance.showToast("Token expired. Getting new token.")
        GetAccessTokenAsyncTask().execute(urlString)
    }

    override fun onError(accessManager: AccessManager, err: String) {
        TwilioApplication.instance.showToast("AccessManager error: " + err)
    }

    // AccessManager.TokenUpdateListener

    override fun onTokenUpdated(token: String) {
        if (chatClient == null) return

        chatClient!!.updateToken(token, ToastStatusListener(
                "Client Update Token was successfull",
                "Client Update Token failed"))
    }

    /**
     * Modify this method if you need to provide more information to your Access Token Service.
     */
    //TODO coroutines
    private inner class GetAccessTokenAsyncTask : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            loginListenerHandler!!.post {
                if (loginListener != null) {
                    loginListener!!.onLoginStarted()
                }
            }
        }

        override fun doInBackground(vararg params: String): String? {
            try {
                accessToken = HttpHelper.httpGet(params[0])
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return accessToken
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            createAccessManager()
            createClient()
            accessManager!!.updateToken(accessToken)
        }
    }
}
