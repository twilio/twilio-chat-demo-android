package com.twilio.chat.demo.utils

import com.twilio.chat.ChatClient

/** Extension function to simulate native crash */
fun ChatClient.simulateCrash(where: Where) {
    val method = ChatClient::class.java.getDeclaredMethod("simulateCrash", Int::class.java)
    method.isAccessible = true
    method.invoke(this, where.value)
}

enum class Where(val value: Int) {
    CHAT_CLIENT_CPP(1),
    TM_CLIENT_CPP(2)
}
