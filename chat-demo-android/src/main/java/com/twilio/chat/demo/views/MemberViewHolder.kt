package com.twilio.chat.demo.views

import com.twilio.chat.demo.R
import com.twilio.chat.Member

import android.view.View
import android.widget.TextView
import kotterknife.bindView
import eu.inloop.simplerecycleradapter.SettableViewHolder

//@LayoutId(R.layout.member_item_layout)
class MemberViewHolder(internal var view: View) : SettableViewHolder<Member>(view) {
    val memberIdentity: TextView by bindView(R.id.identity)
    val memberSid: TextView by bindView(R.id.member_sid)

//    override fun onSetListeners() {
//        view.setOnClickListener {
//            val listener = getListener(OnMemberClickListener::class.java)
//            listener?.onMemberClicked(item)
//        }
//    }

    override fun setData(member: Member) {
        memberIdentity.text = member.identity
    }
}
