package com.twilio.conversations.demo.views

import android.content.Context
import com.twilio.chat.demo.R
import com.twilio.chat.Member
import android.view.ViewGroup
import android.widget.TextView
import kotterknife.bindView
import eu.inloop.simplerecycleradapter.SettableViewHolder

class MemberViewHolder : SettableViewHolder<Member> {
    private val memberIdentity: TextView by bindView(R.id.identity)
//    private val memberSid: TextView by bindView(R.id.member_sid)

    constructor(context: Context, parent: ViewGroup)
        : super(context, R.layout.member_item_layout, parent)
    {}

    override fun setData(member: Member) {
        memberIdentity.text = member.identity
//        memberSid.text = member.sid
    }
}
