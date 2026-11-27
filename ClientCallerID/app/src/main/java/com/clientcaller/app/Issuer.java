package com.clientcaller.app;

import android.content.Context;
import android.content.SharedPreferences;

/** The quote issuer's details (shown on quotes). Editable in the app. */
final class Issuer {
    static final String[] KEYS = {"iss_name", "iss_title", "iss_org", "iss_phone", "iss_email", "iss_tax"};
    static final String[] LABELS = {"\u05E9\u05DD", "\u05EA\u05E4\u05E7\u05D9\u05D3", "\u05D0\u05E8\u05D2\u05D5\u05DF", "\u05D8\u05DC\u05E4\u05D5\u05DF", "\u05DE\u05D9\u05D9\u05DC", "\u05D7.\u05E4 / \u05E2\u05D5\u05E1\u05E7 \u05DE\u05D5\u05E8\u05E9\u05D4"};
    static final String[] DEFAULTS = {"\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF", "\u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA", "\u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8", "", "", ""};

    private Issuer() { }

    static String get(Context c, int i) {
        return CallLogger.prefs(c).getString(KEYS[i], DEFAULTS[i]);
    }

    static void set(Context c, int i, String v) {
        CallLogger.prefs(c).edit().putString(KEYS[i], v).apply();
    }

    static String signature(Context c) {
        StringBuilder sb = new StringBuilder("\u05D1\u05D1\u05E8\u05DB\u05D4,");
        for (int i = 0; i < KEYS.length; i++) {
            String v = get(c, i);
            if (v.isEmpty()) continue;
            sb.append('\n');
            if (i == 3) sb.append("\u05D8\u05DC': ");
            if (i == 5) sb.append("\u05D7.\u05E4: ");
            sb.append(v);
        }
        return sb.toString();
    }
}
