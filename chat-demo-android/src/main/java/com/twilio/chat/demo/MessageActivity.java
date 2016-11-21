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
    private List<Message>            messages = new ArrayList<Message>();
    private List<Member>             members = new ArrayList<Member>();
    private Channel                  channel;
    private static final             String[] EDIT_OPTIONS = { "Change Friendly Name",
                                                   "Change Topic",
                                                   "List Members",
                                                   "Invite Member",
                                                   "Add Member",
                                                   "Remove Member",
                                                   "Leave",
                                                   "Change ChannelType",
                                                   "Destroy",
                                                   "Get Attributes",
                                                   "Change Unique Name",
                                                   "Get Unique Name" };

    private static final int NAME_CHANGE = 0;
    private static final int TOPIC_CHANGE = 1;
    private static final int LIST_MEMBERS = 2;
    private static final int INVITE_MEMBER = 3;
    private static final int ADD_MEMBER = 4;
    private static final int REMOVE_MEMBER = 5;
    private static final int LEAVE = 6;
    private static final int CHANNEL_TYPE = 7;
    private static final int CHANNEL_DESTROY = 8;
    private static final int CHANNEL_ATTRIBUTE = 9;
    private static final int SET_CHANNEL_UNIQUE_NAME = 10;
    private static final int GET_CHANNEL_UNIQUE_NAME = 11;

    private static final int REMOVE = 0;
    private static final int EDIT = 1;
    private static final int GET_ATTRIBUTES = 2;
    private static final int SET_ATTRIBUTES = 3;

    private AlertDialog            editTextDialog;
    private AlertDialog            memberListDialog;
    private AlertDialog            changeChannelTypeDialog;
    private StatusListener         messageListener;
    private StatusListener         leaveListener;
    private StatusListener         destroyListener;
    private StatusListener         nameUpdateListener;
    private ArrayList<MessageItem> messageItemList;
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
            Channel channel = intent.getParcelableExtra(Constants.EXTRA_CHANNEL);
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
            identity = basicClient.getChatClient().getMyUserInfo().getIdentity();
            String   channelSid = getIntent().getStringExtra(Constants.EXTRA_CHANNEL_SID);
            Channels channelsObject = basicClient.getChatClient().getChannels();
            if (channelsObject != null) {
                channel = channelsObject.getChannel(channelSid);
                if (channel != null) {
                    channel.addListener(MessageActivity.this);
                    this.setTitle(
                        "Name:" + channel.getFriendlyName() + " Type:"
                        + ((channel.getType() == ChannelType.PUBLIC) ? "Public" : "Private"));
                }
            }
        }

        channel.synchronize(new CallbackListener<Channel>() {
            @Override
            public void onError(ErrorInfo errorInfo)
            {
                TwilioApplication.get().logErrorInfo("Channel sync failed", errorInfo);
            }

            @Override
            public void onSuccess(Channel result)
            {
                logger.d("Channel sync success for " + result.getFriendlyName());
            }
        });

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
                        Members membersObject = channel.getMembers();
                        Member[] members = membersObject.getMembers();

                        logger.d("members retrieved");
                        StringBuffer name = new StringBuffer();
                        for (int i = 0; i < members.length; i++) {
                            name.append(members[i].getUserInfo().getIdentity());
                            if (i + 1 < members.length) {
                                name.append(", ");
                            }
                        }
                        TwilioApplication.get().showToast(name.toString());
                    } else if (which == INVITE_MEMBER) {
                        showInviteMemberDialog();
                    } else if (which == ADD_MEMBER) {
                        showAddMemberDialog();
                    } else if (which == LEAVE) {
                        leaveListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().logErrorInfo("Error leaving channel",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at leaving channel");
                                finish();
                            }
                        };
                        channel.leave(leaveListener);

                    } else if (which == REMOVE_MEMBER) {
                        showRemoveMemberDialog();
                    } else if (which == CHANNEL_TYPE) {
                        showChangeChannelType();
                    } else if (which == CHANNEL_DESTROY) {
                        destroyListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().logErrorInfo("Error destroying channel",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at destroying channel");
                                finish();
                            }
                        };
                        channel.destroy(destroyListener);
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
                    }
                }
            });

        builder.show();
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
                        nameUpdateListener = new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().showError(errorInfo);
                                TwilioApplication.get().logErrorInfo("Error changing name",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("successfully changed name");
                            }
                        };
                        channel.setFriendlyName(friendlyName, nameUpdateListener);
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

                        channel.setAttributes(attrObj, new StatusListener() {
                            @Override
                            public void onSuccess()
                            {
                                logger.d("Attributes were set successfullly.");
                            }

                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().showError(errorInfo);
                                TwilioApplication.get().logErrorInfo("Setting attributes failed",
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
                        membersObject.inviteByIdentity(memberName, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().showError(errorInfo);
                                TwilioApplication.get().logErrorInfo("Error in inviteByIdentity",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at inviteByIdentity.");
                                TwilioApplication.get().showToast("Invited user to channel");
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
                        membersObject.addByIdentity(memberName, new StatusListener() {
                            @Override
                            public void onError(ErrorInfo errorInfo)
                            {
                                TwilioApplication.get().showError(errorInfo);
                                TwilioApplication.get().logErrorInfo("Error adding member",
                                                                     errorInfo);
                            }

                            @Override
                            public void onSuccess()
                            {
                                logger.d("Successful at addByIdentity");
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

    private void showRemoveMemberDialog()
    {
        final Members membersObject = channel.getMembers();
        Member[] membersArray = membersObject.getMembers();
        if (membersArray.length > 0) {
            members = new ArrayList<Member>(Arrays.asList(membersArray));
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MessageActivity.this);
        View convertView = (View)getLayoutInflater().inflate(R.layout.member_list, null);
        alertDialog.setView(convertView);
        alertDialog.setTitle("Remove members");
        ListView            lv = (ListView)convertView.findViewById(R.id.listView1);
        EasyAdapter<Member> adapterMember = new EasyAdapter<Member>(
            this, MemberViewHolder.class, members, new MemberViewHolder.OnMemberClickListener() {
                @Override
                public void onMemberClicked(Member member)
                {
                    membersObject.removeMember(member, new StatusListener() {
                        @Override
                        public void onError(ErrorInfo errorInfo)
                        {
                            TwilioApplication.get().showError(errorInfo);
                            TwilioApplication.get().logErrorInfo("Error in removeMember operation",
                                                                 errorInfo);
                        }

                        @Override
                        public void onSuccess()
                        {
                            logger.d("Successful removeMember operation");
                        }
                    });
                    memberListDialog.dismiss();
                }
            });
        lv.setAdapter(adapterMember);
        memberListDialog = alertDialog.create();
        memberListDialog.show();
        memberListDialog.getWindow().setLayout(800, 600);
    }

    private void showChangeChannelType()
    {
    }

    private void loadAndShowMessages()
    {
        final Messages messagesObject = channel.getMessages();
        final List<MessageItem> items = new ArrayList<MessageItem>();
        final Members  members = channel.getMembers();
        if (messagesObject != null) {
            messagesObject.getLastMessages(100, new CallbackListener<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messagesArray) {
                    if (messagesArray.size() > 0) {
                        messages = new ArrayList<Message>(messagesArray);
                        Collections.sort(messages,
                                new CustomMessageComparator());
                    }
                    for (int i = 0; i < messagesArray.size(); i++) {
                        items.add(new MessageItem(
                                messages.get(i), members, identity));
                    }
                }
            });
        }

        adapter.getItems().clear();
        adapter.getItems().addAll(items);
        adapter.notifyDataSetChanged();
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
        adapter.notifyDataSetChanged();

        if (messagesObject != null) {
            final Members members = channel.getMembers();
            messagesObject.getLastMessages(500, new CallbackListener<List<Message>>() {
                @Override
                public void onSuccess(List<Message> messagesArray) {
                    if (messagesArray.size() > 0) {
                        messages = new ArrayList<>(messagesArray);
                        Collections.sort(messages, new CustomMessageComparator());
                    }
                    MessageItem[] items = new MessageItem[messagesArray.size()];
                    for (int i = 0; i < items.length; i++) {
                        items[i] = new MessageItem(messages.get(i), members, identity);
                    }
                    messageItemList = new ArrayList<>(Arrays.asList(items));

                    adapter.setItems(messageItemList);
                }
            });
        }
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
    public void onMessageAdd(Message message)
    {
        setupListView(this.channel);
    }

    @Override
    public void onMessageChange(Message message)
    {
        if (message != null) {
            TwilioApplication.get().showToast(message.getSid() + " changed");
            logger.d("Received onMessageChange for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageChange");
        }
    }

    @Override
    public void onMessageDelete(Message message)
    {
        if (message != null) {
            TwilioApplication.get().showToast(message.getSid() + " deleted");
            logger.d("Received onMessageDelete for message sid|" + message.getSid() + "|");
        } else {
            logger.d("Received onMessageDelete.");
        }
    }

    @Override
    public void onMemberJoin(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getUserInfo().getIdentity() + " joined");
        }
    }

    @Override
    public void onMemberChange(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getUserInfo().getIdentity() + " changed");
        }
    }

    @Override
    public void onMemberDelete(Member member)
    {
        if (member != null) {
            TwilioApplication.get().showToast(member.getUserInfo().getIdentity() + " deleted");
        }
    }

    @Override
    public void onTypingStarted(Member member)
    {
        if (member != null) {
            TextView typingIndc = (TextView)findViewById(R.id.typingIndicator);
            String   text = member.getUserInfo().getIdentity() + " is typing .....";
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
            logger.d(member.getUserInfo().getIdentity() + " ended typing");
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
    public void onSynchronizationChange(Channel channel)
    {
        logger.d("Received onSynchronizationChange callback " + channel.getFriendlyName());
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

        public String getCurrentUser()
        {
            return currentUser;
        }
    }
}
