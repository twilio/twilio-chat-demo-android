package com.twilio.chat.demo.views

import android.content.Context
import com.twilio.chat.demo.ChannelModel
import com.twilio.chat.Channel.ChannelStatus
import com.twilio.chat.Channel.ChannelType
import com.twilio.chat.Channel.NotificationLevel
import com.twilio.chat.CallbackListener
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import com.twilio.chat.Message
import com.twilio.chat.demo.R
import eu.inloop.simplerecycleradapter.SettableViewHolder
import kotterknife.bindView
import org.jetbrains.anko.*

class ChannelViewHolder : SettableViewHolder<ChannelModel>, AnkoLogger {
    val friendlyName: TextView by bindView(R.id.channel_friendly_name)
    val channelSid: TextView by bindView(R.id.channel_sid)
    val updatedDate: TextView by bindView(R.id.channel_updated_date)
    val createdDate: TextView by bindView(R.id.channel_created_date)
    val usersCount: TextView by bindView(R.id.channel_users_count)
    val totalMessagesCount: TextView by bindView(R.id.channel_total_messages_count)
    val unconsumedMessagesCount: TextView by bindView(R.id.channel_unconsumed_messages_count)
    val lastMessageDate: TextView by bindView(R.id.channel_last_message_date)
    val pushesLevel: TextView by bindView(R.id.channel_pushes_level)

    constructor(context: Context, parent: ViewGroup)
        : super(context, R.layout.channel_item_layout, parent)
    {}

    override fun setData(channel: ChannelModel) {
        warn { "setData for ${channel.friendlyName} sid|${channel.sid}|" }
        friendlyName.text = channel.friendlyName
        channelSid.text = channel.sid

        updatedDate.text = if (channel.dateUpdatedAsDate != null)
            channel.dateUpdatedAsDate!!.toString()
        else
            "<no updated date>"

        createdDate.text = if (channel.dateCreatedAsDate != null)
            channel.dateCreatedAsDate!!.toString()
        else
            "<no created date>"

        pushesLevel.text = if (channel.notificationLevel == NotificationLevel.MUTED)
            "Pushes: Muted"
        else
            "Pushes: Default"

        channel.getUnconsumedMessagesCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getUnconsumedMessagesCount callback")
                unconsumedMessagesCount.text = "Unread " + (value?.toString() ?: 0)
            }
        })

        channel.getMessagesCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMessagesCount callback")
                totalMessagesCount.text = "Messages " + (value?.toString() ?: 0)
            }
        })

        channel.getMembersCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMembersCount callback")
                usersCount.text = "Members " + (value?.toString() ?: 0)
            }
        })

        val lastmsg = channel.lastMessageDate;
        if (lastmsg != null) {
            lastMessageDate.text = lastmsg.toString()
        }

        itemView.setBackgroundColor(
            if (channel.status == ChannelStatus.JOINED) {
                if (channel.type == ChannelType.PRIVATE)
                    Color.BLUE
                else
                    Color.WHITE
            } else {
                if (channel.status == ChannelStatus.INVITED)
                    Color.YELLOW
                else
                    Color.GRAY
            }
        )
    }
}
