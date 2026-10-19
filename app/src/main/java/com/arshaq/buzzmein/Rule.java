package com.arshaq.buzzmein;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/** One caller to intercept: when {@link #number} rings, answer and send {@link #digits}. */
final class Rule {
    long id = System.currentTimeMillis();
    String label = "Building buzzer";
    String number = "";
    /** DTMF keys to send. ',' inserts a one-second pause. */
    String digits = "9";
    boolean enabled = true;
    /** Active window in minutes since midnight. start == end means all day. */
    int windowStart = 0;
    int windowEnd = 0;
    int answerDelayMs = 500;
    int dtmfDelayMs = 1500;
    int hangupDelayMs = 2500;

    boolean matches(String incoming, Calendar now) {
        return enabled && samePhoneNumber(number, incoming) && inWindow(now);
    }

    boolean inWindow(Calendar now) {
        if (windowStart == windowEnd) return true;
        int m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        if (windowStart < windowEnd) return m >= windowStart && m < windowEnd;
        return m >= windowStart || m < windowEnd; // wraps past midnight
    }

    String describeWindow() {
        if (windowStart == windowEnd) return "all day";
        return formatTime(windowStart) + "–" + formatTime(windowEnd);
    }

    static String formatTime(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** Parses "6:30" / "06:30" / "6". Returns -1 if invalid. */
    static int parseTime(String s) {
        s = s.trim();
        if (s.isEmpty()) return -1;
        try {
            String[] parts = s.split(":");
            int h = Integer.parseInt(parts[0].trim());
            int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            if (h < 0 || h > 23 || m < 0 || m > 59) return -1;
            return h * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String digitsOnly(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) if (c >= '0' && c <= '9') b.append(c);
        return b.toString();
    }

    /** Tolerates country-code / formatting differences, e.g. +1 604-555-0100 vs 6045550100. */
    static boolean samePhoneNumber(String a, String b) {
        String x = digitsOnly(a), y = digitsOnly(b);
        if (x.isEmpty() || y.isEmpty()) return false;
        int shortest = Math.min(x.length(), y.length());
        if (shortest < 7) return x.equals(y);
        int n = Math.min(10, shortest);
        return x.substring(x.length() - n).equals(y.substring(y.length() - n));
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id).put("label", label).put("number", number).put("digits", digits)
                .put("enabled", enabled).put("windowStart", windowStart).put("windowEnd", windowEnd)
                .put("answerDelayMs", answerDelayMs).put("dtmfDelayMs", dtmfDelayMs)
                .put("hangupDelayMs", hangupDelayMs);
    }

    static Rule fromJson(JSONObject o) {
        Rule r = new Rule();
        r.id = o.optLong("id", r.id);
        r.label = o.optString("label", r.label);
        r.number = o.optString("number", "");
        r.digits = o.optString("digits", r.digits);
        r.enabled = o.optBoolean("enabled", true);
        r.windowStart = o.optInt("windowStart", 0);
        r.windowEnd = o.optInt("windowEnd", 0);
        r.answerDelayMs = o.optInt("answerDelayMs", r.answerDelayMs);
        r.dtmfDelayMs = o.optInt("dtmfDelayMs", r.dtmfDelayMs);
        r.hangupDelayMs = o.optInt("hangupDelayMs", r.hangupDelayMs);
        return r;
    }
}
