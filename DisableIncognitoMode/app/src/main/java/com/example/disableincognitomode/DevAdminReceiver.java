package com.example.disableincognitomode;


import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

public class DevAdminReceiver extends DeviceAdminReceiver {
    DevicePolicyManager dpm;

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        Log.d("Msg", "Device Admin Enabled");
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        Log.d("Msg", "Device Admin Disabled");
    }
}
