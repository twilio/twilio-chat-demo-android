package com.twilio.conversations.demo.services

import ChatCallbackListener
import ChatStatusListener
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.twilio.conversations.CallbackListener
import com.twilio.conversations.Conversation
import com.twilio.conversations.Message
import com.twilio.conversations.ProgressListener
import com.twilio.conversations.demo.TwilioApplication
import com.twilio.conversations.demo.models.Media
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import org.jetbrains.anko.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.CancellationException

class MediaService : IntentService(MediaService::class.java.simpleName), AnkoLogger {

    companion object {
        val EXTRA_MEDIA_URI = "com.twilio.demo.conversations.media_uri"
        val EXTRA_CHANNEL_SID = "com.twilio.demo.conversations.media_channel"
        val EXTRA_MESSAGE_INDEX = "com.twilio.demo.conversations.message_index"

        val EXTRA_ACTION = "com.twilio.demo.conversations.media.action"
        val EXTRA_ACTION_UPLOAD = "com.twilio.demo.conversations.media.action_upload"
        val EXTRA_ACTION_DOWNLOAD = "com.twilio.demo.conversations.media.action_download"
    }

    private val coroutineContext = newSingleThreadContext(MediaService::class.java.simpleName)

    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.getStringExtra(EXTRA_ACTION)

        when (action) {
            EXTRA_ACTION_UPLOAD -> upload(intent)
            EXTRA_ACTION_DOWNLOAD -> download(intent)
        }
    }

    private fun upload(intent: Intent) {
        val uriString = intent.getStringExtra(EXTRA_MEDIA_URI) ?: throw NullPointerException("Media URI not provided")
        val channelSid = intent.getStringExtra(EXTRA_CHANNEL_SID) ?: throw NullPointerException("Channel is not provided")

        GlobalScope.launch(coroutineContext) {
            val deferred = CompletableDeferred<Unit>()

            val uri = Uri.parse(uriString)
            val cursor = contentResolver.query(uri, null, null, null, null)!!

            try {
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val type = contentResolver.getType(uri)!!
                    val stream = contentResolver.openInputStream(uri)!!

                    val media = Media(name, type, stream)

                    val options = Message.options()
                            .withMediaFileName(media.name)
                            .withMedia(media.stream, media.type)
                            .withMediaProgressListener(object : ProgressListener {
                                override fun onStarted() = debug { "Start media upload" }
                                override fun onProgress(bytes: Long) = debug { "Media upload progress - bytes done: ${bytes}" }
                                override fun onCompleted(mediaSid: String) = debug { "Media upload completed" }
                            })

                    TwilioApplication.instance.basicClient.conversationsClient?.getConversation(channelSid, ChatCallbackListener<Conversation> {
                        it.sendMessage(options, ChatCallbackListener<Message> {
                            debug { "Media message sent - sid: ${it.sid}, type: ${it.type}" }
                            deferred.complete(Unit)
                        })
                    })

                }
            } catch (e: Exception) {
                error { "Failed to upload media -> error: ${e.message}" }
                deferred.completeExceptionally(e)
            } finally {
                cursor.close()
            }

            deferred.await()
        }
    }

    private fun download(intent: Intent) {
        val channelSid = intent.getStringExtra(EXTRA_CHANNEL_SID) ?: throw NullPointerException("Channel is not provided")
        val messageIndex = intent.getLongExtra(EXTRA_MESSAGE_INDEX, -1L)

        GlobalScope.launch(coroutineContext) {
            val deferred = CompletableDeferred<String>()

            TwilioApplication.instance.basicClient.conversationsClient?.getConversation(channelSid, ChatCallbackListener<Conversation> {
                it.getMessageByIndex(messageIndex, ChatCallbackListener<Message> { message ->

                    debug { "Media received - sid: ${message.mediaSid}, name: ${message.mediaFileName}, type: ${message.mediaType}, size: ${message.mediaSize}" }

                    try {
                        val outStream = FileOutputStream(File(cacheDir, message.mediaSid))
                        message.getMediaContentTemporaryUrl { tempUrl ->
                            val inStream = BufferedInputStream(URL(tempUrl).openStream())

                            inStream.copyTo(outStream)
                            debug { "Media download completed" }
                            deferred.complete(message.mediaSid)
                        }
                    } catch (e: Exception) {
                        error { "Failed to download media - error: ${e.message}" }
                        deferred.cancel(CancellationException(e.message))
                    }
                })
            })

            deferred.await()
        }
    }
}
