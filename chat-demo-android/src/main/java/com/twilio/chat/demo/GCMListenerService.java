package com.twilio.chat.demo;

import java.util.HashMap;

import com.google.android.gms.gcm.GcmListenerService;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.twilio.chat.ChatClient;
import com.twilio.chat.NotificationPayload;

public class GCMListenerService extends GcmListenerService
{
    private static final Logger logger = Logger.getLogger(GCMListenerService.class);

    @Override
    public void onMessageReceived(String from, Bundle data)
    {
        logger.d("onMessageReceived for GCM");
        HashMap<String, String> pushNotification = new HashMap<String, String>();
        for (String key : data.keySet()) {
            pushNotification.put(key, data.getString(key));
        }
        ChatClient client = TwilioApplication.get().getBasicClient().getChatClient();
        if (client != null) {
            client.handleNotification(pushNotification);
        }
        notify(data);
    }

    private void notify(Bundle bundle)
    {
        NotificationPayload payload = new NotificationPayload(bundle);

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
            intent.putExtra("C_SID", cSid);
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
}
