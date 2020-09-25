package com.twilio.conversations.demo.models

import java.io.InputStream

data class Media(val name: String, val type: String, val stream: InputStream)
