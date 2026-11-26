package com.clientcaller.app;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The fault categories list, editable in the app (saved on the phone). */
final class Categories {
    private static final String KEY = "categories";
    private static final String[] DEFAULTS = {
            "\u05D7\u05D5\u05DE\u05E8\u05D4", "\u05EA\u05D5\u05DB\u05E0\u05D4", "\u05E8\u05E9\u05EA", "\u05DE\u05D3\u05E4\u05E1\u05EA", "\u05D3\u05D5\u05D0\u05E8 \u05D0\u05DC\u05E7\u05D8\u05E8\u05D5\u05E0\u05D9", "\u05D2\u05D9\u05D1\u05D5\u05D9", "\u05D0\u05D1\u05D8\u05D7\u05D4", "\u05E9\u05D9\u05E8\u05D5\u05EA\u05D9 \u05D8\u05DC\u05D5\u05D5\u05D9\u05D6\u05D9\u05D4", "\u05D0\u05D7\u05E8"};

    private Categories() { }

    static List<String> list(Context c) {
        String saved = CallLogger.prefs(c).getString(KEY, null);
        if (saved == null) return new ArrayList<>(Arrays.asList(DEFAULTS));
        List<String> out = new ArrayList<>();
        for (String s : saved.split("\t")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    static void save(Context c, List<String> items) {
        CallLogger.prefs(c).edit().putString(KEY, String.join("\t", items)).apply();
    }
}
