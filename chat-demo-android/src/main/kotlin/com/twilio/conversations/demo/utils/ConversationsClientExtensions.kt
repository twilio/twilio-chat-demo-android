package com.twilio.conversations.demo.utils

import com.twilio.conversations.ConversationsClient

/** Extension function to simulate native crash */
fun ConversationsClient.simulateCrash(where: Where) {
    val method = ConversationsClient::class.java.getDeclaredMethod("simulateCrash", Int::class.java)
    method.isAccessible = true
    method.invoke(this, where.value)
}

enum class Where(val value: Int) {
    CHAT_CLIENT_CPP(1),
    TM_CLIENT_CPP(2)
}
