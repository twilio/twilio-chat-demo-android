package com.twilio.conversations.demo.activities

import java.util.ArrayList
import java.util.Comparator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.twilio.conversations.demo.Constants
import com.twilio.conversations.demo.R
import com.twilio.conversations.demo.TwilioApplication
import com.twilio.conversations.demo.services.MediaService
import com.twilio.conversations.demo.views.MemberViewHolder
import com.twilio.conversations.demo.views.MessageViewHolder
import eu.inloop.simplerecycleradapter.ItemClickListener
import eu.inloop.simplerecycleradapter.ItemLongClickListener
import eu.inloop.simplerecycleradapter.SettableViewHolder
import eu.inloop.simplerecycleradapter.SimpleRecyclerAdapter
import org.json.JSONException
import org.json.JSONObject
import kotlinx.android.synthetic.main.activity_message.*
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import ChatStatusListener
import ChatCallbackListener
import ToastStatusListener
import android.os.Parcelable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.*

// RecyclerView Anko
fun ViewManager.recyclerView() = recyclerView(theme = 0) {}

inline fun ViewManager.recyclerView(init: RecyclerView.() -> Unit): RecyclerView {
    return ankoView({ RecyclerView(it) }, theme = 0, init = init)
}

fun ViewManager.recyclerView(theme: Int = 0) = recyclerView(theme) {}

inline fun ViewManager.recyclerView(theme: Int = 0, init: RecyclerView.() -> Unit): RecyclerView {
    return ankoView({ RecyclerView(it) }, theme, init)
}
// End RecyclerView Anko

class MessageActivity : Activity(), ConversationListener, AnkoLogger {
    private lateinit var adapter: SimpleRecyclerAdapter<MessageItem>
    private var conversation: Conversation? = null

