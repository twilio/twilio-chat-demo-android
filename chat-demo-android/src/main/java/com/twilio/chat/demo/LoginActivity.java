package com.twilio.ipmessaging.demo;

import java.io.IOException;
import java.net.URLEncoder;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;

import com.twilio.ipmessaging.Constants.StatusListener;
import com.twilio.ipmessaging.ErrorInfo;

import com.twilio.ipmessaging.IPMessagingClient;
import com.twilio.ipmessaging.demo.BasicIPMessagingClient.LoginListener;
import com.twilio.ipmessaging.demo.R;
import com.twilio.ipmessaging.demo.BuildConfig;

import android.net.Uri;
import android.provider.Settings.Secure;
import android.support.v4.content.LocalBroadcastManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.preference.PreferenceManager;

public class LoginActivity extends Activity implements LoginListener
{
    private static final Logger logger = Logger.getLogger(LoginActivity.class);

    private static final String    DEFAULT_CLIENT_NAME = "TestUser";
    private ProgressDialog         progressDialog;
    private Button                 login;
    private EditText               clientNameTextBox;
    private String                 endpoint_id = "";

    // GCM
    private CheckBox          gcmAvailable;
    private boolean           isReceiverRegistered;
    private BroadcastReceiver registrationBroadcastReceiver;
    private static final int  PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        registrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // progressDialog.dismiss();
                SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken =
                    sharedPreferences.getBoolean(GcmPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    logger.i("GCM token remembered");
                } else {
                    logger.w("GCM token NOT remembered");
                }
            }
        };

        // Registering BroadcastReceiver
        registerReceiver();

        this.clientNameTextBox = (EditText)findViewById(R.id.client_name);
        this.clientNameTextBox.setText(DEFAULT_CLIENT_NAME);
        this.endpoint_id =
            Secure.getString(this.getApplicationContext().getContentResolver(), Secure.ANDROID_ID);

        this.login = (Button)findViewById(R.id.register);
        this.login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String idChosen = clientNameTextBox.getText().toString();
                String endpointIdFull =
                    idChosen + "-" + endpoint_id + "-android-" + getApplication().getPackageName();

                String url = Uri.parse(BuildConfig.ACCESS_TOKEN_SERVICE_URL)
                                 .buildUpon()
                                 .appendQueryParameter("identity", idChosen)
                                 .appendQueryParameter("endpointId", endpointIdFull)
                                 .build()
                                 .toString();
                logger.d("url string : " + url);
                TwilioApplication.get().getBasicClient().login(idChosen, url, LoginActivity.this);
            }
        });

        //logout = (Button)findViewById(R.id.logout);

        gcmAvailable = (CheckBox)findViewById(R.id.gcmcxbx);

        if (checkPlayServices()) {
            gcmAvailable.setChecked(true);
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            showAboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("About")
            .setMessage("Version: " + IPMessagingClient.getSdkVersion())
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.cancel();
                }
            });

        AlertDialog aboutDialog = builder.create();
        aboutDialog.show();
    }

    @Override
    public void onLoginStarted()
    {
        logger.d("Log in started");
        progressDialog = ProgressDialog.show(this, "", "Logging in. Please wait...", true);
    }

    @Override
    public void onLoginFinished()
    {
        progressDialog.dismiss();
        Intent intent = new Intent(this, ChannelActivity.class);
        startActivity(intent);
    }

    @Override
    public void onLoginError(String errorMessage)
    {
        progressDialog.dismiss();
        logger.e("Error logging in : " + errorMessage);
        Toast.makeText(getBaseContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onLogoutFinished()
    {
        logger.d("Log out finished");
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause()
    {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(registrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver()
    {
        if (!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                registrationBroadcastReceiver,
                new IntentFilter(GcmPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int                   resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                    .show();
            } else {
                logger.i("This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
}
