import com.twilio.chat.CallbackListener
import com.twilio.chat.ErrorInfo
import com.twilio.chat.StatusListener
import com.twilio.chat.demo.TwilioApplication

typealias SuccessStatus = () -> Unit
typealias SuccessCallback<T> = (T) -> Unit
typealias ErrorCallback = (error: ErrorInfo) -> Unit

class ChatCallbackListener<T>(val fail: ErrorCallback = {},
                              val success: SuccessCallback<T> = {}) : CallbackListener<T>() {

    override fun onSuccess(p0: T) = success(p0)

    override fun onError(err: ErrorInfo) {
        TwilioApplication.instance.showError(err)
        fail(err)
    }
}

class ChatStatusListener(val fail: ErrorCallback = {},
                         val success: SuccessStatus = {}) : StatusListener() {

    override fun onSuccess() = success()

    override fun onError(err: ErrorInfo) {
        TwilioApplication.instance.showError(err)
        fail(err)
    }
}
