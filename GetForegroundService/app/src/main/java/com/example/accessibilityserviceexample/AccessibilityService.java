package com.example.accessibilityserviceexample;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;


public class AccessibilityService extends android.accessibilityservice.AccessibilityService{
    public static AccessibilityService instance;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        // Note : This event is sometimes called more than one for a foreground service
        if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            Log.d("Event","TYPE_WINDOW_STATE_CHANGED");
            Log.d("Pkg",accessibilityEvent.getPackageName().toString());

            // Check PackageName matching here and continue with code
            // Here we prevent whatsapp from opening
            // Each time it launches we simulate the press of back button
            if (accessibilityEvent.getPackageName().equals("com.whatsapp")){
                doAction();
            }

        }
//        else if (accessibilityEvent.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){
////        If any viw is modified in same app also this is called
//            Log.d("Event","TYPE_WINDOW_CONTENT_CHANGED");
//            Log.d("Pkg",accessibilityEvent.getPackageName().toString());
//        }

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("Accessibility","Service Connected");

        /* Add below code Only if you haven't configured via xml
        Certain configuration options like canRetrieveWindowContent are only
        available if you configure your service using XML.*/

//        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
//        info.eventTypes=AccessibilityEvent.TYPES_ALL_MASK;
//        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK;
//        info.notificationTimeout = 100;
//        info.packageNames = null;
//        setServiceInfo(info);
    }

    public void doAction(){
        performGlobalAction(GLOBAL_ACTION_BACK);
    }
}
