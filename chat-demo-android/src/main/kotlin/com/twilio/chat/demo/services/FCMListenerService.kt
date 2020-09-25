package com.twilio.conversations.demo.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.twilio.chat.NotificationPayload
import com.twilio.chat.demo.Constants
import com.twilio.chat.demo.R
import com.twilio.chat.demo.TwilioApplication
import com.twilio.chat.demo.activities.MessageActivity
import org.jetbrains.anko.*

class FCMListenerService : FirebaseMessagingService(), AnkoLogger {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        if (remoteMessage == null) return;

        debug { "onMessageReceived for FCM from: ${remoteMessage.from}" }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            debug { "Data Message Body: ${remoteMessage.data}" }

            val payload = NotificationPayload(remoteMessage.data)

            val client = TwilioApplication.instance.basicClient.chatClient
            client?.handleNotification(payload)

            val type = payload.type

            if (type == NotificationPayload.Type.UNKNOWN) return  // Ignore everything we don't support

            var title = "Twilio Notification"

            if (type == NotificationPayload.Type.NEW_MESSAGE)
                title = "Twilio: New Message"
            if (type == NotificationPayload.Type.ADDED_TO_CHANNEL)
                title = "Twilio: Added to Channel"
            if (type == NotificationPayload.Type.INVITED_TO_CHANNEL)
                title = "Twilio: Invited to Channel"
            if (type == NotificationPayload.Type.REMOVED_FROM_CHANNEL)
                title = "Twilio: Removed from Channel"

            // Set up action Intent
            val intent = Intent(this, MessageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val cSid = payload.channelSid
            if (!"".contentEquals(cSid)) {
                intent.putExtra(Constants.EXTRA_CHANNEL_SID, cSid)
            }

            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

            val notification = NotificationCompat.Builder(this, /*NotificationChannel.DEFAULT_CHANNEL_ID*/"miscellaneous")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(payload.body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setColor(Color.rgb(214, 10, 37))
                    .build()

            val soundFileName = payload.sound
            if (resources.getIdentifier(soundFileName, "raw", packageName) != 0) {
                val sound = Uri.parse("android.resource://$packageName/raw/$soundFileName")
                notification.defaults = notification.defaults and Notification.DEFAULT_SOUND.inv()
                notification.sound = sound
                debug { "Playing specified sound $soundFileName" }
            } else {
                notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
                debug { "Playing default sound" }
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(0, notification)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            //debug { "Notification Message Body: ${remoteMessage.notification.body}" }
            error { "We do not parse notification body - leave it to system" }
        }
    }
}
