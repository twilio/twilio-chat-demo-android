package com.twilio.conversations.demo.views

import android.content.Context
import com.twilio.conversations.demo.R
import com.twilio.conversations.Participant
import android.view.ViewGroup
import android.widget.TextView
import kotterknife.bindView
import eu.inloop.simplerecycleradapter.SettableViewHolder

class MemberViewHolder : SettableViewHolder<Participant> {
    private val memberIdentity: TextView by bindView(R.id.identity)
//    private val memberSid: TextView by bindView(R.id.member_sid)

    constructor(context: Context, parent: ViewGroup)
        : super(context, R.layout.member_item_layout, parent)
    {}

    override fun setData(member: Participant) {
        memberIdentity.text = member.identity
    }
}
