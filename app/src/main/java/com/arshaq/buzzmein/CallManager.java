package com.arshaq.buzzmein;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory view of the calls Telecom has handed to {@link CallService}. Main thread only. */
final class CallManager {
    interface Listener {
        void onCallsChanged();
    }

    static CallService service;

    private static final List<Call> calls = new ArrayList<>();
    private static final Set<Call> autoHandled = new HashSet<>();
    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private CallManager() {
    }

    static void add(Call call) {
        calls.add(call);
        changed();
    }

    static void remove(Call call) {
        calls.remove(call);
        autoHandled.remove(call);
        changed();
    }

    static int count() {
        return calls.size();
    }

    static void markAuto(Call call, boolean auto) {
        if (auto) autoHandled.add(call);
        else autoHandled.remove(call);
        changed();
    }

    static boolean isAuto(Call call) {
        return autoHandled.contains(call);
    }

    /** Calls the user should see, i.e. everything not being handled silently. */
    static List<Call> visibleCalls() {
        List<Call> out = new ArrayList<>();
        for (Call c : calls) if (!autoHandled.contains(c)) out.add(c);
        return out;
    }

    /** The call the in-call UI should focus on: ringing first, then live, then anything. */
    static Call primary() {
        List<Call> visible = visibleCalls();
        for (Call c : visible) if (state(c) == Call.STATE_RINGING) return c;
        for (Call c : visible) {
            int s = state(c);
            if (s != Call.STATE_HOLDING && s != Call.STATE_DISCONNECTED) return c;
        }
        return visible.isEmpty() ? null : visible.get(0);
    }

    @SuppressWarnings("deprecation")
    static int state(Call call) {
        return Build.VERSION.SDK_INT >= 31 ? call.getDetails().getState() : call.getState();
    }

    static String number(Call call) {
        Uri handle = call.getDetails().getHandle();
        return handle == null ? "" : handle.getSchemeSpecificPart();
    }

    static String displayName(Call call) {
        Call.Details d = call.getDetails();
        String name = null;
        if (Build.VERSION.SDK_INT >= 30) name = d.getContactDisplayName();
        if (name == null || name.isEmpty()) name = d.getCallerDisplayName();
        if (name == null || name.isEmpty()) name = number(call);
        return name == null || name.isEmpty() ? "Unknown caller" : name;
    }

    static void addListener(Listener l) {
        listeners.add(l);
    }

    static void removeListener(Listener l) {
        listeners.remove(l);
    }

    static void changed() {
        for (Listener l : listeners) l.onCallsChanged();
        if (service != null) Notifications.refresh(service);
    }
}
