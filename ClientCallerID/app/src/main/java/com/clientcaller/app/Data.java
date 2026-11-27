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

    static final String KEY_EDITED = "clients_edited";  // changed in the app since last import/export

    /** Saves the client list after adding/editing/deleting in the app. */
    static synchronized void saveEdits(Context ctx) throws java.io.IOException {
        ClientStore s = get(ctx);
        s.saveFile(new File(ctx.getApplicationContext().getFilesDir(), FILE));
        CallLogger.prefs(ctx).edit().putBoolean(KEY_EDITED, true).apply();
    }

    static final String KEY_HIDDEN = "hidden_cols";   // column titles not shown in the client card

    static java.util.List<String> hiddenColumns(Context ctx) {
        String saved = CallLogger.prefs(ctx).getString(KEY_HIDDEN, "\u05DE\u05D6\u05D4\u05D4");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String x : saved.split("\t")) if (!x.trim().isEmpty()) out.add(x.trim());
        return out;
    }

    static void saveHiddenColumns(Context ctx, java.util.List<String> names) {
        CallLogger.prefs(ctx).edit().putString(KEY_HIDDEN, String.join("\t", names)).apply();
    }

    static final String KEY_ACC_TABLE = "access_table";
    static final String KEY_ACC_URI = "access_uri";
    static final String KEY_ACC_EDITS = "acc_edits";
    static final String KEY_ACC_DELETES = "acc_deletes";
    static final String ACCESS_COPY = "source.accdb";
    static final String ID_COL = "\u05DE\u05D6\u05D4\u05D4";

    /** "i:<id>" when the client has an ID (\u05DE\u05D6\u05D4\u05D4), otherwise "n:<name>". */
    static String identity(ClientStore s, int r) {
        int ic = s.headers.indexOf(ID_COL);
        String id = ic >= 0 ? s.rows.get(r).get(ic).trim() : "";
        return id.isEmpty() ? "n:" + s.name(r) : "i:" + AccessExport.norm(id);
    }

    static java.util.List<String> list(Context ctx, String key) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String x : CallLogger.prefs(ctx).getString(key, "").split("\t")) if (!x.isEmpty()) out.add(x);
        return out;
    }

    private static void putList(Context ctx, String key, java.util.List<String> l) {
        CallLogger.prefs(ctx).edit().putString(key, String.join("\t", l)).apply();
    }

    static boolean fromAccess(Context ctx) {
        return !CallLogger.prefs(ctx).getString(KEY_ACC_TABLE, "").isEmpty();
    }

    static void recordEdit(Context ctx, String ident) {
        if (!fromAccess(ctx)) return;
        java.util.List<String> l = list(ctx, KEY_ACC_EDITS);
        if (!l.contains(ident)) l.add(ident);
        putList(ctx, KEY_ACC_EDITS, l);
    }

    static void recordDelete(Context ctx, String ident) {
        if (!fromAccess(ctx)) return;
        java.util.List<String> e = list(ctx, KEY_ACC_EDITS);
        e.remove(ident);
        putList(ctx, KEY_ACC_EDITS, e);
        if (ident.startsWith("n:")) return;   // never saved to Access (no ID yet)
        java.util.List<String> d = list(ctx, KEY_ACC_DELETES);
        if (!d.contains(ident)) d.add(ident);
        putList(ctx, KEY_ACC_DELETES, d);
    }

    static void clearAccessChanges(Context ctx) {
        CallLogger.prefs(ctx).edit().remove(KEY_ACC_EDITS).remove(KEY_ACC_DELETES).apply();
    }

    static synchronized void reset() {
        cache = null;
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
