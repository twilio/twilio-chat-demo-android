package com.twilio.chat.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.twilio.chat.Channel;
import com.twilio.chat.Channels;
import com.twilio.chat.StatusListener;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.ChatClientListener;
import com.twilio.chat.ChatClient;
import com.twilio.chat.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class UserActivity extends Activity
{
    private static final Logger logger = Logger.getLogger(UserActivity.class);

    static final int REQUEST_IMAGE_CAPTURE = 1;

    ChatClient  client;
    EditText    friendlyName;
    ImageView   avatarView;
    Button      save;
    Bitmap      bitmap;

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.channel, menu);
        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        setListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        friendlyName = (EditText)findViewById(R.id.user_friendly_name);
        client = TwilioApplication.get().getBasicClient().getChatClient();

        final User user = client.getUsers().getMyUser();

        friendlyName.setText(user.getFriendlyName());
        avatarView = (ImageView)findViewById(R.id.avatar);
        save = (Button)findViewById(R.id.user_info_save);

        logger.i("message client initialized");
        Channels channels = client.getChannels();
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (!user.getFriendlyName().equals(
                        friendlyName.getText().toString())) {
                    user.setFriendlyName(
                        friendlyName.getText().toString(), new ToastStatusListener(
                            "Update successful for user friendlyName",
                            "Update failed for user friendlyName"));
                }
                if (bitmap != null) {
                    JSONObject attributes = new JSONObject();
                    try {
                        attributes.put("avatar", getBase64FromBitmap(bitmap));
                    } catch (JSONException ignored) {
                        // whatever?
                    }
                    user.setAttributes(attributes, new ToastStatusListener(
                        "Update successful for user attributes",
                        "Update failed for user attributes") {
                        @Override
                        public void onSuccess()
                        {
                            super.onSuccess();
                            fillUserAvatar();
                        }
                    });
                }
            }
        });

        fillUserAvatar();
        avatarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dispatchTakePictureIntent();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap)extras.get("data");
            bitmap = getResizedBitmap(imageBitmap, 96);
            avatarView.setImageBitmap(bitmap);
        }
    }

    public String getBase64FromBitmap(Bitmap bitmap)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        String string = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
        return string;
    }

    private void dispatchTakePictureIntent()
    {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void fillUserAvatar()
    {
        final User user = client.getUsers().getMyUser();
        JSONObject attributes = user.getAttributes();
        String     avatar = (String)attributes.opt("avatar");
        if (avatar != null) {
            byte[] data = Base64.decode(avatar, Base64.NO_WRAP);
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            avatarView.setImageBitmap(bitmap);
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int minSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float)height;
        if (bitmapRatio <= 1) {
            width = minSize;
            height = (int)(width / bitmapRatio);
        } else {
            height = minSize;
            width = (int)(height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void setListener()
    {
        client.setListener(new ChatClientListener() {
            @Override
            public void onChannelAdded(Channel channel)
            {
            }

            @Override
            public void onChannelUpdated(Channel channel, Channel.UpdateReason reason)
            {
            }

            @Override
            public void onChannelDeleted(Channel channel)
            {
            }

            @Override
            public void onChannelInvited(Channel channel)
            {
            }

            @Override
            public void onChannelJoined(Channel channel)
            {
            }

            @Override
            public void onError(ErrorInfo error)
            {
                TwilioApplication.get().showError(error);
                TwilioApplication.get().logErrorInfo("Error listening for userInfoChange", error);
            }

            @Override
            public void onChannelSynchronizationChange(Channel channel)
            {
            }

            @Override
            public void onUserUpdated(User user, User.UpdateReason reason)
            {
                if (reason == User.UpdateReason.ATTRIBUTES) {
                    fillUserAvatar();
                }
                TwilioApplication.get().showToast("Update successful for user attributes");
            }

            @Override
            public void onUserSubscribed(User user)
            {
            }

            @Override
            public void onUserUnsubscribed(User user)
            {
            }

            @Override
            public void onClientSynchronization(
                ChatClient.SynchronizationStatus synchronizationStatus)
            {
            }

            @Override
            public void onNotification(String channelId, String messageId)
            {
            }

            @Override
            public void onNotificationSubscribed()
            {
            }

            @Override
            public void onNotificationFailed(ErrorInfo errorInfo)
            {
            }

            @Override
            public void onConnectionStateChange(ChatClient.ConnectionState connectionState) {
            }
        });
    }
}
