package com.twilio.chat.demo.activities

import java.util.ArrayList
import java.util.Comparator

import com.twilio.chat.Channel
import com.twilio.chat.Channel.ChannelType
import com.twilio.chat.ChannelListener
import com.twilio.chat.StatusListener
import com.twilio.chat.CallbackListener
import com.twilio.chat.Member
import com.twilio.chat.Members
import com.twilio.chat.Message
import com.twilio.chat.ErrorInfo
import com.twilio.chat.Paginator
import com.twilio.chat.User
import com.twilio.chat.UserDescriptor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.twilio.chat.demo.Constants
import com.twilio.chat.demo.R
import com.twilio.chat.demo.TwilioApplication
import com.twilio.chat.demo.services.MediaService
import com.twilio.chat.demo.views.MemberViewHolder
import com.twilio.chat.demo.views.MessageViewHolder
import eu.inloop.simplerecycleradapter.ItemClickListener
import eu.inloop.simplerecycleradapter.ItemLongClickListener
import eu.inloop.simplerecycleradapter.SettableViewHolder
import eu.inloop.simplerecycleradapter.SimpleRecyclerAdapter

import org.json.JSONException
import org.json.JSONObject

import timber.log.Timber
import kotlinx.android.synthetic.main.activity_message.*
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk25.coroutines.onClick
import ChatStatusListener
import ChatCallbackListener
import ToastStatusListener

// RecyclerView Anko
inline fun ViewManager.recyclerView() = recyclerView(theme = 0) {}

inline fun ViewManager.recyclerView(init: RecyclerView.() -> Unit): RecyclerView {
    return ankoView({ RecyclerView(it) }, theme = 0, init = init)
}

inline fun ViewManager.recyclerView(theme: Int = 0) = recyclerView(theme) {}

inline fun ViewManager.recyclerView(theme: Int = 0, init: RecyclerView.() -> Unit): RecyclerView {
    return ankoView({ RecyclerView(it) }, theme, init)
}
// End RecyclerView Anko

class MessageActivity : Activity(), ChannelListener {
    private lateinit var adapter: SimpleRecyclerAdapter<MessageItem>
    private var channel: Channel? = null

