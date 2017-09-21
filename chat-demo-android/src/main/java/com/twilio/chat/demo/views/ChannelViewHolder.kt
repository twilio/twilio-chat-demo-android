package com.twilio.chat.demo.views

import android.content.Context
import com.twilio.chat.demo.ChannelModel
import com.twilio.chat.Channel.ChannelStatus
import com.twilio.chat.Channel.ChannelType
import com.twilio.chat.CallbackListener

import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import butterknife.bindView
import com.twilio.chat.demo.R
import eu.inloop.simplerecycleradapter.SettableViewHolder
import timber.log.Timber

class ChannelViewHolder : SettableViewHolder<ChannelModel> {
    val friendlyName: TextView by bindView(R.id.channel_friendly_name)
    val channelSid: TextView by bindView(R.id.channel_sid)
    val updatedDate: TextView by bindView(R.id.channel_updated_date)
    val createdDate: TextView by bindView(R.id.channel_created_date)
    val usersCount: TextView by bindView(R.id.channel_users_count)
    val totalMessagesCount: TextView by bindView(R.id.channel_total_messages_count)
    val unconsumedMessagesCount: TextView by bindView(R.id.channel_unconsumed_messages_count)

    constructor(context: Context, parent: ViewGroup)
        : super(context, R.layout.channel_item_layout, parent)
    {}

    override fun setData(channel: ChannelModel) {
        Timber.w("setData for ${channel.friendlyName} sid|${channel.sid}|")
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

        channel.getUnconsumedMessagesCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getUnconsumedMessagesCount callback")
                unconsumedMessagesCount.text = "Unread " + value!!.toString()
            }
        })

        channel.getMessagesCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMessagesCount callback")
                totalMessagesCount.text = "Messages " + value!!.toString()
            }
        })

        channel.getMembersCount(object : CallbackListener<Long>() {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMembersCount callback")
                usersCount.text = "Members " + value!!.toString()
            }
        })

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
