package com.twilio.conversations.demo

interface Constants {
    companion object {
        /** Key into an Intent's extras data that points to a [Channel] object.  */
        val EXTRA_CHANNEL = "com.twilio.chat.Channel"
        /** Key into an Intent's extras data that contains Channel SID.  */
        val EXTRA_CHANNEL_SID = "C_SID"
    }
}
