package com.twilio.conversations.demo.activities

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.demo.BasicConversationsClient.LoginListener
import com.twilio.conversations.demo.R
import com.twilio.conversations.demo.BuildConfig
import android.net.Uri
import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.preference.PreferenceManager
import android.view.View
import android.widget.ArrayAdapter
import com.twilio.conversations.demo.TwilioApplication
import com.twilio.conversations.demo.services.RegistrationIntentService
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*

class LoginActivity : Activity(), LoginListener, AnkoLogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        login.setOnClickListener {
            val userName = clientNameTextBox.text.toString()
            val certPinningChosen = certPinning.isChecked
            val realm = realmSelect.selectedItem as String
            val ttl = tokenTtlTextBox.text.toString()

            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            sharedPreferences.edit()
                .putString("userName", userName)
                .putBoolean("pinCerts", certPinningChosen)
                .putString("realm", realm)
                .putString("ttl", ttl)
                .apply()

            val url = Uri.parse(BuildConfig.ACCESS_TOKEN_SERVICE_URL)
                    .buildUpon()
                    .appendQueryParameter("identity", userName)
                    .appendQueryParameter("realm", realm)
                    .appendQueryParameter("ttl", ttl)
                    .build()
                    .toString()
            debug { "url string : $url" }

            TwilioApplication.instance.basicClient.login(userName, certPinningChosen, realm, url, this@LoginActivity)
        }

        if (checkPlayServices()) {
            fcmAvailable.isChecked = true
            // Start IntentService to register this application with GCM.
            startService<RegistrationIntentService>()
        }
    }

    override fun onResume() {
        super.onResume()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val userName = sharedPreferences.getString("userName", DEFAULT_CLIENT_NAME)
        val certPin = sharedPreferences.getBoolean("pinCerts", true)
        val realm = sharedPreferences.getString("realm", DEFAULT_REALM)
        val ttl = sharedPreferences.getString("ttl", DEFAULT_TTL)

        clientNameTextBox.setText(userName)
        certPinning.isChecked = certPin
        realmSelect.setSelection((realmSelect.adapter as ArrayAdapter<String>).getPosition(realm))
        tokenTtlTextBox.setText(ttl)

        // Make sure no client is created
        if (TwilioApplication.instance.basicClient.conversationsClient != null) {
            TwilioApplication.instance.basicClient.shutdown()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_about) {
            showAboutDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        alert("Version: ${ConversationsClient.getSdkVersion()}", "About") {
            positiveButton("OK") { dialog -> dialog.cancel() }
        }.show()
    }

    fun setLoginProgressVisible(enable: Boolean) {
        if (enable) {
            loginInputsLayout.visibility = View.GONE
            loginProgressLayout.visibility = View.VISIBLE
        } else {
            loginInputsLayout.visibility = View.VISIBLE
            loginProgressLayout.visibility = View.GONE
        }
    }

    override fun onLoginStarted() {
        debug { "Log in started" }
        setLoginProgressVisible(true)
    }

    override fun onLoginFinished() {
        setLoginProgressVisible(false)
        startActivity<ChannelActivity>()
    }

    override fun onLoginError(errorMessage: String) {
        setLoginProgressVisible(false)
        TwilioApplication.instance.showToast("Error logging in : " + errorMessage, Toast.LENGTH_LONG)
    }

    override fun onLogoutFinished() {
        setLoginProgressVisible(false)
        TwilioApplication.instance.showToast("Log out finished")
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show()
            } else {
                info { "This device is not supported." }
                finish()
            }
            return false
        }
        return true
    }

    companion object {
        private val DEFAULT_CLIENT_NAME = "TestUser"
        private val DEFAULT_REALM = "us1"
        private val DEFAULT_TTL = "3000"
        private val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
    }
}
