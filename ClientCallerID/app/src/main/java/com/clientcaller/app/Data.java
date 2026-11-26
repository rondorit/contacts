package com.clientcaller.app;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Keeps the imported client list in memory and on the phone's private storage. */
final class Data {
    static final String FILE = "clients.tsv";
    static final String KEY_PHONE_COLS = "phone_cols";  // header names, tab separated
    static final String KEY_NAME_COL = "name_col";      // header name
    private static ClientStore cache;

    private Data() { }

    static synchronized ClientStore get(Context ctx) {
        if (cache == null) {
            cache = new ClientStore();
            File f = new File(ctx.getApplicationContext().getFilesDir(), FILE);
            if (f.exists()) {
                try { cache.loadFile(f); } catch (Exception ignored) { }
            }
            applyPhoneColumns(ctx, cache);
        }
        return cache;
    }

    static synchronized void set(Context ctx, ClientStore s) {
        applyPhoneColumns(ctx, s);
        cache = s;
    }

    /** If the user chose the phone columns by hand, use those (matched by column title). */
    static void applyPhoneColumns(Context ctx, ClientStore s) {
        String nameCol = CallLogger.prefs(ctx).getString(KEY_NAME_COL, "");
        if (!nameCol.isEmpty() && s.headers.indexOf(nameCol) >= 0) s.setNameColumn(s.headers.indexOf(nameCol));
        String saved = CallLogger.prefs(ctx).getString(KEY_PHONE_COLS, "");
        if (saved.isEmpty()) return;
        List<Integer> cols = new ArrayList<>();
        for (String name : saved.split("\t")) {
            int i = s.headers.indexOf(name);
            if (i >= 0) cols.add(i);
        }
        if (!cols.isEmpty()) s.setPhoneColumns(cols);
        if (!nameCol.isEmpty() && s.headers.indexOf(nameCol) >= 0) s.setNameColumn(s.headers.indexOf(nameCol));
    }

    static synchronized void saveNameColumn(Context ctx, String name) {
        CallLogger.prefs(ctx).edit().putString(KEY_NAME_COL, name).apply();
        if (cache != null) applyPhoneColumns(ctx, cache);
    }

    static synchronized void savePhoneColumns(Context ctx, List<String> names) {
        CallLogger.prefs(ctx).edit().putString(KEY_PHONE_COLS, String.join("\t", names)).apply();
        if (cache != null) {
            // Reload so automatic detection doesn't mix with the new choice.
            ClientStore s = new ClientStore();
            File f = new File(ctx.getApplicationContext().getFilesDir(), FILE);
            try { if (f.exists()) s.loadFile(f); } catch (Exception ignored) { }
            applyPhoneColumns(ctx, s);
            cache = s;
        }
    }
}
