package com.arshaq.buzzmein;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.InCallService;

/** Bound by Telecom because this app is the default Phone app. Sees every call. */
public class CallService extends InCallService {

    private final Call.Callback callback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            CallManager.changed();
        }

        @Override
        public void onDetailsChanged(Call call, Call.Details details) {
            CallManager.changed();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        CallManager.service = this;
    }

    @Override
    public void onDestroy() {
        if (CallManager.service == this) CallManager.service = null;
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        call.registerCallback(callback);
        boolean incoming = call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING;
        boolean onlyCall = CallManager.count() == 0;
        Rule rule = incoming && onlyCall && CallManager.state(call) == Call.STATE_RINGING
                ? Store.match(this, CallManager.number(call)) : null;

        if (rule != null) {
            CallManager.markAuto(call, true);
            CallManager.add(call);
            new AutoAnswerer(this, call, rule).start();
        } else {
            CallManager.add(call);
            showInCallUi();
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        call.unregisterCallback(callback);
        CallManager.remove(call);
        if (CallManager.count() == 0) setMuted(false);
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState audioState) {
        CallManager.changed();
    }

    void showInCallUi() {
        Intent i = new Intent(this, InCallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }
}
