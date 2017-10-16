package com.twilio.chat.demo.services

import ChatCallbackListener
import ChatStatusListener
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.twilio.chat.Channel
import com.twilio.chat.Message
import com.twilio.chat.ProgressListener
import com.twilio.chat.demo.TwilioApplication
import com.twilio.chat.demo.models.Media
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.jetbrains.anko.*

class MediaService : IntentService(MediaService::class.java.simpleName), AnkoLogger {

    companion object {
        val EXTRA_MEDIA_URI = "com.twilio.demo.chat.media_uri"
        val EXTRA_CHANNEL = "com.twilio.demo.chat.media_channel"
        val EXTRA_MESSAGE_INDEX = "com.twilio.demo.chat.message_index"

        val EXTRA_ACTION = "com.twilio.demo.chat.media.action"
        val EXTRA_ACTION_UPLOAD = "com.twilio.demo.chat.media.action_upload"
        val EXTRA_ACTION_DOWNLOAD = "com.twilio.demo.chat.media.action_download"
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
        val channel = intent.getParcelableExtra<Channel>(EXTRA_CHANNEL) ?: throw NullPointerException("Channel is not provided")

        launch(coroutineContext) {
            val deferred = CompletableDeferred<Unit>()

            val uri = Uri.parse(uriString)
            val cursor = contentResolver.query(uri, null, null, null, null)

            try {
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    val type = contentResolver.getType(uri)
                    val stream = contentResolver.openInputStream(uri)

                    val media = Media(name, type, stream)

                    val options = Message.options()
                            .withMediaFileName(media.name)
                            .withMedia(media.stream, media.type)
                            .withMediaProgressListener(object : ProgressListener() {
                                override fun onStarted() = debug { "Start media upload" }
                                override fun onProgress(bytes: Long) = debug { "Media upload progress - bytes done: ${bytes}" }
                                override fun onCompleted(mediaSid: String) = debug { "Media upload completed" }
                            })

                    TwilioApplication.instance.basicClient.chatClient?.channels?.getChannel(channel.sid, ChatCallbackListener<Channel> {
                        it.messages.sendMessage(options, ChatCallbackListener<Message> {
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
        val channel = intent.getParcelableExtra<Channel>(EXTRA_CHANNEL) ?: throw NullPointerException("Channel is not provided")
        val messageIndex = intent.getLongExtra(EXTRA_MESSAGE_INDEX, -1L)

        launch(coroutineContext) {
            val deferred = CompletableDeferred<String>()

            channel.messages.getMessageByIndex(messageIndex, ChatCallbackListener<Message> { message ->
                val media = message.media ?: return@ChatCallbackListener

                debug { "Media received - sid: ${media.sid}, name: ${media.fileName}, type: ${media.type}, size: ${media.size}" }

                try {
                    val outStream = openFileOutput(media.sid, Context.MODE_PRIVATE)

                    media.download(outStream, ChatStatusListener { debug { "Download completed" } }, object : ProgressListener() {
                        override fun onStarted() = debug { "Start media download" }
                        override fun onProgress(bytes: Long) = debug { "Media download progress - bytes done: ${bytes}" }
                        override fun onCompleted(mediaSid: String) {
                            debug { "Media download completed" }
                            deferred.complete(mediaSid)
                        }
                    })

                } catch (e: Exception) {
                    error { "Failed to download media - error: ${e.message}" }
                    deferred.completeExceptionally(e)
                }
            })

            deferred.await()
        }
    }
}
