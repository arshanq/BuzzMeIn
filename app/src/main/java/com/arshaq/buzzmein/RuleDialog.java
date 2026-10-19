package com.arshaq.buzzmein;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

/** Add / edit a buzzer rule. */
final class RuleDialog {
    private RuleDialog() {
    }

    static Rule newRule(String number, String name) {
        Rule r = new Rule();
        r.number = number == null ? "" : number;
        if (name != null && !name.isEmpty()) r.label = name;
        return r;
    }

    static void show(Activity a, Rule rule, Runnable onSaved) {
        LinearLayout form = Ui.column(a);
        int p = Ui.dp(a, 20);
        form.setPadding(p, Ui.dp(a, 8), p, 0);

        EditText label = field(a, form, "Name", rule.label, InputType.TYPE_CLASS_TEXT);
        EditText number = field(a, form, "Buzzer phone number (as shown in call log)", rule.number,
                InputType.TYPE_CLASS_PHONE);
        EditText digits = field(a, form, "Keys to press to open the door (e.g. 9). Use , for a 1s pause",
                rule.digits, InputType.TYPE_CLASS_PHONE);
        boolean allDay = rule.windowStart == rule.windowEnd;
        EditText from = field(a, form, "Only between… start (HH:MM, blank = all day)",
                allDay ? "" : Rule.formatTime(rule.windowStart), InputType.TYPE_CLASS_DATETIME);
        EditText until = field(a, form, "…and end (HH:MM)",
                allDay ? "" : Rule.formatTime(rule.windowEnd), InputType.TYPE_CLASS_DATETIME);
        EditText answerDelay = field(a, form, "Answer after (ms)", String.valueOf(rule.answerDelayMs),
                InputType.TYPE_CLASS_NUMBER);
        EditText dtmfDelay = field(a, form, "Wait after answering before pressing keys (ms)",
                String.valueOf(rule.dtmfDelayMs), InputType.TYPE_CLASS_NUMBER);
        EditText hangup = field(a, form, "Hang up after keys (ms)", String.valueOf(rule.hangupDelayMs),
                InputType.TYPE_CLASS_NUMBER);
        CheckBox enabled = new CheckBox(a);
        enabled.setText("Enabled");
        enabled.setChecked(rule.enabled);
        form.addView(enabled);

        ScrollView scroll = new ScrollView(a);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(Store.rules(a).stream().anyMatch(r -> r.id == rule.id) ? "Edit buzzer" : "New buzzer")
                .setView(scroll)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = number.getText().toString().trim();
            String keys = digits.getText().toString().replaceAll("[^0-9*#,]", "");
            if (Rule.digitsOnly(n).length() < 3) {
                toast(a, "Enter the buzzer's phone number");
                return;
            }
            if (keys.replace(",", "").isEmpty()) {
                toast(a, "Enter at least one key (0-9, * or #)");
                return;
            }
            int start, end;
            String fs = from.getText().toString(), us = until.getText().toString();
            if (fs.trim().isEmpty() && us.trim().isEmpty()) {
                start = end = 0;
            } else {
                start = Rule.parseTime(fs);
                end = Rule.parseTime(us);
                if (start < 0 || end < 0) {
                    toast(a, "Times must look like 05:30 (or leave both blank)");
                    return;
                }
            }
            rule.label = label.getText().toString().trim().isEmpty() ? "Building buzzer" : label.getText().toString().trim();
            rule.number = n;
            rule.digits = keys;
            rule.windowStart = start;
            rule.windowEnd = end;
            rule.answerDelayMs = parseInt(answerDelay, rule.answerDelayMs);
            rule.dtmfDelayMs = parseInt(dtmfDelay, rule.dtmfDelayMs);
            rule.hangupDelayMs = parseInt(hangup, rule.hangupDelayMs);
            rule.enabled = enabled.isChecked();
            Store.upsert(a, rule);
            dialog.dismiss();
            toast(a, "Saved");
            if (onSaved != null) onSaved.run();
        }));
        dialog.show();
    }

    private static EditText field(Activity a, LinearLayout form, String hint, String value, int type) {
        form.addView(Ui.text(a, hint, 13));
        EditText e = new EditText(a);
        e.setInputType(type);
        e.setText(value);
        e.setSingleLine(true);
        form.addView(e);
        return e;
    }

    private static int parseInt(EditText e, int fallback) {
        try {
            return Math.max(0, Math.min(30_000, Integer.parseInt(e.getText().toString().trim())));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static void toast(Activity a, String s) {
        Toast.makeText(a, s, Toast.LENGTH_SHORT).show();
    }
}
