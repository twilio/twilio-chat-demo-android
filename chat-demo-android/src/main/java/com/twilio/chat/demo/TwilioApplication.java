package com.twilio.chat.demo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.twilio.chat.ErrorInfo;

public class TwilioApplication extends Application
{
    private static TwilioApplication instance;
    private BasicChatClient   basicClient;

    public static TwilioApplication get()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        TwilioApplication.instance = this;
        basicClient = new BasicChatClient(getApplicationContext());
    }

    public BasicChatClient getBasicClient()
    {
        return this.basicClient;
    }

    public void showToast(final String text) {
        showToast(text, Toast.LENGTH_SHORT);
    }

    public void showToast(final String text, final int duration)
    {
        Log.d("TwilioApplication", text);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();
            }
        });
    }

    public void showError(final ErrorInfo error)
    {
        showError("Something went wrong", error);
    }

    public void showError(final String message, final ErrorInfo error)
    {
        showToast(formatted(message, error), Toast.LENGTH_LONG);
        logErrorInfo(message, error);
    }

    public void logErrorInfo(final String message, final ErrorInfo error)
    {
        Log.e("TwilioApplication", formatted(message, error));
    }

    private String formatted(String message, ErrorInfo error)
    {
        return String.format("%s. Error code: %s, text: %s",
                message,
                error.getErrorCode(),
                error.getErrorText());
    }
}
