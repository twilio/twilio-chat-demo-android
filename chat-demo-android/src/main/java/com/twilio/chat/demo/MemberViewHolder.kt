package com.twilio.chat.demo

import com.twilio.chat.demo.R
import com.twilio.chat.Member

import android.view.View
import android.widget.TextView
import butterknife.BindView
import uk.co.ribot.easyadapter.ItemViewHolder
import uk.co.ribot.easyadapter.PositionInfo
import uk.co.ribot.easyadapter.annotations.LayoutId
import uk.co.ribot.easyadapter.annotations.ViewId

@LayoutId(R.layout.member_item_layout)
class MemberViewHolder(internal var view: View) : ItemViewHolder<Member>(view) {
    internal @BindView(R.id.identity) lateinit var memberIdentity: TextView
    internal @BindView(R.id.member_sid) lateinit var memberSid: TextView

    override fun onSetListeners() {
        view.setOnClickListener {
            val listener = getListener(OnMemberClickListener::class.java)
            listener?.onMemberClicked(item)
        }
    }

    interface OnMemberClickListener {
        fun onMemberClicked(member: Member)
    }

    override fun onSetValues(member: Member, arg1: PositionInfo) {
        memberIdentity.text = member.identity
    }
}
