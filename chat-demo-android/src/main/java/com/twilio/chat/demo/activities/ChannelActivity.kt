package com.twilio.chat.demo.activities

import java.util.Comparator
import java.util.HashMap
import java.util.Random
import com.twilio.chat.Channel
import com.twilio.chat.Channel.ChannelType
import com.twilio.chat.ChannelDescriptor
import com.twilio.chat.CallbackListener
import com.twilio.chat.ChatClientListener
import com.twilio.chat.ChatClient
import com.twilio.chat.ErrorInfo
import com.twilio.chat.User
import com.twilio.chat.Paginator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.view.*
import com.twilio.chat.demo.*
import com.twilio.chat.demo.views.ChannelViewHolder
import eu.inloop.simplerecycleradapter.ItemClickListener
import eu.inloop.simplerecycleradapter.SettableViewHolder
import eu.inloop.simplerecycleradapter.SimpleRecyclerAdapter
import kotlinx.android.synthetic.main.activity_channel.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import timber.log.Timber
import org.json.JSONObject
import org.json.JSONException
import ToastStatusListener

class ChannelActivity : Activity(), ChatClientListener {
    private lateinit var basicClient: BasicChatClient
    private val channels = HashMap<String, ChannelModel>()
    private lateinit var adapter: SimpleRecyclerAdapter<ChannelModel>
    private var incomingChannelInvite: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channel)

        basicClient = TwilioApplication.instance.basicClient
        basicClient.chatClient?.setListener(this@ChannelActivity)
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
            R.id.action_create_public -> showCreateChannelDialog(ChannelType.PUBLIC)
            R.id.action_create_private -> showCreateChannelDialog(ChannelType.PRIVATE)
            R.id.action_create_public_withoptions -> createChannelWithType(ChannelType.PUBLIC)
            R.id.action_create_private_withoptions -> createChannelWithType(ChannelType.PRIVATE)
            R.id.action_search_by_unique_name -> showSearchChannelDialog()
            R.id.action_user_info -> startActivity(Intent(applicationContext, UserActivity::class.java))
            R.id.action_logout -> {
                basicClient.shutdown()
                finish()
            }
            R.id.action_unregistercm -> basicClient.unregisterFcmToken()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createChannelWithType(type: ChannelType) {
        val rand = Random()
        val value = rand.nextInt(50)

        val attrs = JSONObject()
        try {
            attrs.put("topic", "testing channel creation with options " + value)
        } catch (xcp: JSONException) {
            Timber.e("JSON exception", xcp)
        }

        val typ = if (type == ChannelType.PRIVATE) "Priv" else "Pub"

        val builder = basicClient.chatClient?.channels?.channelBuilder()
        if (builder == null) return

        builder.withFriendlyName(typ + "_TestChannelF_" + value)
                .withUniqueName(typ + "_TestChannelU_" + value)
                .withType(type)
                .withAttributes(attrs)
                .build(object : CallbackListener<Channel>() {
                    override fun onSuccess(newChannel: Channel) {
                        Timber.d("Successfully created a channel with options.")
                        channels.put(newChannel.sid, ChannelModel(newChannel))
                        refreshChannelList()
                    }

                    override fun onError(errorInfo: ErrorInfo?) {
                        Timber.e("Error creating a channel")
                    }
                })
    }

    private fun showCreateChannelDialog(type: ChannelType) {
        val builder = AlertDialog.Builder(this@ChannelActivity)

        builder.setView(
            verticalLayout {
                textView { text = "Enter ${type} name" }
                val channel_name = editText {
                    hintResource = R.string.title_add_channel_name
                }.lparams(width = matchParent)
                button(R.string.create) {
                    onClick {
                        val channelName = channel_name.text.toString()
                        Timber.d("Creating channel with friendly Name|$channelName|")
                        basicClient.chatClient?.channels?.createChannel(channelName, type, object : CallbackListener<Channel>() {
                            override fun onSuccess(newChannel: Channel?) {
                                Timber.d("Successfully created a channel")
                                if (newChannel != null) {
                                    Timber.d("Channel created with sid|${newChannel.sid}| and type |${newChannel.type}|")
                                    channels.put(newChannel.sid, ChannelModel(newChannel))
                                    refreshChannelList()
                                }
                            }

                            override fun onError(errorInfo: ErrorInfo?) {
                                TwilioApplication.instance.showError("Error creating channel",
                                        errorInfo!!)
                            }
                        })
                    }
                }
            }.view()
        )
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.create().show()
    }

    private fun showSearchChannelDialog() {
        val builder = AlertDialog.Builder(this@ChannelActivity)

        builder.setView(
                verticalLayout {
                    textView { text = "Enter unique channel name" }
                    val channel_name = editText {
                        hintResource = R.string.title_add_channel_name
                    }.lparams(width = matchParent)
                    button(R.string.create) {
                        onClick {
                            val channelSid = channel_name.text.toString()
                            Timber.d("Searching for " + channelSid)
                            basicClient.chatClient?.channels?.getChannel(channelSid, object : CallbackListener<Channel>() {
                                override fun onSuccess(channel: Channel?) {
                                    if (channel != null) {
                                        TwilioApplication.instance.showToast("${channel.sid}: ${channel.friendlyName}")
                                    } else {
                                        TwilioApplication.instance.showToast("Channel not found.")
                                    }
                                }
                            })
                        }
                    }
                }.view()
        )
        builder.setNegativeButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.create().show()
    }

    private fun setupListView() {
        adapter = SimpleRecyclerAdapter(
                ItemClickListener { channel: ChannelModel, _, _ ->
                    if (channel.status == Channel.ChannelStatus.JOINED) {
                        Handler().postDelayed({
                            channel.getChannel(object : CallbackListener<Channel>() {
                                override fun onSuccess(chan: Channel) {
                                    val i = Intent(this@ChannelActivity, MessageActivity::class.java)
                                    i.putExtra(Constants.EXTRA_CHANNEL, chan as Parcelable)
                                    i.putExtra(Constants.EXTRA_CHANNEL_SID, chan.sid)
                                    startActivity(i)
                                }
                            })
                        }, 0)
                        return@ItemClickListener
                    }
                    val builder = AlertDialog.Builder(this@ChannelActivity)
                    builder.setTitle(R.string.select_action)
                            .setItems(CHANNEL_OPTIONS) { dialog, which ->
                                if (which == JOIN) {
                                    dialog.cancel()
                                    channel.join(
                                            ToastStatusListener("Successfully joined channel",
                                                    "Failed to join channel") {
                                                refreshChannelList()
                                            })
                                }
                            }
                    builder.show()
                },
                object : SimpleRecyclerAdapter.CreateViewHolder<ChannelModel>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettableViewHolder<ChannelModel> {
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

    private fun getChannelsPage(paginator: Paginator<ChannelDescriptor>) {
        for (cd in paginator.items) {
            Timber.e("Adding channel descriptor for sid|${cd.sid}| friendlyName ${cd.friendlyName}")
            channels.put(cd.sid, ChannelModel(cd))
        }
        refreshChannelList()

        if (paginator.hasNextPage()) {
            paginator.requestNextPage(object : CallbackListener<Paginator<ChannelDescriptor>>() {
                override fun onSuccess(channelDescriptorPaginator: Paginator<ChannelDescriptor>) {
                    getChannelsPage(channelDescriptorPaginator)
                }
            })
        } else {
            // Get subscribed channels last - so their status will overwrite whatever we received
            // from public list. Ugly workaround for now.
            val chans = basicClient.chatClient?.channels?.subscribedChannels
            if (chans != null) {
                for (channel in chans) {
                    channels.put(channel.sid, ChannelModel(channel))
                }
            }
            refreshChannelList()
        }
    }

    // Initialize channels with channel list
    private fun getChannels() {
        channels.clear()

        basicClient.chatClient?.channels?.getPublicChannelsList(object : CallbackListener<Paginator<ChannelDescriptor>>() {
            override fun onSuccess(channelDescriptorPaginator: Paginator<ChannelDescriptor>) {
                getChannelsPage(channelDescriptorPaginator)
            }
        })

        basicClient.chatClient?.channels?.getUserChannelsList(object : CallbackListener<Paginator<ChannelDescriptor>>() {
            override fun onSuccess(channelDescriptorPaginator: Paginator<ChannelDescriptor>) {
                getChannelsPage(channelDescriptorPaginator)
            }
        })
    }

    private fun showIncomingInvite(channel: Channel) {
        doAsync {
            if (incomingChannelInvite == null) {
                incomingChannelInvite = AlertDialog.Builder(this@ChannelActivity)
                        .setTitle(R.string.channel_invite)
                        .setMessage(R.string.channel_invite_message)
                        .setPositiveButton(
                                R.string.join
                        ) { _, _ ->
                            channel.join(ToastStatusListener(
                                    "Successfully joined channel",
                                    "Failed to join channel") {
                                channels.put(channel.sid, ChannelModel(channel))
                                refreshChannelList()
                            })
                            incomingChannelInvite = null
                        }
                        .setNegativeButton(
                                R.string.decline
                        ) { _, _ ->
                            channel.declineInvitation(ToastStatusListener(
                                    "Successfully declined channel invite",
                                    "Failed to decline channel invite"))
                            incomingChannelInvite = null
                        }
                        .create()
            }
            incomingChannelInvite!!.show()
        }
    }

    private inner class CustomChannelComparator : Comparator<ChannelModel> {
        override fun compare(lhs: ChannelModel, rhs: ChannelModel): Int {
            return lhs.friendlyName!!.compareTo(rhs.friendlyName!!)
        }
    }

    //=============================================================
    // ChatClientListener
    //=============================================================

    override fun onChannelJoined(channel: Channel) {
        Timber.d("Received onChannelJoined callback for channel |" + channel.friendlyName + "|")
        channels.put(channel.sid, ChannelModel(channel))
        refreshChannelList()
    }

    override fun onChannelAdded(channel: Channel) {
        Timber.d("Received onChannelAdd callback for channel |" + channel.friendlyName + "|")
        channels.put(channel.sid, ChannelModel(channel))
        refreshChannelList()
    }

    override fun onChannelUpdated(channel: Channel, reason: Channel.UpdateReason) {
        Timber.d("Received onChannelChange callback for channel |" + channel.friendlyName
                + "| with reason " + reason.toString())
        channels.put(channel.sid, ChannelModel(channel))
        refreshChannelList()
    }

    override fun onChannelDeleted(channel: Channel) {
        Timber.d("Received onChannelDelete callback for channel |" + channel.friendlyName
                + "|")
        channels.remove(channel.sid)
        refreshChannelList()
    }

    override fun onChannelInvited(channel: Channel) {
        channels.put(channel.sid, ChannelModel(channel))
        refreshChannelList()
        showIncomingInvite(channel)
    }

    override fun onChannelSynchronizationChange(channel: Channel) {
        Timber.e("Received onChannelSynchronizationChange callback for channel |"
                + channel.friendlyName
                + "| with new status " + channel.status.toString())
        refreshChannelList()
    }

    override fun onClientSynchronization(status: ChatClient.SynchronizationStatus) {
        Timber.e("Received onClientSynchronization callback " + status.toString())
    }

    override fun onUserUpdated(user: User, reason: User.UpdateReason) {
        Timber.e("Received onUserUpdated callback for " + reason.toString())
    }

    override fun onUserSubscribed(user: User) {
        Timber.e("Received onUserSubscribed callback")
    }

    override fun onUserUnsubscribed(user: User) {
        Timber.e("Received onUserUnsubscribed callback")
    }

    override fun onNotification(channelId: String, messageId: String) {
        Timber.d("Received new push notification")
        TwilioApplication.instance.showToast("Received new push notification")
    }

    override fun onNotificationSubscribed() {
        Timber.d("Subscribed to push notifications")
        TwilioApplication.instance.showToast("Subscribed to push notifications")
    }

    override fun onNotificationFailed(errorInfo: ErrorInfo) {
        Timber.d("Failed to subscribe to push notifications")
        TwilioApplication.instance.showError("Failed to subscribe to push notifications", errorInfo)
    }

    override fun onError(errorInfo: ErrorInfo) {
        TwilioApplication.instance.showError("Received error", errorInfo)
    }

    override fun onConnectionStateChange(connectionState: ChatClient.ConnectionState) {
        TwilioApplication.instance.showToast("Transport state changed to " + connectionState.toString())
    }

    companion object {
        private val CHANNEL_OPTIONS = arrayOf("Join")
        private val JOIN = 0
    }
}
