package com.twilio.chat.demo.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat

import com.google.firebase.messaging.FirebaseMessagingService

import com.google.firebase.messaging.RemoteMessage
import com.twilio.chat.ChatClient
import com.twilio.chat.NotificationPayload

import org.json.JSONObject

import timber.log.Timber

class FCMListenerService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        Timber.d("onMessageReceived for FCM")

        Timber.d("From: " + remoteMessage!!.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.size > 0) {
            Timber.d("Data Message Body: " + remoteMessage.data)

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

            val notification = NotificationCompat.Builder(this)
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
                Timber.d("Playing specified sound " + soundFileName)
            } else {
                notification.defaults = notification.defaults or Notification.DEFAULT_SOUND
                Timber.d("Playing default sound")
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(0, notification)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Timber.d("Notification Message Body: " + remoteMessage.notification.body!!)
            Timber.e("We do not parse notification body - leave it to system")
        }
    }
}
