package com.twilio.chat.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.twilio.chat.Channel;
import com.twilio.chat.Channel.ChannelType;
import com.twilio.chat.ChannelListener;
import com.twilio.chat.Channels;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.StatusListener;
import com.twilio.chat.ChatClientListener;
import com.twilio.chat.Member;
import com.twilio.chat.Message;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.UserInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import uk.co.ribot.easyadapter.EasyAdapter;
import org.json.JSONObject;
import org.json.JSONException;

@SuppressLint("InflateParams")
public class ChannelActivity extends Activity implements ChatClientListener
{
    private static final Logger logger = Logger.getLogger(ChannelActivity.class);

    private static final String[] CHANNEL_OPTIONS = { "Join" };

    private static final int JOIN = 0;

    private ListView               listView;
    private BasicChatClient basicClient;
    private List<Channel>          channels = new ArrayList<Channel>();
    private EasyAdapter<Channel>   adapter;
    private AlertDialog            createChannelDialog;
    private Channels               channelsObject;

    private static final Handler handler = new Handler();
    private AlertDialog          incomingChannelInvite;
    private StatusListener       joinListener;
    private StatusListener       declineInvitationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel);
        basicClient = TwilioApplication.get().getBasicClient();
        basicClient.getChatClient().setListener(ChannelActivity.this);
        setupListView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        handleIncomingIntent(getIntent());
        getChannels();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.channel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_create_public:
                showCreateChannelDialog(ChannelType.PUBLIC);
                break;
            case R.id.action_create_private:
                showCreateChannelDialog(ChannelType.PRIVATE);
                break;
            case R.id.action_create_public_withoptions:
                createChannelWithType(ChannelType.PUBLIC);
                break;
            case R.id.action_create_private_withoptions:
                createChannelWithType(ChannelType.PRIVATE);
                break;
            case R.id.action_search_by_unique_name:
                showSearchChannelDialog();
                break;
            case R.id.action_user_info:
                startActivity(new Intent(getApplicationContext(), UserInfoActivity.class));
                break;
            case R.id.action_logout:
                basicClient.shutdown();
                finish();
                break;
            case R.id.action_unregistercm: {
                String gcmToken = basicClient.getGCMToken();
                basicClient.getChatClient().unregisterGCMToken(
                    gcmToken, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            logger.w("GCM unregistration not successful");
                            runOnUiThread(new Runnable() {
                                public void run()
                                {
                                    Toast
                                        .makeText(ChannelActivity.this,
                                                  "GCM unregistration not successful",
                                                  Toast.LENGTH_SHORT)
                                        .show();
                                }
                            });
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("GCM unregistration successful");
                            runOnUiThread(new Runnable() {
                                public void run()
                                {
                                    Toast
                                        .makeText(ChannelActivity.this,
                                                  "GCM unregistration successful",
                                                  Toast.LENGTH_SHORT)
                                        .show();
                                }
                            });
                        }
                    });
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createChannelWithType(ChannelType type)
    {
        Random rand = new Random();
        int    value = rand.nextInt(50);

        final JSONObject attrs = new JSONObject();
        try {
            attrs.put("topic", "testing channel creation with options " + value);
        } catch (JSONException xcp) {
            logger.e("JSON exception", xcp);
        }

        basicClient.getChatClient().getChannels()
            .channelBuilder()
            .withFriendlyName("Pub_TestChannelF_" + value)
            .withUniqueName("Pub_TestChannelU_" + value)
            .withType(type)
            .withAttributes(attrs)
            .build(new CreateChannelListener() {
                @Override
                public void onCreated(final Channel newChannel)
                {
                    logger.d("Successfully created a channel with options.");
                }

                @Override
                public void onError(ErrorInfo errorInfo)
                {
                    logger.e("Error creating a channel");
                }
            });
    }

    private boolean handleIncomingIntent(Intent intent)
    {
        if (intent != null) {
            Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
            String  action = intent.getStringExtra(Constants.EXTRA_ACTION);
            intent.removeExtra(Constants.EXTRA_CHANNEL);
            intent.removeExtra(Constants.EXTRA_ACTION);
            if (action != null) {
                if (action.compareTo(Constants.EXTRA_ACTION_INVITE) == 0) {
                    this.showIncomingInvite(channel);
                }
            }
        }
        return false;
    }

    private void showCreateChannelDialog(final ChannelType type)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
        String              title = "Enter " + type.toString() + " name";

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_add_channel, null))
            .setTitle(title)
            .setPositiveButton(
                "Create",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String channelName =
                            ((EditText)createChannelDialog.findViewById(R.id.channel_name))
                                .getText()
                                .toString();
                        logger.d("Creating channel with friendly Name|" + channelName + "|");
                        channelsObject.createChannel(channelName, type, new CreateChannelListener() {
                            @Override
                            public void onCreated(final Channel newChannel)
                            {
                                logger.d("Successfully created a channel");
                                if (newChannel != null) {
                                    final String sid = newChannel.getSid();
                                    ChannelType  type = newChannel.getType();
                                    logger.d("Channel created with sid|" + sid + "| and type |"
                                             + type.toString()
                                             + "|");
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().showError("Error creating channel",
                                                                     errorInfo);
                            }
                        });
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        createChannelDialog = builder.create();
        createChannelDialog.show();
    }

    private void showSearchChannelDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
        String              title = "Enter unique channel name";

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_search_channel, null))
            .setTitle(title)
            .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    String channelName =
                        ((EditText)createChannelDialog.findViewById(R.id.channel_name))
                            .getText()
                            .toString();
                    logger.d("Searching for " + channelName);
                    final Channel channel = channelsObject.getChannelByUniqueName(channelName);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (channel != null) {
                                TwilioApplication.get().showToast(channel.getSid() + ":" + channel.getFriendlyName());
                            } else {
                                TwilioApplication.get().showToast("Channel not found.");
                            }
                        }
                    });
                }
            });
        createChannelDialog = builder.create();
        createChannelDialog.show();
    }

    private void setupListView()
    {
        listView = (ListView)findViewById(R.id.channel_list);
        adapter = new EasyAdapter<Channel>(
            this,
            ChannelViewHolder.class,
            channels,
            new ChannelViewHolder.OnChannelClickListener() {
                @Override
                public void onChannelClicked(final Channel channel)
                {
                    if (channel.getStatus() == Channel.ChannelStatus.JOINED) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run()
                            {
                                Intent i =
                                    new Intent(ChannelActivity.this, MessageActivity.class);
                                i.putExtra(Constants.EXTRA_CHANNEL,
                                           (Parcelable)channel);
                                i.putExtra(Constants.EXTRA_CHANNEL_SID, channel.getSid());
                                startActivity(i);
                            }
                        }, 0);
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChannelActivity.this);
                    builder.setTitle("Select an option")
                        .setItems(CHANNEL_OPTIONS, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if (which == JOIN) {
                                    dialog.cancel();
                                    joinListener = new StatusListener() {
                                        @Override
                                        public void onError(ErrorInfo errorInfo)
                                        {
                                            TwilioApplication.get().showError(
                                                "failed to join channel", errorInfo);
                                        }

                                        @Override
                                        public void onSuccess()
                                        {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run()
                                                {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            });
                                            logger.d("Successfully joined channel");
                                        }

                                    };
                                    channel.join(joinListener);
                                }
                            }
                        });
                    builder.show();
                }
            });

        listView.setAdapter(adapter);
        getChannels();
        adapter.notifyDataSetChanged();
    }

    // Initialize channels with channel list
    private void getChannels()
    {
        if (channels == null) return;
        if (basicClient == null || basicClient.getChatClient() == null) return;

        channelsObject = basicClient.getChatClient().getChannels();

        channels.clear();
        channels.addAll(new ArrayList<>(Arrays.asList(channelsObject.getChannels())));

        Collections.sort(channels, new CustomChannelComparator());
    }

    private void showIncomingInvite(final Channel channel)
    {
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                if (incomingChannelInvite == null) {
                    incomingChannelInvite =
                        new AlertDialog.Builder(ChannelActivity.this)
                            .setTitle(R.string.incoming_call)
                            .setMessage(R.string.incoming_call_message)
                            .setPositiveButton(
                                R.string.join,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        channel.join(new StatusListener() {
                                            @Override
                                            public void onError(ErrorInfo errorInfo)
                                            {
                                                TwilioApplication.get().showError(
                                                        "Failed to join channel", errorInfo);
                                            }

                                            @Override
                                            public void onSuccess()
                                            {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run()
                                                    {
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                });
                                                logger.d("Successfully joined channel");
                                            }
                                        });
                                        incomingChannelInvite = null;
                                    }
                                })
                            .setNegativeButton(
                                R.string.decline,
                                new DialogInterface.OnClickListener() {

                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        declineInvitationListener = new StatusListener() {

                                            @Override
                                            public void onError(ErrorInfo errorInfo)
                                            {
                                                TwilioApplication.get().showError(
                                                        "Failed to decline channel invite", errorInfo);
                                            }

                                            @Override
                                            public void onSuccess()
                                            {
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run()
                                                    {
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                });
                                                logger.d("Successfully declined channel invite");
                                            }

                                        };
                                        channel.declineInvitation(declineInvitationListener);
                                        incomingChannelInvite = null;
                                    }
                                })
                            .create();
                    incomingChannelInvite.show();
                }
            }
        });
    }

    private class CustomChannelComparator implements Comparator<Channel>
    {
        @Override
        public int compare(Channel lhs, Channel rhs)
        {
            return lhs.getFriendlyName().compareTo(rhs.getFriendlyName());
        }
    }

    //=============================================================
    // ChatClientListener
    //=============================================================

    @Override
    public void onChannelAdd(final Channel channel)
    {
        logger.d("Received onChannelAdd callback for channel |" + channel.getFriendlyName() + "|");
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                channels.add(channel);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onChannelChange(Channel channel)
    {
        logger.d("Received onChannelChange callback for channel |" + channel.getFriendlyName()
                + "|");
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onChannelDelete(final Channel channel)
    {
        logger.d("Received onChannelDelete callback for channel |" + channel.getFriendlyName()
                + "|");
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                channels.remove(channel);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onChannelSynchronizationChange(Channel channel)
    {
        logger.e("Received onChannelSynchronizationChange callback for channel |"
                 + channel.getFriendlyName()
                 + "|");
    }

    @Override
    public void onClientSynchronization(ChatClient.SynchronizationStatus status)
    {
        logger.e("Received onClientSynchronization callback " + status.toString());
    }

    @Override
    public void onUserInfoChange(UserInfo userInfo)
    {
        logger.e("Received onUserInfoChange callback");
    }

    @Override
    public void onToastNotification(String channelId, String messageId)
    {
        logger.d("Received new push notification");
        TwilioApplication.get().showToast("Received new push notification");
    }

    @Override
    public void onToastSubscribed()
    {
        logger.d("Subscribed to push notifications");
        TwilioApplication.get().showToast("Subscribed to push notifications");
    }

    @Override
    public void onToastFailed(ErrorInfo errorInfo)
    {
        logger.d("Failed to subscribe to push notifications");
        TwilioApplication.get().showError("Failed to subscribe to push notifications", errorInfo);
    }

    @Override
    public void onError(ErrorInfo errorInfo)
    {
        TwilioApplication.get().showError("Received onError callback", errorInfo);
    }
}
