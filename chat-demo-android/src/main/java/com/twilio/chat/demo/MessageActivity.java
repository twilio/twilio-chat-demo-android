package com.twilio.chat.demo;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.twilio.chat.Channel;
import com.twilio.chat.Channel.ChannelType;
import com.twilio.chat.ChannelListener;
import com.twilio.chat.Channels;
import com.twilio.chat.StatusListener;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.Member;
import com.twilio.chat.Members;
import com.twilio.chat.Message;
import com.twilio.chat.Messages;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.Paginator;
import com.twilio.chat.User;
import com.twilio.chat.UserDescriptor;
import com.twilio.chat.Users;
import com.twilio.chat.internal.Logger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import uk.co.ribot.easyadapter.EasyAdapter;

public class MessageActivity extends Activity implements ChannelListener
{
    private static final Logger logger = Logger.getLogger(MessageActivity.class);
    private static final        String[] MESSAGE_OPTIONS = {
        "Remove", "Edit", "Get Attributes", "Edit Attributes"
    };
    private ListView                 messageListView;
    private EditText                 inputText;
    private EasyAdapter<MessageItem> adapter;
    private Channel                  channel;
    private static final             String[] EDIT_OPTIONS = { "Change Friendly Name",
                                                   "Change Topic",
                                                   "List Members",
                                                   "Invite Member",
                                                   "Add Member",
                                                   "Remove Member",
                                                   "Leave",
                                                   "Destroy",
                                                   "Get Attributes",
                                                   "Change Unique Name",
                                                   "Get Unique Name",
    "Get message index 0",
    "Set all consumed",
    "Set none consumed"};

    private static final int NAME_CHANGE = 0;
    private static final int TOPIC_CHANGE = 1;
    private static final int LIST_MEMBERS = 2;
    private static final int INVITE_MEMBER = 3;
    private static final int ADD_MEMBER = 4;
    private static final int REMOVE_MEMBER = 5;
    private static final int LEAVE = 6;
    private static final int CHANNEL_DESTROY = 7;
    private static final int CHANNEL_ATTRIBUTE = 8;
    private static final int SET_CHANNEL_UNIQUE_NAME = 9;
    private static final int GET_CHANNEL_UNIQUE_NAME = 10;
    private static final int GET_MESSAGE_BY_INDEX = 11;
    private static final int SET_ALL_CONSUMED = 12;
    private static final int SET_NONE_CONSUMED = 13;


    private static final int REMOVE = 0;
    private static final int EDIT = 1;
    private static final int GET_ATTRIBUTES = 2;
    private static final int SET_ATTRIBUTES = 3;

