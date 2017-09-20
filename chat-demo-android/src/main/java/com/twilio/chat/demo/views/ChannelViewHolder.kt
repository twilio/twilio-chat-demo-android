package com.twilio.chat.demo.views

import com.twilio.chat.demo.ChannelModel
import com.twilio.chat.Channel.ChannelStatus
import com.twilio.chat.Channel.ChannelType
import com.twilio.chat.CallbackListener

import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.TextView
import com.twilio.chat.demo.R
import uk.co.ribot.easyadapter.ItemViewHolder
import uk.co.ribot.easyadapter.PositionInfo
import uk.co.ribot.easyadapter.annotations.LayoutId
import uk.co.ribot.easyadapter.annotations.ViewId
import kotlinx.android.synthetic.main.channel_item_layout.*

@LayoutId(R.layout.channel_item_layout)
class ChannelViewHolder(internal var view: View) : ItemViewHolder<ChannelModel>(view) {
    val friendlyName: TextView by bindView(R.id.channel_friendly_name)
//    internal @BindView(R.id.channel_friendly_name) lateinit var
//    internal @BindView(R.id.channel_sid) lateinit var channelSid: TextView
//    internal @BindView(R.id.channel_updated_date) lateinit var updatedDate: TextView
//    internal @BindView(R.id.channel_created_date) lateinit var createdDate: TextView
//    internal @BindView(R.id.channel_users_count) lateinit var usersCount: TextView
//    internal @BindView(R.id.channel_total_messages_count) lateinit var totalMessagesCount: TextView
//    internal @BindView(R.id.channel_unconsumed_messages_count) lateinit var unconsumedMessagesCount: TextView

    override fun onSetListeners() {
        view.setOnClickListener {
            val listener = getListener(OnChannelClickListener::class.java)
            listener?.onChannelClicked(item)
        }
    }

    override fun onSetValues(channel: ChannelModel, arg1: PositionInfo) {
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

        view.setBackgroundColor(
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

    interface OnChannelClickListener {
        fun onChannelClicked(channel: ChannelModel)
    }
}
