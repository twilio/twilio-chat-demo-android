package com.twilio.ipmessaging.demo;

import java.util.Arrays;
import java.util.List;

import com.twilio.common.TwilioAccessManager;
import com.twilio.common.TwilioAccessManagerFactory;
import com.twilio.common.TwilioAccessManagerListener;
import com.twilio.ipmessaging.Channel;
import com.twilio.ipmessaging.Constants.InitListener;
import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.IPMessagingClientListener;
import com.twilio.ipmessaging.TwilioIPMessagingClient;
import com.twilio.ipmessaging.TwilioIPMessagingSDK;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class BasicIPMessagingClient
    implements IPMessagingClientListener, TwilioAccessManagerListener
{
    private static final Logger logger = Logger.getLogger(BasicIPMessagingClient.class);
    private static final String     TAG = "BasicIPMessagingClient";
    private String                  capabilityToken;
    private String                  gcmToken;
    private long                    nativeClientParam;
    private TwilioIPMessagingClient ipMessagingClient;

    private Channel[] channels;
    private Context             context;
    private TwilioAccessManager acessMgr;
    private Handler             loginListenerHandler;
    private String              urlString;

    public BasicIPMessagingClient(Context context)
    {
        super();
        this.context = context;
    }

    public interface LoginListener {
        public void onLoginStarted();

        public void onLoginFinished();

        public void onLoginError(String errorMessage);

        public void onLogoutFinished();
    }

    public String getCapabilityToken()
    {
        return capabilityToken;
    }
    public void setCapabilityToken(String capabilityToken)
    {
        this.capabilityToken = capabilityToken;
    }

    public String getGCMToken()
    {
        return gcmToken;
    }
    public void setGCMToken(String gcmToken)
    {
        this.gcmToken = gcmToken;
    }
    public void doLogin(final String capabilityToken, final LoginListener listener, String url)
    {
        this.urlString = url;
        this.loginListenerHandler = setupListenerHandler();
        TwilioIPMessagingSDK.setLogLevel(android.util.Log.DEBUG);
        if (!TwilioIPMessagingSDK.isInitialized()) {
            TwilioIPMessagingSDK.initializeSDK(context, new InitListener() {
                @Override
                public void onInitialized()
                {
                    createClientWithAccessManager(listener);
                }
                @Override
                public void onError(Exception error)
                {
                    logger.e("Error initializing the SDK :" + error.getMessage());
                }
            });
        } else {
            this.createClientWithAccessManager(listener);
        }
    }

    public BasicIPMessagingClient()
    {
        super();
    }
    public List<Channel> getChannelList()
    {
        List<Channel> list = Arrays.asList(this.channels);
        return list;
    }

    public long getNativeClientParam()
    {
        return nativeClientParam;
    }

    public void setNativeClientParam(long nativeClientParam)
    {
        this.nativeClientParam = nativeClientParam;
    }

    @Override
    public void onChannelAdd(Channel channel)
    {
        if (channel != null) {
            logger.d("A Channel :" + channel.getFriendlyName() + " got added");
        } else {
            logger.d("Received onChannelAdd event.");
        }
    }

    @Override
    public void onChannelChange(Channel channel)
    {
        if (channel != null) {
            logger.d("Channel Name : " + channel.getFriendlyName() + " got Changed");
        } else {
            logger.d("received onChannelChange event.");
        }
    }

    @Override
    public void onChannelDelete(Channel channel)
    {
        if (channel != null) {
            logger.d("A Channel :" + channel.getFriendlyName() + " got deleted");
        } else {
            logger.d("received onChannelDelete event.");
        }
    }

    @Override
    public void onError(int errorCode, String errorText)
    {
        logger.d("Received onError event.");
    }

    @Override
    public void onAttributesChange(String attributes)
    {
        logger.d("Received onAttributesChange event.");
    }

    public TwilioIPMessagingClient getIpMessagingClient()
    {
        return ipMessagingClient;
    }
    private void createClientWithAccessManager(final LoginListener listener)
    {
        this.acessMgr = TwilioAccessManagerFactory.createAccessManager(
            this.capabilityToken, new TwilioAccessManagerListener() {
                @Override
                public void onAccessManagerTokenExpire(TwilioAccessManager twilioAccessManager)
                {
                    Log.d(TAG, "token expired.");
                    new GetCapabilityTokenAsyncTask().execute(
                        BasicIPMessagingClient.this.urlString);
                }

                @Override
                public void onTokenUpdated(TwilioAccessManager twilioAccessManager)
                {
                    Log.d(TAG, "token updated.");
                }

                @Override
                public void onError(TwilioAccessManager twilioAccessManager, String s)
                {
                    Log.d(TAG, "token error: " + s);
                }
            });

        ipMessagingClient = TwilioIPMessagingSDK.createIPMessagingClientWithAccessManager(
            BasicIPMessagingClient.this.acessMgr, BasicIPMessagingClient.this);
        if (ipMessagingClient != null) {
            ipMessagingClient.setListener(BasicIPMessagingClient.this);
            Intent        intent = new Intent(context, ChannelActivity.class);
            PendingIntent pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ipMessagingClient.setIncomingIntent(pendingIntent);
            loginListenerHandler.post(new Runnable() {
                @Override
                public void run()
                {
                    if (listener != null) {
                        listener.onLoginFinished();
                    }
                }
            });
        } else {
            listener.onLoginError("ipMessagingClientWithAccessManager is null");
        }
    }

    @Override
    public void onChannelHistoryLoaded(Channel channel)
    {
        logger.d("Received onChannelHistoryLoaded callback " + channel.getFriendlyName());
    }

    @Override
    public void onAccessManagerTokenExpire(TwilioAccessManager arg0)
    {
        logger.d("Received AccessManager:onAccessManagerTokenExpire.");
    }

    @Override
    public void onError(TwilioAccessManager arg0, String arg1)
    {
        logger.d("Received AccessManager:onError.");
    }

    @Override
    public void onTokenUpdated(TwilioAccessManager arg0)
    {
        logger.d("Received AccessManager:onTokenUpdated.");
    }

    private Handler setupListenerHandler()
    {
        Looper  looper;
        Handler handler;
        if ((looper = Looper.myLooper()) != null) {
            handler = new Handler(looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            handler = new Handler(looper);
        } else {
            handler = null;
            throw new IllegalArgumentException("Channel Listener must have a Looper.");
        }
        return handler;
    }

    private class GetCapabilityTokenAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            ipMessagingClient.updateToken(null, new StatusListener() {

                @Override
                public void onSuccess()
                {
                    logger.d("Updated Token was successfull");
                }
                @Override
                public void onError(ErrorInfo errorInfo)
                {
                    logger.e("Updated Token failed");
                }
            });
            acessMgr.updateToken(null);
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params)
        {
            try {
                capabilityToken = HttpHelper.httpGet(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return capabilityToken;
        }
    }
}
