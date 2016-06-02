package com.twilio.ipmessaging.demo;

import java.util.HashMap;

import com.google.android.gms.gcm.GcmListenerService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GCMListenerService extends GcmListenerService {

	public static final String TAG = "GCMListenerService";
	private BasicIPMessagingClient chatClient;
	
	@Override
	public void onMessageReceived(String from, Bundle data) {
		Log.d(TAG, "onMessageReceived for GCM");
		HashMap<String, String> pushNotification = new HashMap<String, String>();
		for( String key : data.keySet() ){
			pushNotification.put(key, data.getString(key));
		}
		chatClient = TwilioApplication.get().getBasicClient();
		chatClient.getIpMessagingClient().handleNotification(pushNotification);
		notify(data);
	}

	private void notify(Bundle bundle) {

		Intent intent = new Intent(this, MessageActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (bundle.containsKey("channel_id")) {
			intent.putExtra("C_SID", bundle.getString("channel_id"));
		}

		String message = "";
		if (bundle.containsKey("channel_id")) {
			message = bundle.getString("text_message");
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_notification).setContentTitle("Twilio Notification").setContentText(message)
				.setAutoCancel(true).setContentIntent(pendingIntent).setColor(Color.rgb(214, 10, 37));
		

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(0, notificationBuilder.build());
	}

}
