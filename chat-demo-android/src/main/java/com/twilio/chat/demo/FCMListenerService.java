package com.twilio.chat.demo;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;

import com.google.firebase.messaging.RemoteMessage;
import com.twilio.chat.ChatClient;
import com.twilio.chat.NotificationPayload;
import com.twilio.chat.demo.Logger;
import com.twilio.chat.demo.TwilioApplication;

import org.json.JSONObject;

public class FCMListenerService extends FirebaseMessagingService {
    private static final Logger logger = Logger.getLogger(FCMListenerService.class);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

        logger.d("onMessageReceived for FCM");

        logger.d("From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            logger.d("Data Message Body: " + remoteMessage.getData());

            NotificationPayload payload = new NotificationPayload(remoteMessage.getData());

            ChatClient client = TwilioApplication.get().getBasicClient().getChatClient();
            if (client != null) {
                client.handleNotification(payload);
            }

            NotificationPayload.Type type = payload.getType();

            if (type == NotificationPayload.Type.UNKNOWN) return; // Ignore everything we don't support

            String title = "Twilio Notification";

            if (type == NotificationPayload.Type.NEW_MESSAGE)
                title = "Twilio: New Message";
            if (type == NotificationPayload.Type.ADDED_TO_CHANNEL)
                title = "Twilio: Added to Channel";
            if (type == NotificationPayload.Type.INVITED_TO_CHANNEL)
                title = "Twilio: Invited to Channel";
            if (type == NotificationPayload.Type.REMOVED_FROM_CHANNEL)
                title = "Twilio: Removed from Channel";

            // Set up action Intent
            Intent intent = new Intent(this, MessageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            String cSid = payload.getChannelSid();
            if (!"".contentEquals(cSid)) {
                intent.putExtra(Constants.EXTRA_CHANNEL_SID, cSid);
            }

            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

            Notification notification =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(payload.getBody())
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setColor(Color.rgb(214, 10, 37))
                            .build();

            String soundFileName = payload.getSound();
            if (getResources().getIdentifier(soundFileName, "raw", getPackageName()) != 0) {
                Uri sound = Uri.parse("android.resource://" + getPackageName() + "/raw/" + soundFileName);
                notification.defaults &= ~Notification.DEFAULT_SOUND;
                notification.sound = sound;
                logger.d("Playing specified sound "+soundFileName);
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
                logger.d("Playing default sound");
            }

            NotificationManager notificationManager =
                    (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0, notification);
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            logger.d("Notification Message Body: " + remoteMessage.getNotification().getBody());
            logger.e("We do not parse notification body - leave it to system");
        }
    }
}
