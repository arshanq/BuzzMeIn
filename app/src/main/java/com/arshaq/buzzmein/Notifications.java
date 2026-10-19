package com.arshaq.buzzmein;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telecom.Call;

final class Notifications {
    private static final String CH_INCOMING = "incoming";
    private static final String CH_ONGOING = "ongoing";
    private static final String CH_EVENTS = "events";
    private static final int ID_CALL = 1;
    private static final int ID_EVENT = 2;

    private Notifications() {
    }

    static void createChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel incoming = new NotificationChannel(CH_INCOMING, "Incoming calls",
                NotificationManager.IMPORTANCE_HIGH);
        incoming.setSound(null, null); // Telecom plays the ringtone.
        nm.createNotificationChannel(incoming);
        nm.createNotificationChannel(new NotificationChannel(CH_ONGOING, "Ongoing calls",
                NotificationManager.IMPORTANCE_LOW));
        nm.createNotificationChannel(new NotificationChannel(CH_EVENTS, "Buzzer activity",
                NotificationManager.IMPORTANCE_DEFAULT));
    }

    /** Mirrors the current visible call into a notification (or clears it). */
    static void refresh(Context c) {
        createChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        Call call = CallManager.primary();
        if (call == null || CallManager.state(call) == Call.STATE_DISCONNECTED) {
            nm.cancel(ID_CALL);
            return;
        }
        PendingIntent open = PendingIntent.getActivity(c, 0,
                new Intent(c, InCallActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean ringing = CallManager.state(call) == Call.STATE_RINGING;
        Notification.Builder b = new Notification.Builder(c, ringing ? CH_INCOMING : CH_ONGOING)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(CallManager.displayName(call))
                .setContentText(ringing ? "Incoming call" : "Ongoing call")
                .setCategory(Notification.CATEGORY_CALL)
                .setOngoing(true)
                .setContentIntent(open);
        if (ringing) {
            b.setFullScreenIntent(open, true)
                    .addAction(action(c, NotificationActionReceiver.DECLINE, "Decline"))
                    .addAction(new Notification.Action.Builder(null, "Answer",
                            PendingIntent.getActivity(c, 2, new Intent(c, InCallActivity.class)
                                            .setAction(InCallActivity.ACTION_ANSWER)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    PendingIntent.FLAG_IMMUTABLE)).build());
        } else {
            b.setUsesChronometer(true)
                    .addAction(action(c, NotificationActionReceiver.HANGUP, "Hang up"));
        }
        nm.notify(ID_CALL, b.build());
    }

    static void letIn(Context c, Rule rule) {
        createChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        PendingIntent open = PendingIntent.getActivity(c, 1, new Intent(c, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE);
        nm.notify(ID_EVENT, new Notification.Builder(c, CH_EVENTS)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("Buzzed someone in")
                .setContentText(rule.label + " called; sent " + rule.digits)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build());
    }

    private static Notification.Action action(Context c, String what, String title) {
        PendingIntent pi = PendingIntent.getBroadcast(c, what.hashCode(),
                new Intent(c, NotificationActionReceiver.class).setAction(what),
                PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(null, title, pi).build();
    }
}
