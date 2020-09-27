package com.twilio.conversations.demo.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.Menu
import com.twilio.conversations.Conversation
import com.twilio.conversations.ErrorInfo
import com.twilio.conversations.ConversationsClientListener
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.User
import com.twilio.conversations.demo.R
import ToastStatusListener
import com.twilio.conversations.Attributes
import com.twilio.conversations.demo.TwilioApplication
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlinx.android.synthetic.main.activity_user_info.*

class UserActivity : Activity() {
    internal var client: ConversationsClient? = null
    internal var bitmap: Bitmap? = null

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.channel, menu)
        return true
    }

    public override fun onResume() {
        super.onResume()
        setListener()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_info)

        client = TwilioApplication.instance.basicClient.conversationsClient

        val user = client?.myUser

        if (user == null) return

        user_friendly_name.setText(user.friendlyName)

        user_info_save.setOnClickListener {
            if (user.friendlyName != user_friendly_name.text.toString()) {
                user.setFriendlyName(
                        user_friendly_name.text.toString(), ToastStatusListener(
                        "Update successful for user friendlyName",
                        "Update failed for user friendlyName"))
            }
            if (bitmap != null) {
                val attributes = JSONObject()
                try {
                    attributes.put("avatar", getBase64FromBitmap(bitmap!!))
                } catch (ignored: JSONException) {
                    // whatever?
                }

                user.setAttributes(Attributes(attributes), ToastStatusListener(
                        "Update successful for user attributes",
                        "Update failed for user attributes") {
                    fillUserAvatar()
                })
            }
        }

        fillUserAvatar()
        avatar.setOnClickListener { dispatchTakePictureIntent() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data.extras!!
            val imageBitmap = extras.get("data") as Bitmap
            bitmap = getResizedBitmap(imageBitmap, 96)
            avatar.setImageBitmap(bitmap)
        }
    }

    fun getBase64FromBitmap(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val string = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        return string
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun fillUserAvatar() {
        val user = client?.myUser
        val attributes = user?.attributes
        val ava = attributes?.jsonObject?.opt("avatar") as String?
        if (ava != null) {
            val data = Base64.decode(ava, Base64.NO_WRAP)
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            avatar.setImageBitmap(bitmap)
        }
    }

    fun getResizedBitmap(image: Bitmap, minSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio <= 1) {
            width = minSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = minSize
            width = (height * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    private fun setListener() {
        client?.addListener(object : ConversationsClientListener {
            override fun onConversationAdded(channel: Conversation) {}

            override fun onConversationUpdated(channel: Conversation, reason: Conversation.UpdateReason) {}

            override fun onConversationDeleted(channel: Conversation) {}

            override fun onError(error: ErrorInfo) {
                TwilioApplication.instance.showError("Error listening for userInfoChange", error)
            }

            override fun onConversationSynchronizationChange(channel: Conversation) {}

            override fun onUserUpdated(user: User, reason: User.UpdateReason) {
                if (reason == User.UpdateReason.ATTRIBUTES) {
                    fillUserAvatar()
                }
                TwilioApplication.instance.showToast("Update successful for user attributes")
            }

            override fun onUserSubscribed(user: User) {}

            override fun onUserUnsubscribed(user: User) {}

            override fun onClientSynchronization(synchronizationStatus: ConversationsClient.SynchronizationStatus) {}

            override fun onNewMessageNotification(channelSid: String?, messageSid: String?, messageIndex: Long) {}
            override fun onAddedToConversationNotification(channelSid: String?) {}
            override fun onRemovedFromConversationNotification(channelSid: String?) {}

            override fun onNotificationSubscribed() {}

            override fun onNotificationFailed(errorInfo: ErrorInfo) {}

            override fun onConnectionStateChange(connectionState: ConversationsClient.ConnectionState) {}

            override fun onTokenExpired() {
                TwilioApplication.instance.basicClient.onTokenExpired()
            }

            override fun onTokenAboutToExpire() {
                TwilioApplication.instance.basicClient.onTokenAboutToExpire()
            }
        })
    }

    companion object {
        internal val REQUEST_IMAGE_CAPTURE = 1
    }
}
