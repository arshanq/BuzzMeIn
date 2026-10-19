package com.arshaq.buzzmein;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Setup + configuration screen. */
public class MainActivity extends Activity {
    private static final int REQ_ROLE = 1;
    private static final int REQ_PICK = 2;
    private static final int REQ_PERMS = 3;

    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifications.createChannels(this);
        ScrollView scroll = new ScrollView(this);
        content = Ui.column(this);
        int p = Ui.dp(this, 16);
        content.setPadding(p, 0, p, p);
        scroll.addView(content);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        content.removeAllViews();

        // --- Setup checklist
        content.addView(Ui.header(this, "Setup"));
        boolean isDialer = isDefaultDialer();
        content.addView(status(isDialer, isDialer
                ? "Buzz Me In is your default Phone app"
                : "Must be the default Phone app to answer calls and press keys"));
        if (!isDialer) content.addView(Ui.button(this, "Make default Phone app", v -> requestRole()));

        List<String> missing = missingPermissions();
        content.addView(status(missing.isEmpty(), missing.isEmpty()
                ? "Permissions granted" : "Permissions needed: call log, phone, notifications"));
        if (!missing.isEmpty()) {
            content.addView(Ui.button(this, "Grant permissions",
                    v -> requestPermissions(missing.toArray(new String[0]), REQ_PERMS)));
        }

        if (Build.VERSION.SDK_INT >= 34) {
            boolean fsi = getSystemService(NotificationManager.class).canUseFullScreenIntent();
            content.addView(status(fsi, fsi ? "Can show incoming calls on lock screen"
                    : "Allow full-screen notifications so other calls can ring on the lock screen"));
            if (!fsi) {
                content.addView(Ui.button(this, "Allow full-screen notifications", v ->
                        startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:" + getPackageName())))));
            }
        }

        // --- Master switch
        content.addView(Ui.header(this, "Auto let-in"));
        Switch armed = new Switch(this);
        armed.setText("Automatically answer buzzer calls");
        armed.setTextSize(16);
        armed.setChecked(Store.isArmed(this));
        armed.setOnCheckedChangeListener((b, on) -> Store.setArmed(this, on));
        content.addView(armed);

        // --- Rules
        content.addView(Ui.header(this, "Buzzer numbers"));
        List<Rule> rules = Store.rules(this);
        if (rules.isEmpty()) {
            content.addView(Ui.text(this, "None yet. Have the buzzer call you once, then pick it from the call log.", 14));
        }
        for (Rule r : rules) content.addView(ruleCard(r));
        LinearLayout addRow = Ui.row(this);
        addRow.addView(Ui.button(this, "Add from call log", v ->
                startActivityForResult(new Intent(this, RecentsActivity.class)
                        .putExtra(RecentsActivity.EXTRA_PICK, true), REQ_PICK)));
        addRow.addView(Ui.button(this, "Add manually", v ->
                RuleDialog.show(this, RuleDialog.newRule("", null), this::render)));
        content.addView(addRow);

        // --- Phone
        content.addView(Ui.header(this, "Phone"));
        LinearLayout phoneRow = Ui.row(this);
        phoneRow.addView(Ui.button(this, "Dial pad", v -> startActivity(new Intent(this, DialerActivity.class))));
        phoneRow.addView(Ui.button(this, "Recent calls", v -> startActivity(new Intent(this, RecentsActivity.class))));
        content.addView(phoneRow);

        // --- Log
        content.addView(Ui.header(this, "Activity"));
        List<String> log = Store.logEntries(this);
        if (log.isEmpty()) content.addView(Ui.text(this, "Nothing yet.", 14));
        for (int i = 0; i < Math.min(20, log.size()); i++) content.addView(Ui.text(this, log.get(i), 13));
        if (!log.isEmpty()) {
            content.addView(Ui.button(this, "Clear activity", v -> {
                Store.clearLog(this);
                render();
            }));
        }
    }

    private View ruleCard(Rule r) {
        LinearLayout box = Ui.column(this);
        TextView title = Ui.text(this, r.label + (r.enabled ? "" : "  (off)"), 16);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        box.addView(title);
        box.addView(Ui.text(this, r.number + "  →  press " + r.digits + "  ·  " + r.describeWindow(), 14));
        box.addView(Ui.text(this, "Tap to edit · long-press to delete", 12));
        View card = Ui.card(this, box);
        card.setOnClickListener(v -> RuleDialog.show(this, r, this::render));
        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete " + r.label + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        Store.delete(this, r.id);
                        render();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
        return card;
    }

    private TextView status(boolean ok, String text) {
        TextView t = Ui.text(this, (ok ? "✅  " : "⚠️  ") + text, 15);
        t.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        return t;
    }

    private boolean isDefaultDialer() {
        RoleManager rm = getSystemService(RoleManager.class);
        return rm != null && rm.isRoleHeld(RoleManager.ROLE_DIALER);
    }

    private void requestRole() {
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), REQ_ROLE);
        } else {
            startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS));
        }
    }

    /**
     * Android 15+ blocks sideloaded apps from becoming the Phone app until "Allow restricted
     * settings" is enabled in App info; repeated denials also silently suppress the dialog.
     */
    private void showRoleHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Couldn't become the Phone app")
                .setMessage("If Android said \"App was denied access\" or \"Restricted setting\":\n"
                        + "1. Tap Open App info.\n"
                        + "2. Tap the ⋮ menu (top right) → Allow restricted settings, and confirm.\n"
                        + "3. Come back and tap Make default Phone app again.\n\n"
                        + "If no prompt appeared at all, tap Default apps and choose Buzz Me In "
                        + "under Phone app.")
                .setPositiveButton("Open App info", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + getPackageName()))))
                .setNeutralButton("Default apps", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)))
                .setNegativeButton("Close", null)
                .show();
    }

    private List<String> missingPermissions() {
        List<String> wanted = new ArrayList<>(List.of(Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE));
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS);
        List<String> missing = new ArrayList<>();
        for (String p : wanted) {
            if (checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED) missing.add(p);
        }
        return missing;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ROLE && !isDefaultDialer()) showRoleHelp();
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null) {
            RuleDialog.show(this, RuleDialog.newRule(data.getStringExtra(RecentsActivity.RESULT_NUMBER),
                    data.getStringExtra(RecentsActivity.RESULT_NAME)), this::render);
        }
        render();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        render();
    }
}
