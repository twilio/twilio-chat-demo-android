package com.twilio.conversations.demo

import com.twilio.conversations.*
import java.util.Date
import com.twilio.conversations.Conversation.ConversationStatus
import com.twilio.conversations.Conversation.NotificationLevel

class ConversationModel {
    private var convo: Conversation? = null

    constructor(convo_: Conversation) {
        convo = convo_
    }

    val friendlyName: String
        get() {
            if (convo != null) return convo!!.friendlyName
            throw IllegalStateException("Invalid state")
        }

    val sid: String
        get() {
            if (convo != null) return convo!!.sid
            throw IllegalStateException("Invalid state")
        }

    val dateUpdatedAsDate: Date?
        get() {
            if (convo != null) return convo!!.dateUpdatedAsDate
            throw IllegalStateException("Invalid state")
        }

    val dateCreatedAsDate: Date?
        get() {
            if (convo != null) return convo!!.dateCreatedAsDate
            throw IllegalStateException("Invalid state")
        }

    val status: ConversationStatus
        get() {
            if (convo != null) return convo!!.status
            throw IllegalStateException("Invalid state")
        }

    val lastMessageDate: Date?
        get() {
            if (convo != null) return convo!!.lastMessageDate
            throw IllegalStateException("Invalid state")
        }

    val notificationLevel: NotificationLevel
        get() {
            if (convo != null) return convo!!.notificationLevel
            throw IllegalStateException("Invalid state")
        }

    val lastMessageIndex: Long?
        get() {
            if (convo != null) return convo!!.lastMessageIndex
            throw IllegalStateException("Invalid state")
        }

    fun getUnconsumedMessagesCount(listener: CallbackListener<Long>) {
        if (convo != null) {
            convo!!.getUnreadMessagesCount(listener)
            return
        }
        listener.onError(ErrorInfo(-10001, "No channel in model"))
    }

    fun getMessagesCount(listener: CallbackListener<Long>) {
        if (convo != null) {
            convo!!.getMessagesCount(listener)
            return
        }
        listener.onError(ErrorInfo(-10002, "No channel in model"))
    }

    fun getParticipantsCount(listener: CallbackListener<Long>) {
        if (convo != null) {
            convo!!.getParticipantsCount(listener)
            return
        }
        listener.onError(ErrorInfo(-10003, "No channel in model"))
    }

    fun join(listener: StatusListener) {
        if (convo != null) {
            convo!!.join(listener)
            return
        }
        listener.onError(ErrorInfo(-10004, "No channel in model"))
    }

    fun getChannel(listener: CallbackListener<Conversation>) {
        if (convo != null) {
            listener.onSuccess(convo)
            return
        }
        listener.onError(ErrorInfo(-10005, "No channel in model"))
    }
}
