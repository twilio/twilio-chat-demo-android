package com.twilio.chat.demo

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast
import com.twilio.chat.ErrorInfo
import timber.log.Timber
import timber.log.Timber.DebugTree

class TwilioApplication : Application() {
    var basicClient: BasicChatClient? = null
        private set

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }

        super.onCreate()
        TwilioApplication.instance = this
        basicClient = BasicChatClient(applicationContext)
    }

    @JvmOverloads fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
        Timber.d(text)
        Handler(Looper.getMainLooper()).post {
            val toast = Toast.makeText(applicationContext, text, duration)
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
        }
    }

    fun showError(error: ErrorInfo) {
        showError("Something went wrong", error)
    }

    fun showError(message: String, error: ErrorInfo) {
        showToast(formatted(message, error), Toast.LENGTH_LONG)
        logErrorInfo(message, error)
    }

    fun logErrorInfo(message: String, error: ErrorInfo) {
        Timber.e(formatted(message, error))
    }

    private fun formatted(message: String, error: ErrorInfo): String {
        return String.format("%s. %s", message, error.toString())
    }

    companion object {
        private var instance: TwilioApplication? = null

        fun get(): TwilioApplication {
            return instance!!
        }
    }
}
