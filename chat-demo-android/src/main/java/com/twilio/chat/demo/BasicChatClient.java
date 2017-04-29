package com.twilio.chat.demo;

import java.util.Arrays;
import java.util.List;

import com.twilio.accessmanager.AccessManager;

import com.twilio.chat.Channel;
import com.twilio.chat.StatusListener;
import com.twilio.chat.CallbackListener;
import com.twilio.chat.ChatClientListener;
import com.twilio.chat.ChatClient;
import com.twilio.chat.ErrorInfo;
import com.twilio.chat.UserInfo;
import com.twilio.chat.internal.HandlerUtil;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class BasicChatClient extends CallbackListener<ChatClient>
    implements AccessManager.Listener, AccessManager.TokenUpdateListener
{
    private static final Logger logger = Logger.getLogger(BasicChatClient.class);

    private String accessToken;
    private String fcmToken;

    private ChatClient chatClient;

    private Context       context;
    private AccessManager accessManager;

    private LoginListener loginListener;
    private Handler       loginListenerHandler;

    private String        urlString;
    private String        username;

    public BasicChatClient(Context context)
    {
        super();
        this.context = context;

        if (BuildConfig.DEBUG) {
            ChatClient.setLogLevel(android.util.Log.DEBUG);
        } else {
            ChatClient.setLogLevel(android.util.Log.ERROR);
        }
    }

    public interface LoginListener {
        public void onLoginStarted();

        public void onLoginFinished();

        public void onLoginError(String errorMessage);

        public void onLogoutFinished();
    }

    public void setFCMToken(String fcmToken)
    {
        logger.e("setFCMToken "+fcmToken);
        this.fcmToken = fcmToken;
        if (chatClient != null) {
            setupFcmToken();
        }
    }

    public void login(final String username, final String url, final LoginListener listener) {
        if (username == this.username && urlString == url && loginListener == listener && chatClient != null && accessManager != null) {
            onSuccess(chatClient);
            return;
        }

        this.username = username;
        urlString = url;

        loginListenerHandler = HandlerUtil.setupListenerHandler();
        loginListener = listener;

        new GetAccessTokenAsyncTask().execute(username, urlString);
    }

    public ChatClient getChatClient()
    {
        return chatClient;
    }

    private void setupFcmToken()
    {
        chatClient.registerFCMToken(fcmToken,
            new ToastStatusListener(
                "Firebase Messaging registration successful",
                "Firebase Messaging registration not successful"));
    }

    public void unregisterFcmToken()
    {
        chatClient.unregisterFCMToken(fcmToken,
            new ToastStatusListener(
                "Firebase Messaging unregistration successful",
                "Firebase Messaging unregistration not successful"));
    }

    private void createAccessManager()
    {
        if (accessManager != null) return;

        accessManager = new AccessManager(accessToken, this);
        accessManager.addTokenUpdateListener(this);
    }

    private void createClient()
    {
        if (chatClient != null) return;

        ChatClient.Properties props =
            new ChatClient.Properties.Builder()
                .setRegion("us1")
                .createProperties();

        ChatClient.create(context.getApplicationContext(),
                                 accessToken,
                                 props,
                                 this);
    }

    public void shutdown()
    {
        chatClient.shutdown();
        chatClient = null; // Client no longer usable after shutdown()
    }

    // Client created, remember the reference and set up UI
    @Override
    public void onSuccess(ChatClient client)
    {
        logger.d("Received completely initialized ChatClient");
        chatClient = client;

        if (fcmToken != null) {
            setupFcmToken();
        }

        loginListenerHandler.post(new Runnable() {
            @Override
            public void run()
            {
            if (loginListener != null) {
                loginListener.onLoginFinished();
            }
            }
        });
    }

    // Client not created, fail
    @Override
    public void onError(final ErrorInfo errorInfo)
    {
        TwilioApplication.get().logErrorInfo("Received onError event", errorInfo);

        loginListenerHandler.post(new Runnable() {
            @Override
            public void run()
            {
            if (loginListener != null) {
                loginListener.onLoginError(errorInfo.getErrorCode() + " " + errorInfo.getErrorText());
            }
            }
        });
    }

    // AccessManager.Listener

    @Override
    public void onTokenWillExpire(AccessManager accessManager)
    {
        TwilioApplication.get().showToast("AccessManager.onTokenWillExpire");
    }

    @Override
    public void onTokenExpired(AccessManager accessManager)
    {
        TwilioApplication.get().showToast("Token expired. Getting new token.");
        new GetAccessTokenAsyncTask().execute(username, urlString);
    }

    @Override
    public void onError(AccessManager accessManager, String err)
    {
        TwilioApplication.get().showToast("AccessManager error: " + err);
    }

    // AccessManager.TokenUpdateListener

    @Override
    public void onTokenUpdated(String token)
    {
        TwilioApplication.get().showToast("AccessManager token updated: " + token);

        if (chatClient == null) return;

        chatClient.updateToken(token, new ToastStatusListener(
            "Client Update Token was successfull",
            "Client Update Token failed"));
    }

    /**
     * Modify this method if you need to provide more information to your Access Token Service.
     */
    private class GetAccessTokenAsyncTask extends AsyncTask<String, Void, String>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            loginListenerHandler.post(new Runnable() {
                @Override
                public void run()
                {
                    if (loginListener != null) {
                        loginListener.onLoginStarted();
                    }
                }
            });
        }

        @Override
        protected String doInBackground(String... params)
        {
            try {
                accessToken = HttpHelper.httpGet(params[0], params[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return accessToken;
        }

        @Override
        protected void onPostExecute(String result)
        {
            super.onPostExecute(result);
            createAccessManager();
            createClient();
            accessManager.updateToken(accessToken);
        }
    }
}