    private AlertDialog            editTextDialog;
    private AlertDialog            memberListDialog;
    private AlertDialog            changeChannelTypeDialog;
    private ArrayList<MessageItem> messageItemList = new ArrayList<>();
    private String                 identity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        createUI();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
            if (channel != null) {
                setupListView(channel);
            }
        }
    }

    private void createUI()
    {
        setContentView(R.layout.activity_message);
        if (getIntent() != null) {
            BasicChatClient basicClient = TwilioApplication.get().getBasicClient();
            identity = basicClient.getChatClient().getMyIdentity();
            String   channelSid = getIntent().getStringExtra(Constants.EXTRA_CHANNEL_SID);
            Channels channelsObject = basicClient.getChatClient().getChannels();
            channelsObject.getChannel(channelSid, new CallbackListener<Channel>() {
                @Override
                public void onSuccess(final Channel foundChannel)
                {
                    channel = foundChannel;
                    channel.addListener(MessageActivity.this);
                    MessageActivity.this.setTitle(
                        ((channel.getType() == ChannelType.PUBLIC) ? "PUB " : "PRIV ")
                        + channel.getFriendlyName());

                    setupListView(channel);

                    messageListView = (ListView)findViewById(R.id.message_list_view);
                    if (messageListView != null) {
                        messageListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                        messageListView.setStackFromBottom(true);
                        adapter.registerDataSetObserver(new DataSetObserver() {
                            @Override
                            public void onChanged()
                            {
                                super.onChanged();
                                messageListView.setSelection(adapter.getCount() - 1);
                            }
                        });
                    }
                    setupInput();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.action_settings: showChannelSettingsDialog(); break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showChannelSettingsDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setTitle("Select an option")
            .setItems(EDIT_OPTIONS, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which)
                {
                    if (which == NAME_CHANGE) {
                        showChangeNameDialog();
                    } else if (which == TOPIC_CHANGE) {
                        showChangeTopicDialog();
                    } else if (which == LIST_MEMBERS) {
                        Users users = TwilioApplication.get().getBasicClient().getChatClient().getUsers();
                        // Members.getMembersList() way
                        List<Member> members = channel.getMembers().getMembersList();
                        StringBuffer name = new StringBuffer();
                        for (int i = 0; i < members.size(); i++) {
                            name.append(members.get(i).getIdentity());
                            if (i + 1 < members.size()) {
                                name.append(", ");
                            }
                            members.get(i).getUserDescriptor(new CallbackListener<UserDescriptor>() {
                                @Override
                                public void onSuccess(UserDescriptor userDescriptor) {
                                    logger.d("Got user descriptor from member: "+userDescriptor.getIdentity());
                                }
                            });
                            members.get(i).getAndSubscribeUser(new CallbackListener<User>() {
                                @Override
                                public void onSuccess(User user) {
                                    logger.d("Got subscribed user from member: "+user.getIdentity());
                                }
                            });
                        }
                        TwilioApplication.get().showToast(name.toString(), Toast.LENGTH_LONG);
                        // Users.getSubscribedUsers() everybody we subscribed to at the moment
                        List<User> userList = users.getSubscribedUsers();
                        StringBuffer name2 = new StringBuffer();
                        for (int i = 0; i < userList.size(); i++) {
                            name2.append(userList.get(i).getIdentity());
                            if (i + 1 < userList.size()) {
                                name2.append(", ");
                            }
                        }
                        TwilioApplication.get().showToast("Subscribed users: "+name2.toString(), Toast.LENGTH_LONG);
                        // Get user descriptor via identity
                        users.getUserDescriptor(channel.getMembers().getMembersList().get(0).getIdentity(), new CallbackListener<UserDescriptor>() {
                            @Override
                            public void onSuccess(UserDescriptor userDescriptor) {
                                TwilioApplication.get().showToast("Random user descriptor: "+
                                        userDescriptor.getFriendlyName()+"/"+userDescriptor.getIdentity(), Toast.LENGTH_SHORT);
                            }
                        });

                        // Users.getChannelUserDescriptors() way - paginated
                        users.getChannelUserDescriptors(channel.getSid(),
                                new CallbackListener<Paginator<UserDescriptor>>() {
                                    @Override
                                    public void onSuccess(Paginator<UserDescriptor> userDescriptorPaginator) {
                                        getUsersPage(userDescriptorPaginator);
                                    }
                                });

                        // Channel.getMemberByIdentity() for finding the user in all channels
                        List<Member> members2 = TwilioApplication.get().getBasicClient().getChatClient().getChannels().getMembersByIdentity(channel.getMembers().getMembersList().get(0).getIdentity());
                        StringBuffer name3 = new StringBuffer();
                        for (int i = 0; i < members2.size(); i++) {
                            name3.append(members2.get(i).getIdentity()+" in "+members2.get(i).getChannel().getFriendlyName());
                            if (i + 1 < members2.size()) {
                                name3.append(", ");
                            }
                        }
                        //TwilioApplication.get().showToast("Random user in all channels: "+name3.toString(), Toast.LENGTH_LONG);
                    } else if (which == INVITE_MEMBER) {
                        showInviteMemberDialog();
                    } else if (which == ADD_MEMBER) {
                        showAddMemberDialog();
                    } else if (which == LEAVE) {
                        channel.leave(new ToastStatusListener(
                            "Successfully left channel", "Error leaving channel") {
                            @Override
                            public void onSuccess()
                            {
                                super.onSuccess();
                                finish();
                            }
                        });
                    } else if (which == REMOVE_MEMBER) {
                        showRemoveMemberDialog();
                    } else if (which == CHANNEL_DESTROY) {
                        channel.destroy(new ToastStatusListener(
                            "Successfully destroyed channel", "Error destroying channel") {
                            @Override
                            public void onSuccess()
                            {
                                super.onSuccess();
                                finish();
                            }
                        });
                    } else if (which == CHANNEL_ATTRIBUTE) {
                        try {
                            TwilioApplication.get().showToast(channel.getAttributes().toString());
                        } catch (JSONException e) {
                            TwilioApplication.get().showToast("JSON exception in channel attributes");
                        }
                    } else if (which == SET_CHANNEL_UNIQUE_NAME) {
                        showChangeUniqueNameDialog();
                    } else if (which == GET_CHANNEL_UNIQUE_NAME) {
                        TwilioApplication.get().showToast(channel.getUniqueName());
                    } else if (which == GET_MESSAGE_BY_INDEX) {
                        channel.getMessages().getMessageByIndex(channel.getMessages().getLastConsumedMessageIndex(), new CallbackListener<Message>() {
                                    @Override
                                    public void onSuccess(Message message) {
                                        TwilioApplication.get().showToast("SUCCESS GET MESSAGE BY IDX");
                                        logger.e("MESSAGES "+message.getMessages().toString());
                                        logger.e("MESSAGE CHANNEL "+message.getChannel().getSid());
                                    }
                                    @Override
                            public void onError(ErrorInfo info) {
                                        TwilioApplication.get().showError(info);
                                    }
                                });
                    } else if (which == SET_ALL_CONSUMED) {
                        channel.getMessages().setAllMessagesConsumed();
                    } else if (which == SET_NONE_CONSUMED) {
                        channel.getMessages().setNoMessagesConsumed();
                    }
                }
            });

        builder.show();
    }

    private void getUsersPage(Paginator<UserDescriptor> userDescriptorPaginator) {
        logger.e(userDescriptorPaginator.getItems().toString());
        for (UserDescriptor u : userDescriptorPaginator.getItems()) {
            u.subscribe(new CallbackListener<User>() {
                @Override
                public void onSuccess(User user) {
                    logger.d("Hi I am subscribed user now "+user.getIdentity());
                }
            });
        }
        if (userDescriptorPaginator.hasNextPage()) {
            userDescriptorPaginator.requestNextPage(new CallbackListener<Paginator<UserDescriptor>>() {
                @Override
                public void onSuccess(Paginator<UserDescriptor> userDescriptorPaginator) {
                    getUsersPage(userDescriptorPaginator);
                }
            });
        }
    }

    private void showChangeNameDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_friendly_name, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String friendlyName =
                            ((EditText)editTextDialog.findViewById(R.id.update_friendly_name))
                                .getText()
                                .toString();
                        logger.d(friendlyName);
                        channel.setFriendlyName(friendlyName, new ToastStatusListener(
                            "Successfully changed name", "Error changing name"));
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showChangeTopicDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_channel_topic, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String topic = ((EditText)editTextDialog.findViewById(R.id.update_topic))
                                           .getText()
                                           .toString();
                        logger.d(topic);
                        JSONObject attrObj = new JSONObject();
                        try {
                            attrObj.put("Topic", topic);
                        } catch (JSONException ignored) {
                            // whatever
                        }

                        channel.setAttributes(attrObj, new ToastStatusListener(
                            "Attributes were set successfullly.",
                            "Setting attributes failed"));
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showInviteMemberDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_invite_member, null))
            .setPositiveButton(
                "Invite",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String memberName =
                            ((EditText)editTextDialog.findViewById(R.id.invite_member))
                                .getText()
                                .toString();
                        logger.d(memberName);

                        Members membersObject = channel.getMembers();
                        membersObject.inviteByIdentity(memberName, new ToastStatusListener(
                            "Invited user to channel",
                            "Error in inviteByIdentity"));
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showAddMemberDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_add_member, null))
            .setPositiveButton(
                "Add",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String memberName = ((EditText)editTextDialog.findViewById(R.id.add_member))
                                                .getText()
                                                .toString();
                        logger.d(memberName);

                        Members membersObject = channel.getMembers();
                        membersObject.addByIdentity(memberName, new ToastStatusListener(
                            "Successful addByIdentity",
                            "Error adding member"));
                    }
                })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showRemoveMemberDialog()
    {
        final Members membersObject = channel.getMembers();
        List<Member> members = membersObject.getMembersList();
        final View convertView = getLayoutInflater().inflate(R.layout.member_list, null);
        final AlertDialog memberListDialog = new AlertDialog.Builder(MessageActivity.this)
                .setView(convertView)
                .setTitle("Remove members")
                .create();
        ListView            lv = (ListView)convertView.findViewById(R.id.listView1);
        EasyAdapter<Member> adapterMember = new EasyAdapter<Member>(
                MessageActivity.this, MemberViewHolder.class, members, new MemberViewHolder.OnMemberClickListener() {
            @Override
            public void onMemberClicked(Member member)
            {
                membersObject.remove(member, new ToastStatusListener(
                        "Successful removeMember operation",
                        "Error in removeMember operation"));
                memberListDialog.dismiss();
            }
        });
        lv.setAdapter(adapterMember);
        memberListDialog.show();
        memberListDialog.getWindow().setLayout(800, 600);
    }

    private void loadAndShowMessages()
    {
        final Messages messagesObject = channel.getMessages();
        if (messagesObject != null) {
            messagesObject.getLastMessages(50, new CallbackListener<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messagesArray) {
                    messageItemList.clear();
                    Members  members = channel.getMembers();
                    if (messagesArray.size() > 0) {
                        for (int i = 0; i < messagesArray.size(); i++) {
                            messageItemList.add(new MessageItem(messagesArray.get(i), members, identity));
                        }
                    }
                    adapter.getItems().clear();
                    adapter.getItems().addAll(messageItemList);
                    adapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void showUpdateMessageDialog(final Message message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_message, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String updatedMsg =
                            ((EditText)editTextDialog.findViewById(R.id.update_message))
                                .getText()
                                .toString();
                        message.updateMessageBody(updatedMsg, new ToastStatusListener(
                            "Success updating message",
                            "Error updating message") {
                            @Override
                            public void onSuccess()
                            {
                                super.onSuccess();
                                loadAndShowMessages();// @todo only need to update one message body
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
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    private void showUpdateMessageAttributesDialog(final Message message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_message_attributes, null))
            .setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String updatedAttr =
                            ((EditText)editTextDialog.findViewById(R.id.update_attributes))
                                .getText()
                                .toString();
                        JSONObject jsonObj = null;
                        try {
                            jsonObj = new JSONObject(updatedAttr);
                        } catch (JSONException e) {
                            logger.e("Invalid JSON attributes entered, using old value");
                            try {
                                jsonObj = message.getAttributes();
                            } catch (JSONException ex) {
                                jsonObj = null;
                            }
                        }

                        message.setAttributes(jsonObj, new ToastStatusListener(
                            "Success updating message attributes",
                            "Error updating message attributes") {
                            @Override
                            public void onSuccess()
                            {
                                super.onSuccess();
                                loadAndShowMessages();// @todo only need to update one message
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
        editTextDialog = builder.create();

        String attr = "";
        try {
            attr = message.getAttributes().toString();
        } catch (JSONException e) {
        }
        editTextDialog.create(); // Force creation of sub-view hierarchy
        ((EditText)editTextDialog.findViewById(R.id.update_attributes))
            .setText(attr);

        editTextDialog.show();
    }

    private void setupInput()
    {
        // Setup our input methods. Enter key on the keyboard or pushing the send button
        EditText inputText = (EditText)findViewById(R.id.messageInput);
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
            }
            @Override
            public void afterTextChanged(Editable s)
            {
                if (channel != null) {
                    channel.typing();
                }
            }
        });

        inputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (actionId == EditorInfo.IME_NULL
                    && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendMessage();
                }
                return true;
            }
        });

        findViewById(R.id.sendButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                sendMessage();
            }
        });
    }

    private class CustomMessageComparator implements Comparator<Message>
    {
        @Override
        public int compare(Message lhs, Message rhs)
        {
            if (lhs == null) {
                return (rhs == null) ? 0 : -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
        }
    }

    private void setupListView(Channel channel)
    {
        messageListView = (ListView)findViewById(R.id.message_list_view);
        final Messages messagesObject = channel.getMessages();

        messageListView.getViewTreeObserver().addOnScrollChangedListener(
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged()
                {
                    if ((messageListView.getLastVisiblePosition() >= 0)
                        && (messageListView.getLastVisiblePosition() < adapter.getCount())) {
                        MessageItem item =
                            adapter.getItem(messageListView.getLastVisiblePosition());
                        if (item != null && messagesObject != null)
                            messagesObject.advanceLastConsumedMessageIndex(
                                item.getMessage().getMessageIndex());
                    }
                }
            });

        adapter = new EasyAdapter<MessageItem>(
                MessageActivity.this,
                MessageViewHolder.class,
                new MessageViewHolder.OnMessageClickListener() {
                    @Override
                    public void onMessageClicked(final MessageItem message)
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
                        builder.setTitle("Select an option")
                                .setItems(MESSAGE_OPTIONS, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        if (which == REMOVE) {
                                            dialog.cancel();
                                            messagesObject.removeMessage(
                                                    message.getMessage(), new ToastStatusListener(
                                                        "Successfully removed message. It should be GONE!!",
                                                        "Error removing message") {
                                                        @Override
                                                        public void onSuccess()
                                                        {
                                                            super.onSuccess();
                                                            messageItemList.remove(message);
                                                            adapter.notifyDataSetChanged();
                                                        }
                                                    });
                                        } else if (which == EDIT) {
                                            showUpdateMessageDialog(message.getMessage());
                                        } else if (which == GET_ATTRIBUTES) {
                                            String attr = "";
                                            try {
                                                attr = message.getMessage().getAttributes().toString();
                                            } catch (JSONException e) {
                                            }
                                            TwilioApplication.get().showToast(attr);
                                        } else if (which == SET_ATTRIBUTES) {
                                            showUpdateMessageAttributesDialog(message.getMessage());
                                        }
                                    }
                                });
                        builder.show();
                    }
                });
        messageListView.setAdapter(adapter);

        loadAndShowMessages();
    }

    private void sendMessage()
    {
        inputText = (EditText)findViewById(R.id.messageInput);
        String input = inputText.getText().toString();
        if (!input.equals("")) {
            final Messages messagesObject = this.channel.getMessages();

            messagesObject.sendMessage(input, new ToastStatusListener(
                "Successfully sent message",
                "Error sending message") {
                @Override
                public void onSuccess()
                {
                    super.onSuccess();
                    adapter.notifyDataSetChanged();
                    inputText.setText("");
                }
            });
        }

        inputText.requestFocus();
    }

    @Override
    public void onMessageAdded(Message message)
    {
        setupListView(this.channel);
    }

    @Override
    public void onMessageUpdated(Message message)
    {
        if (message != null) {
            TwilioApplication.get().showToast(message.getSid() + " changed");
            logger.d("Received onMessageChange for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageChange");
        }
    }

    @Override
    public void onMessageDeleted(Message message)
    {
        if (message != null) {
            TwilioApplication.get().showToast(message.getSid() + " deleted");
            logger.d("Received onMessageDelete for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageDelete.");
        }
    }

    @Override
    public void onMemberAdded(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getIdentity() + " joined");
        }
    }

    @Override
    public void onMemberUpdated(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getIdentity() + " changed");
        }
    }

    @Override
    public void onMemberDeleted(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getIdentity() + " deleted");
        }
    }

    @Override
    public void onTypingStarted(Member member)
    {
        if (member != null) {
            TextView typingIndc = (TextView)findViewById(R.id.typingIndicator);
            String   text = member.getIdentity() + " is typing .....";
            typingIndc.setText(text);
            typingIndc.setTextColor(Color.RED);
            logger.d(text);
        }
    }

    @Override
    public void onTypingEnded(Member member)
    {
        if (member != null) {
            TextView typingIndc = (TextView)findViewById(R.id.typingIndicator);
            typingIndc.setText(null);
            logger.d(member.getIdentity() + " ended typing");
        }
    }

    private void showChangeUniqueNameDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
        builder.setView(getLayoutInflater().inflate(R.layout.dialog_edit_unique_name, null))
            .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id)
                {
                    String uniqueName =
                        ((EditText)editTextDialog.findViewById(R.id.update_unique_name))
                            .getText()
                            .toString();
                    logger.d(uniqueName);
                    channel.setUniqueName(uniqueName, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            TwilioApplication.get().showError(errorInfo);
                            TwilioApplication.get().logErrorInfo(
                                "Error changing channel uniqueName", errorInfo);
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successfully changed channel uniqueName");
                        }
                    });
                }
            });
        editTextDialog = builder.create();
        editTextDialog.show();
    }

    @Override
    public void onSynchronizationChanged(Channel channel)
    {
        logger.d("Received onSynchronizationChanged callback " + channel.getFriendlyName());
    }

    public static class MessageItem
    {
        Message message;
        Members members;
        String  currentUser;

        public MessageItem(Message message, Members members, String currentUser)
        {
            this.message = message;
            this.members = members;
            this.currentUser = currentUser;
        }

        public Message getMessage()
        {
            return message;
        }

        public Members getMembers()
        {
            return members;
        }
    }
}
