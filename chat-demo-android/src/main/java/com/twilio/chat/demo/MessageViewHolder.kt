package com.twilio.chat.demo

import org.json.JSONObject

import com.twilio.chat.CallbackListener
import com.twilio.chat.Member
import com.twilio.chat.Message
import com.twilio.chat.Paginator
import com.twilio.chat.User
import com.twilio.chat.demo.R

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle

import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import butterknife.BindView

import uk.co.ribot.easyadapter.ItemViewHolder
import uk.co.ribot.easyadapter.PositionInfo
import uk.co.ribot.easyadapter.annotations.LayoutId
import uk.co.ribot.easyadapter.annotations.ViewId

@LayoutId(R.layout.message_item_layout)
class MessageViewHolder(internal var view: View) : ItemViewHolder<MessageActivity.MessageItem>(view) {

    internal @BindView(R.id.avatar)       lateinit var imageView: ImageView
    internal @BindView(R.id.reachability) lateinit var reachabilityView: ImageView
    internal @BindView(R.id.body)         lateinit var body: TextView
    internal @BindView(R.id.author)       lateinit var author: TextView
    internal @BindView(R.id.date)         lateinit var date: TextView
    internal @BindView(R.id.consumptionHorizonIdentities) lateinit var identities: RelativeLayout
    internal @BindView(R.id.consumptionHorizonLines)      lateinit var lines: LinearLayout

    override fun onSetListeners() {
        view.setOnLongClickListener(View.OnLongClickListener {
            val listener = getListener(OnMessageClickListener::class.java)
            if (listener != null) {
                listener.onMessageClicked(item)
                return@OnLongClickListener true
            }
            false
        })

        view.setOnClickListener { date.visibility = if (date.visibility == View.GONE) View.VISIBLE else View.GONE }
    }

    override fun onSetValues(message: MessageActivity.MessageItem?, pos: PositionInfo) {
        if (message != null) {
            val msg = message.message

            author.text = msg.author
            body.text = msg.messageBody
            date.text = msg.timeStamp

            identities.removeAllViews()
            lines.removeAllViews()

            for (member in message.members.membersList) {
                if (msg.author.contentEquals(member.identity)) {
                    fillUserAvatar(imageView, member)
                    fillUserReachability(reachabilityView, member)
                }

                if (member.lastConsumedMessageIndex != null && member.lastConsumedMessageIndex == message.message.messageIndex) {
                    drawConsumptionHorizon(member)
                }
            }
        }
    }

    private fun drawConsumptionHorizon(member: Member) {
        val ident = member.identity
        val color = getMemberRgb(ident)

        val identity = TextView(context)
        identity.text = ident
        identity.textSize = 8f
        identity.setTextColor(color)

        // Layout
        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        val cc = identities.childCount
        if (cc > 0) {
            params.addRule(RelativeLayout.RIGHT_OF, identities.getChildAt(cc - 1).id)
        }
        identity.layoutParams = params

        val line = View(context)
        line.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5)
        line.setBackgroundColor(color)

        identities.addView(identity)
        lines.addView(line)
    }

    private fun fillUserAvatar(avatarView: ImageView, member: Member) {
        TwilioApplication.instance.basicClient.chatClient?.users?.getAndSubscribeUser(member.identity, object : CallbackListener<User>() {
            override fun onSuccess(user: User) {
                val attributes = user.attributes
                val avatar = attributes.opt("avatar") as String?
                if (avatar != null) {
                    val data = Base64.decode(avatar, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    avatarView.setImageBitmap(bitmap)
                } else {
                    avatarView.setImageResource(R.drawable.avatar2)
                }
            }
        })
    }

    private fun fillUserReachability(reachabilityView: ImageView, member: Member) {
        if (!TwilioApplication.instance.basicClient.chatClient?.isReachabilityEnabled!!) {
            reachabilityView.setImageResource(R.drawable.reachability_disabled)
        } else {
            member.getAndSubscribeUser(object : CallbackListener<User>() {
                override fun onSuccess(user: User) {
                    if (user.isOnline) {
                        reachabilityView.setImageResource(R.drawable.reachability_online)
                    } else if (user.isNotifiable) {
                        reachabilityView.setImageResource(R.drawable.reachability_notifiable)
                    } else {
                        reachabilityView.setImageResource(R.drawable.reachability_offline)
                    }
                }
            })
        }
    }

    interface OnMessageClickListener {
        fun onMessageClicked(message: MessageActivity.MessageItem)
    }

    fun getMemberRgb(identity: String): Int {
        return HORIZON_COLORS[Math.abs(identity.hashCode()) % HORIZON_COLORS.size]
    }

    companion object {
        private val HORIZON_COLORS = intArrayOf(Color.GRAY, Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA)
    }
}
