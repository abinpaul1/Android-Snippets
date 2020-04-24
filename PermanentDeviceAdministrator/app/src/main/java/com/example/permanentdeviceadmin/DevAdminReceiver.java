package com.example.permanentdeviceadmin;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;


public class DevAdminReceiver extends DeviceAdminReceiver {
    DevicePolicyManager dpm;
    long current_time;
    Timer myThread;

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        Log.d("Root", "Device Owner Enabled");
    }

    @Nullable
    @Override
    public CharSequence onDisableRequested(@NonNull Context context, @NonNull Intent intent) {
        Log.d("Root","Disable Requested");
        Intent startMain = new Intent(android.provider.Settings.ACTION_SETTINGS);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(startMain);

        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        myThread = new Timer();
        current_time = System.currentTimeMillis();
        myThread.schedule(lock_task,0,1000);

        //// Preventing disabling using AccessibilityService
        // AccessibilityService.instance.doAction();

        return "Warning";
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_DEVICE_ADMIN_DISABLE_REQUESTED.equals(action)) {
            CharSequence res = onDisableRequested(context, intent);
            if (res != null) {
                dpm.lockNow();
                Bundle extras = getResultExtras(true);
                extras.putCharSequence(EXTRA_DISABLE_WARNING, res);
            }
        }else if (ACTION_DEVICE_ADMIN_DISABLED.equals(action)) {
            Log.d("Device Admin","Disabled");
        }
    }


    // For 5 seconds after disable request, android stops any other apps from
    // drawing over the settings activity
    // We repeatedly lock the phone for 5 seconds to pass the time
    // After that on unlocking, the settings activity would have been restarted from the intent we
    // opened in the disable request handling
    TimerTask lock_task = new TimerTask() {
        @Override
        public void run() {
            long diff = System.currentTimeMillis() - current_time;
            if (diff<5000) {
                Log.d("Timer","1 second");
                dpm.lockNow();
            }
            else{
                myThread.cancel();
            }
        }
    };


}
