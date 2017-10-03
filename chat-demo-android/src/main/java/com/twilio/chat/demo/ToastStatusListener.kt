package com.twilio.chat.demo

import com.twilio.chat.ErrorInfo
import com.twilio.chat.StatusListener

/**
 * Status listener that shows a toast with operation results.
 */
internal open class ToastStatusListener(private val okText: String, private val errorText: String) : StatusListener() {

    override fun onSuccess() {
        TwilioApplication.instance.showToast(okText)
    }

    override fun onError(errorInfo: ErrorInfo?) {
        TwilioApplication.instance.showError(errorText, errorInfo!!)
    }
}
