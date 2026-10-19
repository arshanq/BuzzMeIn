package com.arshaq.buzzmein;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ringing / in-call screen for every call that is not auto-handled. */
public class InCallActivity extends Activity implements CallManager.Listener {
    static final String ACTION_ANSWER = "com.arshaq.buzzmein.ANSWER";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView name, status;
    private LinearLayout ringingRow, activeRow;
    private Button muteButton, speakerButton, holdButton;
    private GridLayout keypad;
    private Call shownAccountPickerFor;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        buildUi();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (ACTION_ANSWER.equals(intent.getAction())) {
            Call call = CallManager.primary();
            if (call != null && CallManager.state(call) == Call.STATE_RINGING) {
                call.answer(VideoProfile.STATE_AUDIO_ONLY);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallManager.addListener(this);
        onCallsChanged();
        handler.post(tick);
    }

    @Override
    protected void onStop() {
        CallManager.removeListener(this);
        handler.removeCallbacks(tick);
        super.onStop();
    }

    private void buildUi() {
        int white = 0xFFFFFFFF;
        LinearLayout root = Ui.column(this);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 24);
        root.setPadding(p, Ui.dp(this, 72), p, p);

        name = Ui.text(this, "", 32);
        name.setTextColor(white);
        name.setGravity(Gravity.CENTER);
        status = Ui.text(this, "", 18);
        status.setTextColor(0xFFB0BEC5);
        status.setGravity(Gravity.CENTER);
        root.addView(name);
        root.addView(status);

        View spacer = new View(this);
        root.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));

        keypad = Ui.keypad(this, white, new Ui.KeyListener() {
            @Override
            public void onKeyDown(char key) {
                Call c = CallManager.primary();
                if (c != null) c.playDtmfTone(key);
            }

            @Override
            public void onKeyUp(char key) {
                Call c = CallManager.primary();
                if (c != null) c.stopDtmfTone();
            }
        });
        keypad.setVisibility(View.GONE);
        root.addView(keypad, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        activeRow = Ui.column(this);
        activeRow.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout toggles = Ui.row(this);
        toggles.setGravity(Gravity.CENTER);
        muteButton = Ui.roundButton(this, "Mute", 0xFF37474F, v -> toggleMute());
        speakerButton = Ui.roundButton(this, "Speaker", 0xFF37474F, v -> toggleSpeaker());
        Button keys = Ui.roundButton(this, "Keypad", 0xFF37474F, v ->
                keypad.setVisibility(keypad.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
        holdButton = Ui.roundButton(this, "Hold", 0xFF37474F, v -> toggleHold());
        toggles.addView(muteButton);
        toggles.addView(speakerButton);
        toggles.addView(keys);
        LinearLayout toggles2 = Ui.row(this);
        toggles2.setGravity(Gravity.CENTER);
        toggles2.addView(holdButton);
        toggles2.addView(Ui.roundButton(this, "End", 0xFFC62828, v -> {
            Call c = CallManager.primary();
            if (c != null) c.disconnect();
        }));
        activeRow.addView(toggles);
        activeRow.addView(toggles2);
        root.addView(activeRow);

        ringingRow = Ui.row(this);
        ringingRow.setGravity(Gravity.CENTER);
        ringingRow.addView(Ui.roundButton(this, "Decline", 0xFFC62828, v -> {
            Call c = CallManager.primary();
            if (c != null) c.reject(false, null);
        }));
        ringingRow.addView(Ui.roundButton(this, "Answer", 0xFF2E7D32, v -> {
            Call c = CallManager.primary();
            if (c != null) c.answer(VideoProfile.STATE_AUDIO_ONLY);
        }));
        root.addView(ringingRow);

        setContentView(root);
    }

    @Override
    public void onCallsChanged() {
        Call call = CallManager.primary();
        if (call == null) {
            handler.postDelayed(() -> {
                if (CallManager.primary() == null) finishAndRemoveTask();
            }, 800);
            status.setText("Call ended");
            ringingRow.setVisibility(View.GONE);
            activeRow.setVisibility(View.GONE);
            return;
        }
        int state = CallManager.state(call);
        name.setText(CallManager.displayName(call));
        ringingRow.setVisibility(state == Call.STATE_RINGING ? View.VISIBLE : View.GONE);
        activeRow.setVisibility(state == Call.STATE_RINGING ? View.GONE : View.VISIBLE);
        holdButton.setText(state == Call.STATE_HOLDING ? "Resume" : "Hold");
        if (state == Call.STATE_SELECT_PHONE_ACCOUNT && shownAccountPickerFor != call) {
            shownAccountPickerFor = call;
            pickAccount(call);
        }

        CallAudioState audio = CallManager.service == null ? null : CallManager.service.getCallAudioState();
        if (audio != null) {
            highlight(muteButton, audio.isMuted());
            highlight(speakerButton, audio.getRoute() == CallAudioState.ROUTE_SPEAKER);
        }
        updateStatus();
    }

    private void updateStatus() {
        Call call = CallManager.primary();
        if (call == null) return;
        switch (CallManager.state(call)) {
            case Call.STATE_RINGING:
                status.setText("Incoming call");
                break;
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
            case Call.STATE_NEW:
                status.setText("Calling…");
                break;
            case Call.STATE_HOLDING:
                status.setText("On hold");
                break;
            case Call.STATE_DISCONNECTING:
            case Call.STATE_DISCONNECTED:
                status.setText("Call ended");
                break;
            case Call.STATE_SELECT_PHONE_ACCOUNT:
                status.setText("Choose SIM");
                break;
            case Call.STATE_ACTIVE: {
                long connected = call.getDetails().getConnectTimeMillis();
                long secs = connected > 0 ? (System.currentTimeMillis() - connected) / 1000 : 0;
                status.setText(String.format(Locale.US, "%d:%02d", secs / 60, secs % 60));
                break;
            }
            default:
                status.setText("");
        }
    }

    private void highlight(Button b, boolean on) {
        b.getBackground().mutate().setTint(on ? 0xFF2E7D32 : 0xFF37474F);
    }

    private void toggleMute() {
        CallService s = CallManager.service;
        if (s == null || s.getCallAudioState() == null) return;
        s.setMuted(!s.getCallAudioState().isMuted());
    }

    private void toggleSpeaker() {
        CallService s = CallManager.service;
        if (s == null || s.getCallAudioState() == null) return;
        boolean speaker = s.getCallAudioState().getRoute() == CallAudioState.ROUTE_SPEAKER;
        s.setAudioRoute(speaker ? CallAudioState.ROUTE_WIRED_OR_EARPIECE : CallAudioState.ROUTE_SPEAKER);
    }

    private void toggleHold() {
        Call c = CallManager.primary();
        if (c == null) return;
        if (CallManager.state(c) == Call.STATE_HOLDING) c.unhold();
        else c.hold();
    }

    /** Dual-SIM phones with no default SIM ask the dialer which SIM to use. */
    private void pickAccount(Call call) {
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            call.disconnect();
            return;
        }
        TelecomManager tm = getSystemService(TelecomManager.class);
        List<PhoneAccountHandle> handles = tm.getCallCapablePhoneAccounts();
        List<String> labels = new ArrayList<>();
        for (PhoneAccountHandle h : handles) {
            PhoneAccount a = tm.getPhoneAccount(h);
            labels.add(a == null ? h.getId() : String.valueOf(a.getLabel()));
        }
        new AlertDialog.Builder(this)
                .setTitle("Call with")
                .setItems(labels.toArray(new String[0]), (d, which) ->
                        call.phoneAccountSelected(handles.get(which), false))
                .setOnCancelListener(d -> call.disconnect())
                .show();
    }
}
