package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** Navigation to a client's address: Waze, Google Maps or any navigation app. */
final class Nav {
    static final String KEY_COL = "addr_col";
    static final String KEY_SUFFIX = "addr_suffix";

    private Nav() { }

    /** The address column: chosen by the user, or one whose title looks like an address. */
    static int column(Context c, ClientStore s) {
        String saved = CallLogger.prefs(c).getString(KEY_COL, "");
        if (!saved.isEmpty()) return s.headers.indexOf(saved);
        for (String w : new String[]{"\u05DB\u05EA\u05D5\u05D1\u05EA", "address", "\u05E8\u05D7\u05D5\u05D1", "\u05DE\u05D9\u05E7\u05D5\u05DD"}) {
            for (int i = 0; i < s.headers.size(); i++) {
                if (s.headers.get(i).toLowerCase(java.util.Locale.ROOT).contains(w)) return i;
            }
        }
        return -1;
    }

    static String address(Context c, ClientStore s, int row) {
        int col = column(c, s);
        if (col < 0 || row < 0 || row >= s.rows.size()) return "";
        String a = s.rows.get(row).get(col).trim();
        if (a.isEmpty()) return "";
        String suffix = CallLogger.prefs(c).getString(KEY_SUFFIX, "").trim();
        return suffix.isEmpty() || a.contains(suffix) ? a : a + ", " + suffix;
    }

    /** Lets the user pick the address column and an optional text to add (e.g. the kibbutz name). */
    static void chooseColumn(final Activity a, final Runnable after) {
        final ClientStore s = Data.get(a);
        if (s.headers.isEmpty()) {
            Toast.makeText(a, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] names = s.headers.toArray(new String[0]);
        new AlertDialog.Builder(a)
                .setTitle("\u05D1\u05D0\u05D9\u05D6\u05D5 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05E0\u05DE\u05E6\u05D0\u05EA \u05D4\u05DB\u05EA\u05D5\u05D1\u05EA?")
                .setSingleChoiceItems(names, column(a, s), (d, which) -> {
                    d.dismiss();
                    CallLogger.prefs(a).edit().putString(KEY_COL, names[which]).apply();
                    final android.widget.EditText suffix = new android.widget.EditText(a);
                    suffix.setText(CallLogger.prefs(a).getString(KEY_SUFFIX, ""));
                    suffix.setHint("\u05DC\u05DE\u05E9\u05DC: \u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8");
                    android.widget.LinearLayout box = new android.widget.LinearLayout(a);
                    int p = Ui.dp(a, 20);
                    box.setPadding(p, Ui.dp(a, 8), p, 0);
                    box.addView(suffix, new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
                    new AlertDialog.Builder(a)
                            .setTitle("\u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05DC\u05DB\u05DC \u05DB\u05EA\u05D5\u05D1\u05EA?")
                            .setMessage("\u05D0\u05DD \u05D1\u05E2\u05DE\u05D5\u05D3\u05D4 \u05D9\u05E9 \u05E8\u05E7 \u05E8\u05D7\u05D5\u05D1 \u05D5\u05DE\u05E1\u05E4\u05E8 \u05D1\u05D9\u05EA, \u05D4\u05D5\u05E1\u05E3 \u05D0\u05EA \u05E9\u05DD \u05D4\u05D9\u05D9\u05E9\u05D5\u05D1 \u05DB\u05D3\u05D9 \u05E9\u05D4\u05E0\u05D9\u05D5\u05D5\u05D8 \u05D9\u05DE\u05E6\u05D0 \u05D0\u05EA \u05D4\u05DE\u05E7\u05D5\u05DD.")
                            .setView(box)
                            .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d2, w2) -> {
                                CallLogger.prefs(a).edit().putString(KEY_SUFFIX, suffix.getText().toString().trim()).apply();
                                if (after != null) after.run();
                            })
                            .setNegativeButton("\u05D1\u05DC\u05D9 \u05EA\u05D5\u05E1\u05E4\u05EA", (d2, w2) -> {
                                CallLogger.prefs(a).edit().putString(KEY_SUFFIX, "").apply();
                                if (after != null) after.run();
                            })
                            .show();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    static void choose(final Activity a, final String address) {
        if (address.isEmpty()) {
            Toast.makeText(a, "\u05D0\u05D9\u05DF \u05DB\u05EA\u05D5\u05D1\u05EA \u05DC\u05DC\u05E7\u05D5\u05D7", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] opts = {"\uD83D\uDE97 Waze", "\uD83D\uDDFA\uFE0F Google Maps", "\uD83D\uDCCD \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05D0\u05D7\u05E8\u05EA\u2026"};
        new AlertDialog.Builder(a)
                .setTitle("\u05E0\u05D9\u05D5\u05D5\u05D8 \u05D0\u05DC: " + address)
                .setItems(opts, (d, w) -> {
                    if (w == 0) waze(a, address);
                    else if (w == 1) maps(a, address);
                    else any(a, address);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    static void waze(Activity a, String address) {
        open(a, new Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=" + Uri.encode(address) + "&navigate=yes")));
    }

    static void maps(Activity a, String address) {
        open(a, new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                + Uri.encode(address))));
    }

    static void any(Activity a, String address) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(address)));
        open(a, Intent.createChooser(i, "\u05E0\u05D9\u05D5\u05D5\u05D8 \u05E2\u05DD"));
    }

    private static void open(Activity a, Intent i) {
        try {
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D9\u05EA \u05E0\u05D9\u05D5\u05D5\u05D8", Toast.LENGTH_SHORT).show();
        }
    }
}
