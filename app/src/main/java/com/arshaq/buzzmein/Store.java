package com.arshaq.buzzmein;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Persists rules, the master switch, and a short activity log. */
final class Store {
    private static final int MAX_LOG = 50;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("buzz", Context.MODE_PRIVATE);
    }

    static boolean isArmed(Context c) {
        return prefs(c).getBoolean("armed", true);
    }

    static void setArmed(Context c, boolean armed) {
        prefs(c).edit().putBoolean("armed", armed).apply();
    }

    static List<Rule> rules(Context c) {
        List<Rule> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("rules", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(Rule.fromJson(a.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
        return out;
    }

    static void saveRules(Context c, List<Rule> rules) {
        JSONArray a = new JSONArray();
        try {
            for (Rule r : rules) a.put(r.toJson());
        } catch (JSONException ignored) {
        }
        prefs(c).edit().putString("rules", a.toString()).apply();
    }

    static void upsert(Context c, Rule rule) {
        List<Rule> rules = rules(c);
        boolean replaced = false;
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).id == rule.id) {
                rules.set(i, rule);
                replaced = true;
            }
        }
        if (!replaced) rules.add(rule);
        saveRules(c, rules);
    }

    static void delete(Context c, long id) {
        List<Rule> rules = rules(c);
        rules.removeIf(r -> r.id == id);
        saveRules(c, rules);
    }

    /** Returns the rule that should auto-handle a call from {@code number} right now, or null. */
    static Rule match(Context c, String number) {
        if (!isArmed(c)) return null;
        Calendar now = Calendar.getInstance();
        for (Rule r : rules(c)) if (r.matches(number, now)) return r;
        return null;
    }

    static void log(Context c, String message) {
        String stamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date());
        List<String> entries = logEntries(c);
        entries.add(0, stamp + "  " + message);
        while (entries.size() > MAX_LOG) entries.remove(entries.size() - 1);
        prefs(c).edit().putString("log", new JSONArray(entries).toString()).apply();
    }

    static List<String> logEntries(Context c) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("log", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (JSONException ignored) {
        }
        return out;
    }

    static void clearLog(Context c) {
        prefs(c).edit().remove("log").apply();
    }
}
