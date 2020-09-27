package com.twilio.conversations.demo.activities

import java.util.Comparator
import java.util.HashMap
import java.util.Random
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.*
import com.twilio.conversations.demo.*
import com.twilio.conversations.demo.views.ChannelViewHolder
import eu.inloop.simplerecycleradapter.ItemClickListener
import eu.inloop.simplerecycleradapter.SettableViewHolder
import eu.inloop.simplerecycleradapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.activity_channel.*
import org.jetbrains.anko.*
import org.json.JSONObject
import org.json.JSONException
import ChatCallbackListener
import ToastStatusListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.twilio.conversations.*
import com.twilio.conversations.demo.R
import com.twilio.conversations.demo.utils.Where

class ChannelActivity : Activity(), ConversationsClientListener, AnkoLogger {
    private lateinit var basicClient: BasicConversationsClient
    private val channels = HashMap<String, ConversationModel>()
    private lateinit var adapter: SimpleRecyclerAdapter<ConversationModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        basicClient = TwilioApplication.instance.basicClient
        basicClient.conversationsClient?.addListener(this@ChannelActivity)
        setupListView()
    }

    override fun onResume() {
        super.onResume()
        getChannels()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.channel, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_create -> showCreateChannelDialog()
            R.id.action_create_with_options -> createChannelWithType()
            R.id.action_search_by_unique_name -> showSearchChannelDialog()
            R.id.action_user_info -> startActivity(Intent(applicationContext, UserActivity::class.java))
            R.id.action_update_token -> basicClient.updateToken()
            R.id.action_logout -> {
                basicClient.shutdown()
                finish()
            }
            R.id.action_unregistercm -> basicClient.unregisterFcmToken()
            R.id.action_crash_in_java -> throw RuntimeException("Simulated crash in ChannelActivity.kt")
//            R.id.action_crash_in_chat_client -> basicClient.conversationsClient!!.simulateCrash(Where.CHAT_CLIENT_CPP)
//            R.id.action_crash_in_tm_client -> basicClient.conversationsClient!!.simulateCrash(Where.TM_CLIENT_CPP)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createChannelWithType() {
        val rand = Random()
        val value = rand.nextInt(50)

        val attrs = JSONObject()
        try {
            attrs.put("topic", "testing channel creation with options ${value}")
        } catch (xcp: JSONException) {
            error { "JSON exception: $xcp" }
        }

        val typ = "Priv"

        val builder = basicClient.conversationsClient?.conversationBuilder()

        builder?.withFriendlyName("${typ}_TestChannelF_${value}")
               ?.withUniqueName("${typ}_TestChannelU_${value}")
               ?.withAttributes(Attributes(attrs))
               ?.build(object : CallbackListener<Conversation> {
                    override fun onSuccess(newConversation: Conversation) {
                        debug { "Successfully created a channel with options." }
                        channels.put(newConversation.sid, ConversationModel(newConversation))
                        refreshChannelList()
                    }

                    override fun onError(errorInfo: ErrorInfo?) {
                        error { "Error creating a channel" }
                    }
                })
    }

    private fun showCreateChannelDialog() {
        alert(R.string.title_add_channel) {
            customView {
                verticalLayout {
                    textView {
                        text = "Enter conversation name"
                        padding = dip(10)
                    }.lparams(width = matchParent)
                    val channel_name = editText { padding = dip(10) }.lparams(width = matchParent)
                    positiveButton(R.string.create) {
                        val channelName = channel_name.text.toString()
                        debug { "Creating channel with friendly Name|$channelName|" }
                        basicClient.conversationsClient?.createConversation(channelName, ChatCallbackListener<Conversation>() {
                            debug { "Channel created with sid|${it.sid}|" }
                            channels.put(it.sid, ConversationModel(it))
                            refreshChannelList()
                        })
                    }
                    negativeButton(R.string.cancel) {}
                }
            }
        }.show()
    }

    private fun showSearchChannelDialog() {
        alert(R.string.title_find_channel) {
            customView {
                verticalLayout {
                    textView {
                        text = "Enter unique channel name"
                        padding = dip(10)
                    }.lparams(width = matchParent)
                    val channel_name = editText { padding = dip(10) }.lparams(width = matchParent)
                    positiveButton(R.string.search) {
                        val channelSid = channel_name.text.toString()
                        debug { "Searching for ${channelSid}" }
                        basicClient.conversationsClient?.getConversation(channelSid, ChatCallbackListener<Conversation?>() {
                            if (it != null) {
                                TwilioApplication.instance.showToast("${it.sid}: ${it.friendlyName}")
                            } else {
                                TwilioApplication.instance.showToast("Channel not found.")
                            }
                        })
                    }
                    negativeButton(R.string.cancel) {}
                }
            }
        }.show()
    }

    private fun setupListView() {
        adapter = SimpleRecyclerAdapter(
                ItemClickListener { conversation: ConversationModel, _, _ ->
                    if (conversation.status == Conversation.ConversationStatus.JOINED) {
                        Handler().postDelayed({
                            conversation.getChannel(ChatCallbackListener<Conversation>() {
                                startActivity<MessageActivity>(
                                    Constants.EXTRA_CHANNEL_SID to it.sid
                                )
                            })
                        }, 0)
                        return@ItemClickListener
                    }
                    alert(R.string.select_action) {
                        positiveButton("Join") { dialog ->
                            conversation.join(
                                    ToastStatusListener("Successfully joined channel",
                                            "Failed to join channel") {
                                        refreshChannelList()
                                    })
                            dialog.cancel()
                        }
                        negativeButton(R.string.cancel) {}
                    }.show()
                },
                object : SimpleRecyclerAdapter.CreateViewHolder<ConversationModel>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettableViewHolder<ConversationModel> {
                        return ChannelViewHolder(this@ChannelActivity, parent);
                    }
                })

        channel_list.adapter = adapter
        channel_list.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
    }

    private fun refreshChannelList() {
        adapter.clear()
        adapter.addItems(channels.values.sortedWith(CustomChannelComparator()))
        adapter.notifyDataSetChanged()
    }

    // Initialize channels with channel list
    private fun getChannels() {
        channels.clear()

        for (cd in basicClient.conversationsClient!!.myConversations) {
            error { "Adding channel descriptor for sid|${cd.sid}| friendlyName ${cd.friendlyName}" }
            channels.put(cd.sid, ConversationModel(cd))
        }
        refreshChannelList()
    }

    private inner class CustomChannelComparator : Comparator<ConversationModel> {
        override fun compare(lhs: ConversationModel, rhs: ConversationModel): Int {
            return lhs.friendlyName.compareTo(rhs.friendlyName)
        }
    }

    //=============================================================
    // ChatClientListener
    //=============================================================

    override fun onConversationAdded(conversation: Conversation) {
        debug { "Received onChannelAdded callback for channel |${conversation.friendlyName}|" }
        channels.put(conversation.sid, ConversationModel(conversation))
        refreshChannelList()
    }

    override fun onConversationUpdated(conversation: Conversation, reason: Conversation.UpdateReason) {
        debug { "Received onChannelUpdated callback for channel |${conversation.friendlyName}| with reason ${reason}" }
        channels.put(conversation.sid, ConversationModel(conversation))
        refreshChannelList()
    }

    override fun onConversationDeleted(conversation: Conversation) {
        debug { "Received onChannelDeleted callback for channel |${conversation.friendlyName}|" }
        channels.remove(conversation.sid)
        refreshChannelList()
    }

    override fun onConversationSynchronizationChange(conversation: Conversation) {
        error { "Received onChannelSynchronizationChange callback for channel |${conversation.friendlyName}| with new status ${conversation.status}" }
        refreshChannelList()
    }

    override fun onClientSynchronization(status: ConversationsClient.SynchronizationStatus) {
        error { "Received onClientSynchronization callback ${status}" }
    }

    override fun onUserUpdated(user: User, reason: User.UpdateReason) {
        error { "Received onUserUpdated callback for ${reason}" }
    }

    override fun onUserSubscribed(user: User) {
        error { "Received onUserSubscribed callback" }
    }

    override fun onUserUnsubscribed(user: User) {
        error { "Received onUserUnsubscribed callback" }
    }

    override fun onNewMessageNotification(conversationSid: String?, messageSid: String?, messageIndex: Long) {
        TwilioApplication.instance.showToast("Received onNewMessage push notification")
    }

    override fun onAddedToConversationNotification(conversationSid: String?) {
        TwilioApplication.instance.showToast("Received onAddedToChannel push notification")
    }

    override fun onRemovedFromConversationNotification(conversationSid: String?) {
        TwilioApplication.instance.showToast("Received onRemovedFromChannel push notification")
    }

    override fun onNotificationSubscribed() {
        TwilioApplication.instance.showToast("Subscribed to push notifications")
    }

    override fun onNotificationFailed(errorInfo: ErrorInfo) {
        TwilioApplication.instance.showError("Failed to subscribe to push notifications", errorInfo)
    }

    override fun onError(errorInfo: ErrorInfo) {
        TwilioApplication.instance.showError("Received error", errorInfo)
    }

    override fun onConnectionStateChange(connectionState: ConversationsClient.ConnectionState) {
        TwilioApplication.instance.showToast("Transport state changed to ${connectionState}")
    }

    override fun onTokenExpired() {
        basicClient.onTokenExpired()
    }

    override fun onTokenAboutToExpire() {
        basicClient.onTokenAboutToExpire()
    }

    companion object {
        private val CHANNEL_OPTIONS = arrayOf("Join")
        private val JOIN = 0
    }
}
