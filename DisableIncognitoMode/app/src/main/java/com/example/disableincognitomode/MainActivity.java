package com.example.disableincognitomode;

import androidx.appcompat.app.AppCompatActivity;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.ArraySet;

public class MainActivity extends AppCompatActivity {

    // Blacklist for URLS to block from chrome
    private String blacklist = "[\"youtube.com\", \"imdb.com\"]";
    private static final String PACKAGE_NAME_CHROME = "com.android.chrome";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setChromeRestrictions(getApplicationContext());
    }

    private void setChromeRestrictions(Context context) {
        final Bundle settings = new Bundle();
        settings.putString("IncognitoModeAvailability", "1");
        settings.putString("BrowserGuestModeEnabled", "false");
        settings.putString("BrowserAddPersonEnabled", "false");
        settings.putString("URLBlacklist", blacklist);

        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName ownerComponent = new ComponentName(context, DevAdminReceiver.class);
        dpm.setApplicationRestrictions(ownerComponent,PACKAGE_NAME_CHROME, settings);
    }
}