    private val messageItemList = ArrayList<MessageItem>()
    private lateinit var identity: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        createUI()
    }

    override fun onDestroy() {
        channel?.removeListener(this@MessageActivity);
        super.onDestroy();
    }

    override fun onResume() {
        super.onResume()
        if (intent != null) {
            channel = intent.getParcelableExtra<Channel>(Constants.EXTRA_CHANNEL)
            if (channel != null) {
                setupListView(channel!!)
            }
        }
    }

    private fun createUI() {
        if (intent != null) {
            val basicClient = TwilioApplication.instance.basicClient
            identity = basicClient.chatClient!!.myIdentity
            val channelSid = intent.getStringExtra(Constants.EXTRA_CHANNEL_SID)
            val channelsObject = basicClient.chatClient!!.channels
            channelsObject.getChannel(channelSid, ChatCallbackListener<Channel>() {
                channel = it
                channel!!.addListener(this@MessageActivity)
                this@MessageActivity.title = (if (channel!!.type == ChannelType.PUBLIC) "PUB " else "PRIV ") + channel!!.friendlyName

                setupListView(channel!!)

//                    message_list_view.transcriptMode = ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
//                    message_list_view.isStackFromBottom = true
//                    adapter.registerDataSetObserver(object : DataSetObserver() {
//                        override fun onChanged() {
//                            super.onChanged()
//                            message_list_view.setSelection(adapter.count - 1)
//                        }
//                    })
                setupInput()
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.message, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> showChannelSettingsDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showChannelSettingsDialog() {
        val builder = AlertDialog.Builder(this@MessageActivity)
        builder.setTitle("Select an option")
            .setItems(EDIT_OPTIONS) { _, which ->
                if (which == NAME_CHANGE) {
                    showChangeNameDialog()
                } else if (which == TOPIC_CHANGE) {
                    showChangeTopicDialog()
                } else if (which == LIST_MEMBERS) {
                    val users = TwilioApplication.instance.basicClient.chatClient!!.users
                    // Members.getMembersList() way
                    val members = channel!!.members.membersList
                    val name = StringBuffer()
                    for (i in members.indices) {
                        name.append(members[i].identity)
                        if (i + 1 < members.size) {
                            name.append(", ")
                        }
                        members[i].getUserDescriptor(ChatCallbackListener<UserDescriptor>() {
                            Timber.d("Got user descriptor from member: ${it.identity}")
                        })
                        members[i].getAndSubscribeUser(ChatCallbackListener<User>() {
                            Timber.d("Got subscribed user from member: ${it.identity}")
                        })
                    }
                    TwilioApplication.instance.showToast(name.toString(), Toast.LENGTH_LONG)
                    // Users.getSubscribedUsers() everybody we subscribed to at the moment
                    val userList = users.subscribedUsers
                    val name2 = StringBuffer()
                    for (i in userList.indices) {
                        name2.append(userList[i].identity)
                        if (i + 1 < userList.size) {
                            name2.append(", ")
                        }
                    }
                    TwilioApplication.instance.showToast("Subscribed users: ${name2.toString()}", Toast.LENGTH_LONG)

                    // Get user descriptor via identity
                    users.getUserDescriptor(channel!!.members.membersList[0].identity, ChatCallbackListener<UserDescriptor>() {
                        TwilioApplication.instance.showToast("Random user descriptor: ${it.friendlyName}/${it.identity}", Toast.LENGTH_SHORT)
                    })

                    // Users.getChannelUserDescriptors() way - paginated
                    users.getChannelUserDescriptors(channel!!.sid,
                        object : CallbackListener<Paginator<UserDescriptor>>() {
                            override fun onSuccess(userDescriptorPaginator: Paginator<UserDescriptor>) {
                                getUsersPage(userDescriptorPaginator)
                            }
                        })

                    // Channel.getMemberByIdentity() for finding the user in all channels
                    val members2 = TwilioApplication.instance.basicClient.chatClient!!.channels.getMembersByIdentity(channel!!.members.membersList[0].identity)
                    val name3 = StringBuffer()
                    for (i in members2.indices) {
                        name3.append(members2[i].identity + " in " + members2[i].channel.friendlyName)
                        if (i + 1 < members2.size) {
                            name3.append(", ")
                        }
                    }
                    //TwilioApplication.get().showToast("Random user in all channels: "+name3.toString(), Toast.LENGTH_LONG);
                } else if (which == INVITE_MEMBER) {
                    showInviteMemberDialog()
                } else if (which == ADD_MEMBER) {
                    showAddMemberDialog()
                } else if (which == LEAVE) {
                    channel!!.leave(ToastStatusListener(
                            "Successfully left channel", "Error leaving channel") {
                        finish()
                    })
                } else if (which == REMOVE_MEMBER) {
                    showRemoveMemberDialog()
                } else if (which == CHANNEL_DESTROY) {
                    channel!!.destroy(ToastStatusListener(
                            "Successfully destroyed channel", "Error destroying channel") {
                        finish()
                    })
                } else if (which == CHANNEL_ATTRIBUTE) {
                    try {
                        TwilioApplication.instance.showToast(channel!!.attributes.toString())
                    } catch (e: JSONException) {
                        TwilioApplication.instance.showToast("JSON exception in channel attributes")
                    }

                } else if (which == SET_CHANNEL_UNIQUE_NAME) {
                    showChangeUniqueNameDialog()
                } else if (which == GET_CHANNEL_UNIQUE_NAME) {
                    TwilioApplication.instance.showToast(channel!!.uniqueName)
                } else if (which == GET_MESSAGE_BY_INDEX) {
                    channel!!.messages.getMessageByIndex(channel!!.messages.lastConsumedMessageIndex!!, ChatCallbackListener<Message>() {
                        TwilioApplication.instance.showToast("SUCCESS GET MESSAGE BY IDX")
                        Timber.e("MESSAGES " + it.messages.toString())
                        Timber.e("MESSAGE CHANNEL " + it.channel.sid)
                    })
                } else if (which == SET_ALL_CONSUMED) {
                    channel!!.messages.setAllMessagesConsumed()
                } else if (which == SET_NONE_CONSUMED) {
                    channel!!.messages.setNoMessagesConsumed()
                }
            }

        builder.show()
    }

    private fun getUsersPage(userDescriptorPaginator: Paginator<UserDescriptor>) {
        for (u in userDescriptorPaginator.items) {
            u.subscribe(ChatCallbackListener<User>() {
                Timber.d("${it.identity} is a subscribed user now")
            })
        }
        if (userDescriptorPaginator.hasNextPage()) {
            userDescriptorPaginator.requestNextPage(ChatCallbackListener<Paginator<UserDescriptor>>() {
                getUsersPage(it)
            })
        }
    }

    private fun showChangeNameDialog() {
        alert(R.string.title_update_friendly_name) {
            customView {
                val friendly_name = editText { text.append(channel!!.friendlyName) }
                positiveButton(R.string.update) {
                    val friendlyName = friendly_name.text.toString()
                    Timber.d(friendlyName)
                    channel!!.setFriendlyName(friendlyName, ToastStatusListener(
                            "Successfully changed name", "Error changing name"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showChangeTopicDialog() {
        alert(R.string.title_update_topic) {
            customView {
                val topic = editText { text.append(channel!!.attributes.toString()) }
                positiveButton(R.string.change_topic) {
                    val topicText = topic.text.toString()
                    Timber.d(topicText)

                    val attrObj = JSONObject()
                    try {
                        attrObj.put("Topic", topicText)
                    } catch (ignored: JSONException) {
                        // whatever
                    }

                    channel!!.setAttributes(attrObj, ToastStatusListener(
                            "Attributes were set successfullly.",
                            "Setting attributes failed"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showInviteMemberDialog() {
        alert(R.string.title_invite_member) {
            customView {
                val member = editText { hint = "Enter user id" }
                positiveButton(R.string.invite_member) {
                    val memberName = member.text.toString()
                    Timber.d(memberName)
                    channel!!.members.inviteByIdentity(memberName, ToastStatusListener(
                            "Invited user to channel",
                            "Error in inviteByIdentity"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showAddMemberDialog() {
        alert(R.string.title_add_member) {
            customView {
                val member = editText { hint = "Enter user id" }
                positiveButton(R.string.invite_member) {
                    val memberName = member.text.toString()
                    Timber.d(memberName)
                    channel!!.members.addByIdentity(memberName, ToastStatusListener(
                            "Successful addByIdentity",
                            "Error adding member"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showRemoveMemberDialog() {
        val builder = AlertDialog.Builder(this@MessageActivity)
        builder.setView(
            verticalLayout {
                textView { textResource = R.string.title_remove_member }
                val view = recyclerView {}.lparams(width = dip(250), height = matchParent)
            }.view()
        )

//        view.adapter = SimpleRecyclerAdapter<Member>()
//
//
//        val membersObject = channel!!.members
//        val members = membersObject.membersList
//        val convertView = layoutInflater.inflate(R.layout.member_list, null)
//        val memberListDialog = AlertDialog.Builder(this@MessageActivity)
//                .setView(convertView)
//                .setTitle("Remove members")
//                .create()
//        val lv = convertView.findViewById(R.id.listView1) as ListView
//        val adapterMember = SimpleRecyclerAdapter<Member>(
//                this@MessageActivity, MemberViewHolder::class.java, members, object : MemberViewHolder.OnMemberClickListener {
//                    override fun onMemberClicked(member: Member) {
//                        membersObject.remove(member, ToastStatusListener(
//                                "Successful removeMember operation",
//                                "Error in removeMember operation"))
//                        memberListDialog.dismiss()
//                    }
//                })
//        lv.adapter = adapterMember
//        memberListDialog.show()
//        memberListDialog.window!!.setLayout(800, 600)
    }

    private fun showUpdateMessageDialog(message: Message) {
        alert(R.string.title_update_message) {
            customView {
                val messageText = editText { text.append(message.messageBody) }
                positiveButton(R.string.update) {
                    val text = messageText.text.toString()
                    Timber.d(text)
                    message.updateMessageBody(text, ToastStatusListener(
                            "Success updating message",
                            "Error updating message") {
                        // @todo only need to update one message body
                        loadAndShowMessages()
                    })
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showUpdateMessageAttributesDialog(message: Message) {
        alert(R.string.title_update_attributes) {
            customView {
                val messageAttrText = editText { text.append(message.attributes.toString()) }
                positiveButton(R.string.update) {
                    val text = messageAttrText.text.toString()
                    Timber.d(text)
                    try {
                        JSONObject(text).apply {
                            message.setAttributes(this, ToastStatusListener(
                                    "Success updating message attributes",
                                    "Error updating message attributes") {
                                // @todo only need to update one message
                                loadAndShowMessages()
                            })
                        }
                    } catch (e: JSONException) {
                        Timber.e("Invalid JSON attributes entered, using old value")
                    }
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showChangeUniqueNameDialog() {
        alert("Update channel unique name") {
            customView {
                val uniqueNameText = editText { text.append(channel!!.uniqueName) }
                positiveButton(R.string.update) {
                    val uniqueName = uniqueNameText.text.toString()
                    Timber.d(uniqueName)
                    channel!!.setUniqueName(uniqueName, ChatStatusListener());
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun loadAndShowMessages() {
        val messagesObject = channel!!.messages
        messagesObject?.getLastMessages(50, ChatCallbackListener<List<Message>>() {
            messageItemList.clear()
            val members = channel!!.members
            if (it.isNotEmpty()) {
                for (i in it.indices) {
                    messageItemList.add(MessageItem(it[i], members, identity))
                }
            }
            adapter.clear()
            adapter.addItems(messageItemList)
            adapter.notifyDataSetChanged()
        })
    }

    private fun setupInput() {
        // Setup our input methods. Enter key on the keyboard or pushing the send button
        val inputText = find<EditText>(R.id.messageInput)
        inputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                if (channel != null) {
                    channel!!.typing()
                }
            }
        })

        inputText.setOnEditorActionListener { _, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN) {
                sendMessage()
            }
            true
        }

        find<ImageButton>(R.id.sendButton).apply {
            setOnClickListener { sendMessage() }

            setOnLongClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                this@MessageActivity.startActivityForResult(intent, FILE_REQUEST)
                true
            }
        }
    }

    private inner class CustomMessageComparator : Comparator<Message> {
        override fun compare(lhs: Message?, rhs: Message?): Int {
            if (lhs == null) {
                return if (rhs == null) 0 else -1
            }
            if (rhs == null) {
                return 1
            }
            return lhs.timeStamp.compareTo(rhs.timeStamp)
        }
    }

    private fun setupListView(channel: Channel) {
        val messagesObject = channel.messages

//        message_list_view.viewTreeObserver.addOnScrollChangedListener {
//            if (message_list_view.lastVisiblePosition >= 0 && message_list_view.lastVisiblePosition < adapter!!.count) {
//                val item = adapter.getItem(message_list_view.lastVisiblePosition)
//                if (item != null && messagesObject != null)
//                    messagesObject.advanceLastConsumedMessageIndex(
//                            item.message.messageIndex)
//            }
//        }

        adapter = SimpleRecyclerAdapter<MessageItem>(
            ItemClickListener { _: MessageItem, viewHolder, _ ->
                (viewHolder as MessageViewHolder).toggleDateVisibility()
            },
            object : SimpleRecyclerAdapter.CreateViewHolder<MessageItem>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettableViewHolder<MessageItem> {
                    return MessageViewHolder(this@MessageActivity, parent);
                }
            })
        adapter.setLongClickListener(
                ItemLongClickListener { message: MessageItem, _, _ ->
                    val builder = AlertDialog.Builder(this@MessageActivity)
                    builder.setTitle(R.string.select_action)
                            .setItems(MESSAGE_OPTIONS) { dialog, which ->
                                if (which == REMOVE) {
                                    dialog.cancel()
                                    messagesObject!!.removeMessage(
                                            message.message, ToastStatusListener(
                                            "Successfully removed message. It should be GONE!!",
                                            "Error removing message") {
                                        messageItemList.remove(message)
                                        adapter.notifyDataSetChanged()
                                    })
                                } else if (which == EDIT) {
                                    showUpdateMessageDialog(message.message)
                                } else if (which == GET_ATTRIBUTES) {
                                    var attr = ""
                                    try {
                                        attr = message.message.attributes.toString()
                                    } catch (e: JSONException) {
                                    }

                                    TwilioApplication.instance.showToast(attr)
                                } else if (which == SET_ATTRIBUTES) {
                                    showUpdateMessageAttributesDialog(message.message)
                                }
                            }
                    builder.show()
                    true
                }
        )

        message_list_view.adapter = adapter
        message_list_view.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        loadAndShowMessages()
    }

    private fun sendMessage(text: String) {
        channel!!.messages.sendMessage(Message.options().withBody(text), ChatCallbackListener<Message>() {
            TwilioApplication.instance.showToast("Successfully sent message");
            adapter.notifyDataSetChanged()
            messageInput.setText("")
        })
    }

    private fun sendMessage() {
        val input = messageInput.text.toString()
        if (input != "") {
            sendMessage(input)
        }

        messageInput.requestFocus()
    }

    /// Send media message
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            Timber.d("Uri: ${data?.data}")

//            startService<MediaService>(
//                MediaService.EXTRA_ACTION to MediaService.EXTRA_ACTION_UPLOAD,
//                MediaService.EXTRA_MEDIA_URI to data?.data.toString(),
//                MediaService.EXTRA_CHANNEL to channel)
        }
    }

    override fun onMessageAdded(message: Message) {
        setupListView(channel!!)
    }

    override fun onMessageUpdated(message: Message?, reason: Message.UpdateReason) {
        if (message != null) {
            TwilioApplication.instance.showToast("onMessageUpdated for ${message.sid}, changed because of ${reason}")
        } else {
            Timber.d("Received onMessageUpdated")
        }
    }

    override fun onMessageDeleted(message: Message?) {
        if (message != null) {
            TwilioApplication.instance.showToast("onMessageDeleted for ${message.sid}")
        } else {
            Timber.d("Received onMessageDeleted.")
        }
    }

    override fun onMemberAdded(member: Member?) {
        if (member != null) {
            TwilioApplication.instance.showToast("${member.identity} joined")
        }
    }

    override fun onMemberUpdated(member: Member?, reason: Member.UpdateReason) {
        if (member != null) {
            TwilioApplication.instance.showToast("${member.identity} changed because of ${reason}")
        }
    }

    override fun onMemberDeleted(member: Member?) {
        if (member != null) {
            TwilioApplication.instance.showToast("${member.identity} deleted")
        }
    }

    override fun onTypingStarted(member: Member?) {
        if (member != null) {
            val text = "${member.identity} is typing ..."
            typingIndicator.text = text
            typingIndicator.setTextColor(Color.RED)
            Timber.d(text)
        }
    }

    override fun onTypingEnded(member: Member?) {
        if (member != null) {
            typingIndicator.text = null
            Timber.d("${member.identity} has ended typing")
        }
    }

    override fun onSynchronizationChanged(channel: Channel) {
        Timber.d("Received onSynchronizationChanged callback for ${channel.friendlyName}")
    }

    data class MessageItem(val message: Message, val members: Members, internal var currentUser: String);

    companion object {
        private val MESSAGE_OPTIONS = arrayOf("Remove", "Edit", "Get Attributes", "Edit Attributes")
        private val EDIT_OPTIONS = arrayOf("Change Friendly Name", "Change Topic", "List Members", "Invite Member", "Add Member", "Remove Member", "Leave", "Destroy", "Get Attributes", "Change Unique Name", "Get Unique Name", "Get message index 0", "Set all consumed", "Set none consumed")

        private val NAME_CHANGE = 0
        private val TOPIC_CHANGE = 1
        private val LIST_MEMBERS = 2
        private val INVITE_MEMBER = 3
        private val ADD_MEMBER = 4
        private val REMOVE_MEMBER = 5
        private val LEAVE = 6
        private val CHANNEL_DESTROY = 7
        private val CHANNEL_ATTRIBUTE = 8
        private val SET_CHANNEL_UNIQUE_NAME = 9
        private val GET_CHANNEL_UNIQUE_NAME = 10
        private val GET_MESSAGE_BY_INDEX = 11
        private val SET_ALL_CONSUMED = 12
        private val SET_NONE_CONSUMED = 13

        private val REMOVE = 0
        private val EDIT = 1
        private val GET_ATTRIBUTES = 2
        private val SET_ATTRIBUTES = 3

        private val FILE_REQUEST = 1000;
    }
}
