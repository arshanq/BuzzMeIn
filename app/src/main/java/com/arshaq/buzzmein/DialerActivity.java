package com.arshaq.buzzmein;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/** Minimal dial pad. Needed because the default Phone app must handle DIAL intents. */
public class DialerActivity extends Activity {
    private static final int REQ_CALL = 1;
    private EditText number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = Ui.column(this);
        int p = Ui.dp(this, 16);
        root.setPadding(p, p, p, p);

        number = new EditText(this);
        number.setTextSize(30);
        number.setGravity(Gravity.CENTER);
        number.setInputType(InputType.TYPE_CLASS_PHONE);
        number.setShowSoftInputOnFocus(false);
        root.addView(number);

        root.addView(Ui.keypad(this, 0xFF212121, key -> {
            int at = Math.max(number.getSelectionStart(), 0);
            number.getText().insert(at, String.valueOf(key));
        }), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = Ui.row(this);
        actions.setGravity(Gravity.CENTER);
        actions.addView(Ui.button(this, "Recents", v ->
                startActivity(new Intent(this, RecentsActivity.class))));
        actions.addView(Ui.roundButton(this, "Call", 0xFF2E7D32, v -> placeCall()));
        actions.addView(Ui.button(this, "⌫", v -> {
            int end = number.getSelectionEnd();
            if (end > 0) number.getText().delete(end - 1, end);
        }));
        root.addView(actions);
        setContentView(root);
        readIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        readIntent(intent);
    }

    private void readIntent(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "tel".equals(data.getScheme())) {
            number.setText(data.getSchemeSpecificPart());
            number.setSelection(number.length());
        }
    }

    private void placeCall() {
        String n = number.getText().toString().trim();
        if (n.isEmpty()) return;
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL);
            return;
        }
        call(this, n);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == REQ_CALL && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            placeCall();
        }
    }

    static void call(Activity a, String number) {
        if (a.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(a, "Grant the Phone permission first", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            a.getSystemService(TelecomManager.class)
                    .placeCall(Uri.fromParts("tel", number, null), new Bundle());
        } catch (SecurityException e) {
            Toast.makeText(a, "Could not place call: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
