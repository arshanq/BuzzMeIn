package com.arshaq.buzzmein;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog;
import android.text.format.DateUtils;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Recent calls from the system call log. With {@link #EXTRA_PICK}, tapping an entry returns its
 * number instead of calling it.
 */
public class RecentsActivity extends Activity {
    static final String EXTRA_PICK = "pick";
    static final String RESULT_NUMBER = "number";
    static final String RESULT_NAME = "name";
    private static final int REQ_LOG = 1;

    private static final class Entry {
        String number, name, detail;

        @Override
        public String toString() {
            String title = name == null || name.isEmpty() ? number : name + "  " + number;
            return title + "\n" + detail;
        }
    }

    private boolean pick;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pick = getIntent().getBooleanExtra(EXTRA_PICK, false);
        setTitle(pick ? "Pick the buzzer call" : "Recent calls");
        list = new ListView(this);
        setContentView(list);
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            load();
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_CALL_LOG}, REQ_LOG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            load();
        } else {
            TextView t = Ui.text(this, "Call log permission is needed to show recent calls.", 16);
            t.setPadding(40, 40, 40, 40);
            setContentView(t);
        }
    }

    private void load() {
        List<Entry> entries = new ArrayList<>();
        String[] cols = {CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE,
                CallLog.Calls.DATE, CallLog.Calls.DURATION};
        try (Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, cols, null, null,
                CallLog.Calls.DATE + " DESC")) {
            while (c != null && c.moveToNext() && entries.size() < 200) {
                Entry e = new Entry();
                e.number = c.getString(0);
                e.name = c.getString(1);
                if (e.number == null || e.number.isEmpty()) continue;
                CharSequence when = DateUtils.getRelativeDateTimeString(this, c.getLong(3),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
                e.detail = typeLabel(c.getInt(2)) + " · " + when + " · " + c.getLong(4) + "s";
                entries.add(e);
            }
        }
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, entries));
        list.setOnItemClickListener((parent, view, position, id) -> {
            Entry e = entries.get(position);
            if (pick) {
                setResult(RESULT_OK, new Intent().putExtra(RESULT_NUMBER, e.number).putExtra(RESULT_NAME, e.name));
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(e.number)
                        .setItems(new String[]{"Call back", "Use as buzzer"}, (d, which) -> {
                            if (which == 0) DialerActivity.call(this, e.number);
                            else RuleDialog.show(this, RuleDialog.newRule(e.number, e.name), null);
                        })
                        .show();
            }
        });
    }

    private static String typeLabel(int type) {
        switch (type) {
            case CallLog.Calls.INCOMING_TYPE: return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE: return "Outgoing";
            case CallLog.Calls.MISSED_TYPE: return "Missed";
            case CallLog.Calls.REJECTED_TYPE: return "Declined";
            case CallLog.Calls.BLOCKED_TYPE: return "Blocked";
            default: return "Call";
        }
    }
}
