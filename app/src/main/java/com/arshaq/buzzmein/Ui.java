package com.arshaq.buzzmein;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the (deliberately simple) UI in code. */
final class Ui {
    private Ui() {
    }

    interface KeyListener {
        void onKeyDown(char key);

        default void onKeyUp(char key) {
        }
    }

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static TextView text(Context c, CharSequence s, float sp) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        return t;
    }

    static TextView header(Context c, String s) {
        TextView t = text(c, s, 18);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setPadding(0, dp(c, 20), 0, dp(c, 6));
        return t;
    }

    static Button button(Context c, String label, View.OnClickListener l) {
        Button b = new Button(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    static Button roundButton(Context c, String label, int color, View.OnClickListener l) {
        Button b = button(c, label, l);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        b.setBackground(bg);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setPadding(0, 0, 0, 0);
        b.setMaxLines(1);
        int size = dp(c, 76);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(c, 12), dp(c, 8), dp(c, 12), dp(c, 8));
        b.setLayoutParams(lp);
        return b;
    }

    static View card(Context c, View child) {
        LinearLayout box = column(c);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(c, 10));
        bg.setColor(0xFFF1F5F1);
        box.setBackground(bg);
        int p = dp(c, 12);
        box.setPadding(p, p, p, p);
        box.addView(child);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(c, 4), 0, dp(c, 4));
        box.setLayoutParams(lp);
        return box;
    }

    /** A 4x3 phone keypad. Reports press and release so callers can hold DTMF tones. */
    @SuppressLint("ClickableViewAccessibility")
    static GridLayout keypad(Context c, int textColor, KeyListener listener) {
        GridLayout g = new GridLayout(c);
        g.setColumnCount(3);
        g.setUseDefaultMargins(false);
        String keys = "123456789*0#";
        for (int i = 0; i < keys.length(); i++) {
            char k = keys.charAt(i);
            Button b = new Button(c, null, android.R.attr.borderlessButtonStyle);
            b.setText(String.valueOf(k));
            b.setTextSize(26);
            b.setTextColor(textColor);
            b.setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    v.setPressed(true);
                    listener.onKeyDown(k);
                } else if (e.getAction() == MotionEvent.ACTION_UP
                        || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    v.setPressed(false);
                    listener.onKeyUp(k);
                }
                return true;
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                    GridLayout.spec(i / 3, 1f), GridLayout.spec(i % 3, 1f));
            lp.width = 0;
            lp.height = dp(c, 64);
            b.setLayoutParams(lp);
            g.addView(b);
        }
        return g;
    }
}
