package com.arshaq.buzzmein;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.telecom.Call;
import android.telecom.TelecomManager;
import android.telecom.VideoProfile;

/** Silently answers a matched call, plays the rule's DTMF keys, then hangs up. */
final class AutoAnswerer extends Call.Callback {
    private static final int TONE_MS = 300;
    private static final int GAP_MS = 300;
    private static final int PAUSE_MS = 1000;
    private static final int ANSWER_TIMEOUT_MS = 10_000;

    private final CallService service;
    private final Call call;
    private final Rule rule;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PowerManager.WakeLock wakeLock;
    private boolean keysScheduled;
    private boolean keysSent;

    AutoAnswerer(CallService service, Call call, Rule rule) {
        this.service = service;
        this.call = call;
        this.rule = rule;
        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BuzzMeIn:auto");
    }

    void start() {
        wakeLock.acquire(60_000);
        silenceRinger();
        call.registerCallback(this);
        handler.postDelayed(() -> call.answer(VideoProfile.STATE_AUDIO_ONLY), rule.answerDelayMs);
        handler.postDelayed(this::giveUpIfNotAnswered, rule.answerDelayMs + ANSWER_TIMEOUT_MS);
    }

    private void silenceRinger() {
        try {
            TelecomManager tm = (TelecomManager) service.getSystemService(Context.TELECOM_SERVICE);
            tm.silenceRinger();
        } catch (SecurityException ignored) {
            // Only allowed while we are the default dialer; ringing briefly is harmless.
        }
    }

    @Override
    public void onStateChanged(Call c, int state) {
        if (state == Call.STATE_ACTIVE && !keysScheduled) {
            keysScheduled = true;
            service.setMuted(true);
            scheduleKeys();
        } else if (state == Call.STATE_DISCONNECTED) {
            finish(keysSent ? null : "Call from " + rule.label + " ended before keys were sent");
        }
    }

    private void scheduleKeys() {
        long t = rule.dtmfDelayMs;
        for (char key : rule.digits.toCharArray()) {
            if (key == ',') {
                t += PAUSE_MS;
            } else if ("0123456789*#".indexOf(key) >= 0) {
                handler.postDelayed(() -> call.playDtmfTone(key), t);
                t += TONE_MS;
                handler.postDelayed(call::stopDtmfTone, t);
                t += GAP_MS;
            }
        }
        handler.postDelayed(() -> {
            keysSent = true;
            Store.log(service, "Let in via " + rule.label + " (sent " + rule.digits + ")");
            Notifications.letIn(service, rule);
        }, t);
        handler.postDelayed(call::disconnect, t + rule.hangupDelayMs);
    }

    /** If the answer never took effect, fall back to normal ringing UI so the user can act. */
    private void giveUpIfNotAnswered() {
        if (keysScheduled || CallManager.state(call) != Call.STATE_RINGING) return;
        Store.log(service, "Could not auto-answer " + rule.label + "; showing call normally");
        call.unregisterCallback(this);
        handler.removeCallbacksAndMessages(null);
        release();
        CallManager.markAuto(call, false);
        service.showInCallUi();
    }

    private void finish(String failure) {
        call.unregisterCallback(this);
        handler.removeCallbacksAndMessages(null);
        if (failure != null) Store.log(service, failure);
        release();
    }

    private void release() {
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