    private val messageItemList = ArrayList<MessageItem>()
    private lateinit var identity: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        createUI()
    }

    override fun onDestroy() {
        conversation?.removeListener(this@MessageActivity);
        super.onDestroy();
    }

    override fun onResume() {
        super.onResume()
        createUI()
    }

    private fun createUI() {
        if (intent != null) {
            val basicClient = TwilioApplication.instance.basicClient
            identity = basicClient.conversationsClient!!.myIdentity
            val conversationSid = intent.getStringExtra(Constants.EXTRA_CHANNEL_SID)
            basicClient.conversationsClient!!.getConversation(conversationSid, ChatCallbackListener<Conversation>() {
                conversation = it
                conversation!!.addListener(this@MessageActivity)
                this@MessageActivity.title = conversation!!.friendlyName

                setupListView(conversation!!)

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
        selector("Select an option", EDIT_OPTIONS) { _, which ->
            when (which) {
                NAME_CHANGE -> showChangeNameDialog()
                TOPIC_CHANGE -> showChangeTopicDialog()
                LIST_MEMBERS -> {
                    val client = TwilioApplication.instance.basicClient.conversationsClient!!
                    // Members.getMembersList() way
                    val members = conversation!!.participantsList
                    val name = StringBuffer()
                    for (i in members.indices) {
                        name.append(members[i].identity)
                        if (i + 1 < members.size) {
                            name.append(", ")
                        }
                        members[i].getAndSubscribeUser(ChatCallbackListener<User>() {
                            debug { "Got subscribed user from member: ${it.identity}" }
                        })
                    }
                    TwilioApplication.instance.showToast(name.toString(), Toast.LENGTH_LONG)
                    // Users.getSubscribedUsers() everybody we subscribed to at the moment
                    val userList = client.subscribedUsers
                    val name2 = StringBuffer()
                    for (i in userList.indices) {
                        name2.append(userList[i].identity)
                        if (i + 1 < userList.size) {
                            name2.append(", ")
                        }
                    }
                    TwilioApplication.instance.showToast("Subscribed users: ${name2.toString()}", Toast.LENGTH_LONG)
                }
                ADD_MEMBER -> showAddMemberDialog()
                REMOVE_MEMBER -> showRemoveMemberDialog()
                LEAVE -> conversation!!.leave(ToastStatusListener(
                            "Successfully left channel", "Error leaving channel") {
                        finish()
                    })
                CHANNEL_DESTROY -> conversation!!.destroy(ToastStatusListener(
                        "Successfully destroyed channel", "Error destroying channel") {
                        finish()
                    })
                CHANNEL_ATTRIBUTE -> try {
                        TwilioApplication.instance.showToast(conversation!!.attributes.toString())
                    } catch (e: JSONException) {
                        TwilioApplication.instance.showToast("JSON exception in channel attributes")
                    }
                SET_CHANNEL_UNIQUE_NAME -> showChangeUniqueNameDialog()
                GET_CHANNEL_UNIQUE_NAME -> TwilioApplication.instance.showToast(conversation!!.uniqueName)
                GET_MESSAGE_BY_INDEX -> conversation!!.getMessageByIndex(conversation!!.lastReadMessageIndex!!, ChatCallbackListener<Message>() {
                        TwilioApplication.instance.showToast("SUCCESS GET MESSAGE BY IDX")
                        error { "MESSAGES ${it.messageBody}, CHANNEL ${it.conversation.sid}" }
                    })
                SET_ALL_CONSUMED -> conversation!!.setAllMessagesRead(ChatCallbackListener<Long>()
                    {unread -> TwilioApplication.instance.showToast("$unread messages still unread")})
                SET_NONE_CONSUMED -> conversation!!.setAllMessagesUnread(ChatCallbackListener<Long>()
                    {unread -> TwilioApplication.instance.showToast("$unread messages still unread")})
                DISABLE_PUSHES -> conversation!!.setNotificationLevel(Conversation.NotificationLevel.MUTED, ToastStatusListener(
                        "Successfully disabled pushes", "Error disabling pushes") {
                        finish()
                    })
                ENABLE_PUSHES -> conversation!!.setNotificationLevel(Conversation.NotificationLevel.DEFAULT, ToastStatusListener(
                        "Successfully enabled pushes", "Error enabling pushes") {
                        finish()
                    })
            }
        }
    }

    private fun showChangeNameDialog() {
        alert(R.string.title_update_friendly_name) {
            customView {
                val friendly_name = editText { text.append(conversation!!.friendlyName) }
                positiveButton(R.string.update) {
                    val friendlyName = friendly_name.text.toString()
                    debug { friendlyName }
                    conversation!!.setFriendlyName(friendlyName, ToastStatusListener(
                            "Successfully changed name", "Error changing name"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showChangeTopicDialog() {
        alert(R.string.title_update_topic) {
            customView {
                val topic = editText { text.append(conversation!!.attributes.toString()) }
                positiveButton(R.string.change_topic) {
                    val topicText = topic.text.toString()
                    debug { topicText }

                    try { // @todo Get attributes to update
                        JSONObject().apply {
                            put("Topic", topicText)
                            conversation!!.setAttributes(Attributes(this), ToastStatusListener(
                                    "Attributes were set successfullly.",
                                    "Setting attributes failed"))
                        }
                    } catch (ignored: JSONException) {
                        // whatever
                    }
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
                    debug { memberName }
                    conversation!!.addParticipantByIdentity(memberName, Attributes(), ToastStatusListener(
                            "Successful addByIdentity",
                            "Error adding member"))
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showRemoveMemberDialog() {
        alert("Remove members") {
            customView {
                verticalLayout {
                    val view = recyclerView {}.lparams(width = dip(250), height = matchParent)
                    negativeButton(R.string.cancel) {}

                    view.adapter = SimpleRecyclerAdapter<Participant>(
                            ItemClickListener { participant: Participant, _, _ ->
                                conversation!!.removeParticipant(participant, ToastStatusListener(
                                        "Successful removeMember operation",
                                        "Error in removeMember operation"))
                                // @todo update memberList here
                            },
                            object : SimpleRecyclerAdapter.CreateViewHolder<Participant>() {
                                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettableViewHolder<Participant> {
                                    return MemberViewHolder(this@MessageActivity, parent)
                                }
                            },
                            conversation!!.participantsList)

                    view.layoutManager = LinearLayoutManager(this@MessageActivity).apply {
                        orientation = LinearLayoutManager.VERTICAL
                    }
                }
            }
        }.show()
    }

    private fun showUpdateMessageDialog(message: Message) {
        alert(R.string.title_update_message) {
            customView {
                val messageText = editText { text.append(message.messageBody) }
                positiveButton(R.string.update) {
                    val text = messageText.text.toString()
                    debug { text }
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
                    debug { text }
                    try {
                        JSONObject(text).apply {
                            message.setAttributes(Attributes(this), ToastStatusListener(
                                    "Success updating message attributes",
                                    "Error updating message attributes") {
                                // @todo only need to update one message
                                loadAndShowMessages()
                            })
                        }
                    } catch (e: JSONException) {
                        error { "Invalid JSON attributes entered, using old value" }
                    }
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun showChangeUniqueNameDialog() {
        alert("Update channel unique name") {
            customView {
                val uniqueNameText = editText { text.append(conversation!!.uniqueName) }
                positiveButton(R.string.update) {
                    val uniqueName = uniqueNameText.text.toString()
                    debug { uniqueName }
                    conversation!!.setUniqueName(uniqueName, ChatStatusListener());
                }
                negativeButton(R.string.cancel) {}
            }
        }.show()
    }

    private fun loadAndShowMessages() {
        conversation!!.getLastMessages(50, ChatCallbackListener<List<Message>>() {
            messageItemList.clear()
            if (it.isNotEmpty()) {
                for (i in it.indices) {
                    messageItemList.add(MessageItem(it[i], identity))
                }
            }
            adapter.clear()
            adapter.addItems(messageItemList)
            adapter.notifyDataSetChanged()
        })
    }

    private fun setupInput() {
        // Setup our input methods. Enter key on the keyboard or pushing the send button
        messageInput.apply {
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (conversation != null) {
                        conversation!!.typing()
                    }
                }
            })

            setOnEditorActionListener { _, actionId, keyEvent ->
                if (actionId == EditorInfo.IME_NULL && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    sendMessage()
                }
                true
            }
        }

        sendButton.apply {
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
            return lhs.dateCreated.compareTo(rhs.dateCreated)
        }
    }

    private fun setupListView(conversation: Conversation) {
//        message_list_view.viewTreeObserver.addOnScrollChangedListener {
//            if (message_list_view.lastVisiblePosition >= 0 && message_list_view.lastVisiblePosition < adapter.itemCount) {
//                val item = adapter.getItem(message_list_view.lastVisiblePosition)
//                if (item != null && messagesObject != null)
//                    channel.messages.advanceLastConsumedMessageIndex(
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
                    selector("Select an option", MESSAGE_OPTIONS) { dialog, which ->
                        when (which) {
                            REMOVE -> {
                                dialog.cancel()
                                conversation.removeMessage(
                                        message.message, ToastStatusListener(
                                        "Successfully removed message. It should be GONE!!",
                                        "Error removing message") {
                                    messageItemList.remove(message)
                                    adapter.notifyDataSetChanged()
                                })
                            }
                            EDIT -> showUpdateMessageDialog(message.message)
                            GET_ATTRIBUTES -> {
                                try {
                                    TwilioApplication.instance.showToast(message.message.attributes.toString())
                                } catch (e: JSONException) {
                                    TwilioApplication.instance.showToast("Error parsing message attributes")
                                }
                            }
                            SET_ATTRIBUTES -> showUpdateMessageAttributesDialog(message.message)
                        }
                    }
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
        conversation!!.sendMessage(Message.options().withBody(text), ChatCallbackListener<Message>() {
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
            debug { "Uri: ${data?.data}" }

            startService<MediaService>(
                MediaService.EXTRA_ACTION to MediaService.EXTRA_ACTION_UPLOAD,
                MediaService.EXTRA_CHANNEL_SID to conversation!!.sid,
                MediaService.EXTRA_MEDIA_URI to data?.data.toString())
        }
    }

    override fun onMessageAdded(message: Message) {
        setupListView(conversation!!)

        startService<MediaService>(
            MediaService.EXTRA_ACTION to MediaService.EXTRA_ACTION_DOWNLOAD,
            MediaService.EXTRA_CHANNEL_SID to conversation!!.sid,
            MediaService.EXTRA_MESSAGE_INDEX to message.messageIndex)
    }

    override fun onMessageUpdated(message: Message?, reason: Message.UpdateReason) {
        if (message != null) {
            TwilioApplication.instance.showToast("onMessageUpdated for ${message.sid}, changed because of ${reason}")
        } else {
            debug { "Received onMessageUpdated" }
        }
    }

    override fun onMessageDeleted(message: Message?) {
        if (message != null) {
            TwilioApplication.instance.showToast("onMessageDeleted for ${message.sid}")
        } else {
            debug { "Received onMessageDeleted." }
        }
    }

    override fun onParticipantAdded(participant: Participant?) {
        if (participant != null) {
            TwilioApplication.instance.showToast("${participant.identity} joined")
        }
    }

    override fun onParticipantUpdated(member: Participant?, reason: Participant.UpdateReason) {
        if (member != null) {
            TwilioApplication.instance.showToast("${member.identity} changed because of ${reason}")
        }
    }

    override fun onParticipantDeleted(member: Participant?) {
        if (member != null) {
            TwilioApplication.instance.showToast("${member.identity} deleted")
        }
    }

    override fun onTypingStarted(channel: Conversation?, member: Participant?) {
        if (member != null) {
            val text = "${member.identity} is typing ..."
            typingIndicator.text = text
            typingIndicator.setTextColor(Color.RED)
            debug { text }
        }
    }

    override fun onTypingEnded(channel: Conversation?, member: Participant?) {
        if (member != null) {
            typingIndicator.text = null
            debug { "${member.identity} finished typing" }
        }
    }

    override fun onSynchronizationChanged(channel: Conversation) {
        debug { "Received onSynchronizationChanged callback for ${channel.friendlyName}" }
    }

    data class MessageItem(val message: Message, /*val members: Members,*/ internal var currentUser: String);

    companion object {
        private val MESSAGE_OPTIONS = listOf("Remove", "Edit", "Get Attributes", "Edit Attributes")
        private val REMOVE = 0
        private val EDIT = 1
        private val GET_ATTRIBUTES = 2
        private val SET_ATTRIBUTES = 3

        private val EDIT_OPTIONS = listOf("Change Friendly Name", "Change Topic", "List Members", "Invite Member", "Add Member", "Remove Member", "Leave", "Destroy", "Get Attributes", "Change Unique Name", "Get Unique Name", "Get message index 0", "Set all consumed", "Set none consumed", "Disable Pushes", "Enable Pushes")
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
        private val DISABLE_PUSHES = 14
        private val ENABLE_PUSHES = 15

        private val FILE_REQUEST = 1000;
    }
}
