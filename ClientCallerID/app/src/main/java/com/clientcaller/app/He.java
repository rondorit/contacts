package com.clientcaller.app;

import android.app.Activity;
import android.view.View;

/** Hebrew labels for the values stored in English, and right-to-left layout. */
final class He {
    private He() { }

    static final String[] STATUS = {"\u05E4\u05EA\u05D5\u05D7", "\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05DC\u05E7\u05D5\u05D7", "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5", "\u05E1\u05D2\u05D5\u05E8"};
    static final String[] PRIORITY = {"\u05E0\u05DE\u05D5\u05DB\u05D4", "\u05E8\u05D2\u05D9\u05DC\u05D4", "\u05D3\u05D7\u05D5\u05E4\u05D4"};
    static final String[] CATEGORY = {"\u05D7\u05D5\u05DE\u05E8\u05D4", "\u05EA\u05D5\u05DB\u05E0\u05D4", "\u05E8\u05E9\u05EA", "\u05DE\u05D3\u05E4\u05E1\u05EA", "\u05D3\u05D5\u05D0\u05E8 \u05D0\u05DC\u05E7\u05D8\u05E8\u05D5\u05E0\u05D9", "\u05D2\u05D9\u05D1\u05D5\u05D9", "\u05D0\u05D1\u05D8\u05D7\u05D4", "\u05D0\u05D7\u05E8"};

    static String status(String key) { return map(TicketDb.STATUSES, STATUS, key); }
    static String priority(String key) { return map(TicketDb.PRIORITIES, PRIORITY, key); }
    static String category(String key) { return map(TicketDb.CATEGORIES, CATEGORY, key); }

    private static String map(String[] keys, String[] he, String key) {
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(key)) return he[i];
        return key;
    }

    static void rtl(Activity a) {
        a.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }
}
