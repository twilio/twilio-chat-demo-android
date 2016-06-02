package com.twilio.ipmessaging.demo;

import android.app.Application;

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
}
