package com.twilio.conversations.demo.views

import android.content.Context
import com.twilio.conversations.demo.ConversationModel
import com.twilio.conversations.Conversation.ConversationStatus
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.CallbackListener
import com.twilio.conversations.Message
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.widget.TextView
import com.twilio.conversations.demo.R
import eu.inloop.simplerecycleradapter.SettableViewHolder
import kotterknife.bindView
import org.jetbrains.anko.*

class ChannelViewHolder : SettableViewHolder<ConversationModel>, AnkoLogger {
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

    override fun setData(conversation: ConversationModel) {
        warn { "setData for ${conversation.friendlyName} sid|${conversation.sid}|" }
        friendlyName.text = conversation.friendlyName
        channelSid.text = conversation.sid

        updatedDate.text = if (conversation.dateUpdatedAsDate != null)
            conversation.dateUpdatedAsDate!!.toString()
        else
            "<no updated date>"

        createdDate.text = if (conversation.dateCreatedAsDate != null)
            conversation.dateCreatedAsDate!!.toString()
        else
            "<no created date>"

        pushesLevel.text = if (conversation.notificationLevel == NotificationLevel.MUTED)
            "Pushes: Muted"
        else
            "Pushes: Default"

        conversation.getUnconsumedMessagesCount(object : CallbackListener<Long> {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getUnconsumedMessagesCount callback")
                unconsumedMessagesCount.text = "Unread " + value!!.toString()
            }
        })

        conversation.getMessagesCount(object : CallbackListener<Long> {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMessagesCount callback")
                totalMessagesCount.text = "Messages " + value!!.toString()
            }
        })

        conversation.getParticipantsCount(object : CallbackListener<Long> {
            override fun onSuccess(value: Long?) {
                Log.d("ChannelViewHolder", "getMembersCount callback")
                usersCount.text = "Members " + value!!.toString()
            }
        })

        val lastmsg = conversation.lastMessageDate;
        if (lastmsg != null) {
            lastMessageDate.text = lastmsg.toString()
        }

        itemView.setBackgroundColor(
            if (conversation.status == ConversationStatus.JOINED) {
                Color.WHITE
            } else {
                Color.GRAY
            }
        )
    }
}
