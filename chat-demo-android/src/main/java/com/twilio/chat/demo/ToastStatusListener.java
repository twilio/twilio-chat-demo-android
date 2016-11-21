package com.twilio.chat.demo;

import com.twilio.chat.ErrorInfo;
import com.twilio.chat.StatusListener;

class ToastStatusListener extends StatusListener
{
    private final String okText;
    private final String errorText;

    ToastStatusListener(String ok, String error) {
        okText = ok;
        errorText = error;
    }

    @Override
    public void onSuccess()
    {
        TwilioApplication.get().showToast(okText);
    }

    @Override
    public void onError(ErrorInfo errorInfo)
    {
        TwilioApplication.get().showError(errorText, errorInfo);
    }
}
