package com.twilio.ipmessaging.demo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.twilio.ipmessaging.ErrorInfo;

public class TwilioApplication extends Application
{
    private static TwilioApplication instance;
    private BasicIPMessagingClient   basicClient;

    public static TwilioApplication get()
    {
        return instance;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        TwilioApplication.instance = this;
        basicClient = new BasicIPMessagingClient(getApplicationContext());
    }

    public BasicIPMessagingClient getBasicClient()
    {
        return this.basicClient;
    }

    public void showError(final ErrorInfo error)
    {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run()
            {
                Toast
                    .makeText(getApplicationContext(),
                              String.format("Something went wrong. Error code: %s, text: %s",
                                            error.getErrorCode(),
                                            error.getErrorText()),
                              Toast.LENGTH_LONG)
                    .show();
            }
        });
    }

    public void logErrorInfo(final String message, final ErrorInfo error)
    {
        Log.e("TwilioApplication",
              String.format("%s. Error code: %s, text: %s",
                            message,
                            error.getErrorCode(),
                            error.getErrorText()));
    }
}
