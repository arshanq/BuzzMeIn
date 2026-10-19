package com.arshaq.buzzmein;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.Call;

public class NotificationActionReceiver extends BroadcastReceiver {
    static final String DECLINE = "com.arshaq.buzzmein.DECLINE";
    static final String HANGUP = "com.arshaq.buzzmein.HANGUP";

    @Override
    public void onReceive(Context context, Intent intent) {
        Call call = CallManager.primary();
        if (call == null || intent.getAction() == null) return;
        switch (intent.getAction()) {
            case DECLINE:
                call.reject(false, null);
                break;
            case HANGUP:
                call.disconnect();
                break;
        }
    }
}
