package com.twilio.ipmessaging.demo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.twilio.ipmessaging.demo.R;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Channel.ChannelType;
import com.twilio.ipmessaging.ChannelListener;
import com.twilio.ipmessaging.Channels;
import com.twilio.ipmessaging.Constants;
import com.twilio.ipmessaging.Constants.CreateChannelListener;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.IPMessagingClientListener;
import com.twilio.ipmessaging.Member;
import com.twilio.ipmessaging.Message;
import com.twilio.ipmessaging.TwilioIPMessagingSDK;

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

@SuppressLint("InflateParams")
public class ChannelActivity extends Activity implements ChannelListener, IPMessagingClientListener
{
    private static final String[] CHANNEL_OPTIONS = { "Join" };
    private static final Logger logger = Logger.getLogger(ChannelActivity.class);
    private static final int    JOIN = 0;

    private ListView               listView;
    private BasicIPMessagingClient basicClient;
    private List<Channel>          channels = new ArrayList<Channel>();
    private EasyAdapter<Channel>   adapter;
    private AlertDialog            createChannelDialog;
    private Channels               channelsObject;
    private Channel[] channelArray;

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
        if (basicClient != null && basicClient.getIpMessagingClient() != null) {
            basicClient.getIpMessagingClient().setListener(ChannelActivity.this);
            setupListView();
        }
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
                showCreateChannelDialog(ChannelType.CHANNEL_TYPE_PUBLIC);
                break;
            case R.id.action_create_private:
                showCreateChannelDialog(ChannelType.CHANNEL_TYPE_PRIVATE);
                break;
            case R.id.action_create_public_withoptions: {
                Random   rand = new Random();
                int      value = rand.nextInt(50);
                Channels channelsLocal = basicClient.getIpMessagingClient().getChannels();
                final Map<String, String> attrs = new HashMap<String, String>();
                attrs.put("topic", "testing channel creation with options");
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(Constants.CHANNEL_FRIENDLY_NAME, "Pub_TestChannelF_" + value);
                options.put(Constants.CHANNEL_UNIQUE_NAME, "Pub_TestChannelU_" + value);
                options.put(Constants.CHANNEL_TYPE, ChannelType.CHANNEL_TYPE_PUBLIC);
                options.put("attributes", attrs);
                channelsLocal.createChannel(options, new CreateChannelListener() {
                    @Override
                    public void onCreated(final Channel newChannel)
                    {
                        logger.d("Successfully created a channel with options.");
                    }

                    @Override
                    public void onError()
                    {
                        logger.e("Error creating a channel");
                    }
                });
                break;
            }
            case R.id.action_create_private_withoptions: {
                Random rand = new Random();
                int    value = rand.nextInt(50);

                Channels channelsLocal = basicClient.getIpMessagingClient().getChannels();
                Map<String, Object> options = new HashMap<String, Object>();
                options.put(Constants.CHANNEL_FRIENDLY_NAME, "Priv_TestChannelF_" + value);
                options.put(Constants.CHANNEL_UNIQUE_NAME, "Priv_TestChannelU_" + value);
                options.put(Constants.CHANNEL_TYPE, ChannelType.CHANNEL_TYPE_PUBLIC);
                channelsLocal.createChannel(null, null);
                break;
            }
            case R.id.action_search_by_unique_name: showSearchChannelDialog(); break;
            case R.id.action_logout:
                basicClient.getIpMessagingClient().shutdown();
                finish();
                break;
            case R.id.action_shutdown:
                TwilioIPMessagingSDK.shutdown();
                finish();
                break;
            case R.id.action_unregistercm:
                String gcmToken = basicClient.getGCMToken();
                basicClient.getIpMessagingClient().unregisterGCMToken(
                    gcmToken, new StatusListener() {

                        @Override
                        public void onError()
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        handleIncomingIntent(getIntent());
        getChannels(null);
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
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();
        String         title = "Enter " + type.toString() + " name";

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_add_channel, null))
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
                        Channels channelsLocal = basicClient.getIpMessagingClient().getChannels();
                        channelsLocal.createChannel(channelName, type, new CreateChannelListener() {
                            @Override
                            public void onCreated(final Channel newChannel)
                            {
                                logger.d("Successfully created a channel");
                                if (newChannel != null) {
                                    final String sid = newChannel.getSid();
                                    ChannelType  type = newChannel.getType();
                                    newChannel.setListener(ChannelActivity.this);
                                    logger.d("Channel created|SID|" + sid + "|TYPE|"
                                             + type.toString());
                                }
                            }

                            @Override
                            public void onError()
                            {
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
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();
        String         title = "Enter unique channel name";

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.dialog_search_channel, null))
            .setTitle(title)
            .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    String channelName =
                        ((EditText)createChannelDialog.findViewById(R.id.channel_name))
                            .getText()
                            .toString();
                    logger.d(channelName);
                    Channels channelsLocal = basicClient.getIpMessagingClient().getChannels();
                    final Channel channel = channelsLocal.getChannelByUniqueName(channelName);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            if (channel != null) {
                                showToast(channel.getSid() + ":" + channel.getFriendlyName());
                            } else {
                                showToast("Channel not found.");
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
                        final Channel channelSelected = channelsObject.getChannel(channel.getSid());
                        if (channelSelected != null) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run()
                                {
                                    Intent i =
                                        new Intent(ChannelActivity.this, MessageActivity.class);
                                    i.putExtra(Constants.EXTRA_CHANNEL,
                                               (Parcelable)channelSelected);
                                    i.putExtra("C_SID", channelSelected.getSid());
                                    startActivity(i);
                                }
                            }, 0);
                        }
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
                                        public void onError()
                                        {
                                            logger.e("failed to join channel");
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
        getChannels(null);
    }

    private void getChannels(String channelId)
    {
        if (this.channels != null) {
            if (basicClient != null && basicClient.getIpMessagingClient() != null) {
                channelsObject = basicClient.getIpMessagingClient().getChannels();
                if (channelsObject != null) {
                    channelsObject.loadChannelsWithListener(new StatusListener() {
                        @Override
                        public void onError()
                        {
                            logger.d("Failed to loadChannelsWithListener");
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successfully loadChannelsWithListener.");
                            if (channels != null) {
                                channels.clear();
                            }
                            if (channelsObject != null) {
                                channelArray = channelsObject.getChannels();
                                setupListenersForChannel(channelArray);
                                if (ChannelActivity.this.channels != null && channelArray != null) {
                                    ChannelActivity.this.channels.addAll(
                                        new ArrayList<Channel>(Arrays.asList(channelArray)));
                                    Collections.sort(ChannelActivity.this.channels,
                                                     new CustomChannelComparator());
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            }
        }
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
                                            public void onError()
                                            {
                                                logger.d("Failed to join channel");
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
                                            public void onError()
                                            {
                                                logger.d("Failed to decline channel invite");
                                            }

                                            @Override
                                            public void onSuccess()
                                            {
                                                logger.d("Successfully to declined channel invite");
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

    private void setupListenersForChannel(Channel[] channelArray)
    {
        if (channelArray != null) {
            for (int i = 0; i < channelArray.length; i++) {
                if (channelArray[i] != null) {
                    channelArray[i].setListener(ChannelActivity.this);
                }
            }
        }
    }

    private void showToast(String text)
    {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        LinearLayout toastLayout = (LinearLayout)toast.getView();
        TextView     toastTV = (TextView)toastLayout.getChildAt(0);
        toastTV.setTextSize(30);
        toast.show();
    }

    // ChannelListener implementation

    @Override
    public void onTypingStarted(Member member)
    {
        if (member != null) {
            logger.d(member.getIdentity() + " started typing");
        }
    }

    @Override
    public void onTypingEnded(Member member)
    {
        if (member != null) {
            logger.d(member.getIdentity() + " ended typing");
        }
    }

    @Override
    public void onChannelHistoryLoaded(Channel channel)
    {
        logger.e("Received onChannelHistoryLoaded callback " + channel.getFriendlyName());
    }

    @Override
    public void onChannelAdd(Channel channel)
    {
        logger.d("Received onChannelAdd callback " + channel.getFriendlyName());
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                getChannels(null);
            }
        });
    }

    @Override
    public void onChannelChange(Channel channel)
    {
        logger.d("Received onChannelAdd callback " + channel.getFriendlyName());
    }

    @Override
    public void onChannelDelete(Channel channel)
    {
        logger.d("Received onChannelDelete callback " + channel.getFriendlyName());
    }

    @Override
    public void onError(int errorCode, String errorText)
    {
        logger.d("Received onError callback " + errorCode + " " + errorText);
    }

    @Override
    public void onAttributesChange(String attributes)
    {
        logger.d("Received onAttributesChange callback ");
    }

    @Override
    public void onMessageAdd(Message message)
    {
        if (message != null) {
            logger.d("Received onMessageAdd event");
        }
    }

    public void onMessageChange(Message message)
    {
        if (message != null) {
            logger.d("Received onMessageChange event");
        }
    }

    @Override
    public void onMessageDelete(Message message)
    {
        logger.d("Member deleted");
    }

    // Member Related Callbacks

    @Override
    public void onMemberJoin(Member member)
    {
        if (member != null) {
            logger.d(member.getIdentity() + " joined");
        }
    }

    @Override
    public void onMemberChange(Member member)
    {
        if (member != null) {
            logger.d(member.getIdentity() + " changed");
        }
    }

    @Override
    public void onMemberDelete(Member member)
    {
        if (member != null) {
            logger.d(member.getIdentity() + " deleted");
        }
    }

    @Override
    public void onAttributesChange(Map<String, String> updatedAttributes)
    {
        if (updatedAttributes != null) {
            logger.d("updatedAttributes event received");
        }
    }
}
