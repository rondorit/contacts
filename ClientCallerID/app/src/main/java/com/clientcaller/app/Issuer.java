package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Quote issuers ("from" details on quotes): a list of profiles, one of them the default. */
final class Issuer {
    static final String[] KEYS = {"iss_name", "iss_title", "iss_org", "iss_phone", "iss_email", "iss_tax"};
    static final String[] LABELS = {"\u05E9\u05DD", "\u05EA\u05E4\u05E7\u05D9\u05D3", "\u05D0\u05E8\u05D2\u05D5\u05DF", "\u05D8\u05DC\u05E4\u05D5\u05DF", "\u05DE\u05D9\u05D9\u05DC", "\u05D7.\u05E4 / \u05E2\u05D5\u05E1\u05E7 \u05DE\u05D5\u05E8\u05E9\u05D4"};
    static final String[] DEFAULTS = {"\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF", "\u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA", "Beki Projects", "", "", ""};
    private static final String KEY_LIST = "issuers";
    private static final String KEY_DEFAULT = "issuer_default";

    static final class Profile {
        String id = "";
        final String[] v = new String[KEYS.length];

        Profile() {
            for (int i = 0; i < v.length; i++) v[i] = "";
        }

        String label() {
            String s = v[0];
            if (!v[2].isEmpty()) s += (s.isEmpty() ? "" : " \u2013 ") + v[2];
            return s.isEmpty() ? "(\u05DC\u05DC\u05D0 \u05E9\u05DD)" : s;
        }
    }

    private Issuer() { }

    /** All profiles. The first time, the single old issuer becomes the first profile. */
    static List<Profile> list(Context c) {
        List<Profile> out = new ArrayList<>();
        String raw = CallLogger.prefs(c).getString(KEY_LIST, null);
        if (raw == null) {
            Profile p = new Profile();
            p.id = "1";
            for (int i = 0; i < KEYS.length; i++) p.v[i] = CallLogger.prefs(c).getString(KEYS[i], DEFAULTS[i]);
            out.add(p);
            save(c, out);
            CallLogger.prefs(c).edit().putString(KEY_DEFAULT, "1").apply();
            return out;
        }
        if (!CallLogger.prefs(c).getBoolean("issuer_fix_beki", false)) {
            // The app now belongs to Beki Projects instead of Kibbutz Metzer.
            raw = raw.replace("\u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8", "Beki Projects");
            CallLogger.prefs(c).edit().putString(KEY_LIST, raw).putBoolean("issuer_fix_beki", true).apply();
        }
        try {
            JSONArray a = new JSONArray(raw);
            for (int k = 0; k < a.length(); k++) {
                JSONObject o = a.getJSONObject(k);
                Profile p = new Profile();
                p.id = o.optString("id");
                for (int i = 0; i < KEYS.length; i++) p.v[i] = o.optString(KEYS[i]);
                out.add(p);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static void save(Context c, List<Profile> list) {
        JSONArray a = new JSONArray();
        try {
            for (Profile p : list) {
                JSONObject o = new JSONObject().put("id", p.id);
                for (int i = 0; i < KEYS.length; i++) o.put(KEYS[i], p.v[i]);
                a.put(o);
            }
        } catch (Exception ignored) { }
        CallLogger.prefs(c).edit().putString(KEY_LIST, a.toString()).apply();
    }

    static String defaultId(Context c) {
        List<Profile> all = list(c);
        String id = CallLogger.prefs(c).getString(KEY_DEFAULT, "");
        for (Profile p : all) if (p.id.equals(id)) return id;
        return all.isEmpty() ? "" : all.get(0).id;
    }

    /** The profile with this id, else the default one (never null). */
    static Profile byId(Context c, String id) {
        List<Profile> all = list(c);
        String want = id == null || id.isEmpty() ? defaultId(c) : id;
        for (Profile p : all) if (p.id.equals(want)) return p;
        String def = defaultId(c);
        for (Profile p : all) if (p.id.equals(def)) return p;
        Profile p = new Profile();
        System.arraycopy(DEFAULTS, 0, p.v, 0, DEFAULTS.length);
        return p;
    }

    static Profile forQuote(Context c, QuoteDb.Quote q) {
        return byId(c, q == null ? "" : q.issuer);
    }

    /** Default profile's field (kept for older callers). */
    static String get(Context c, int i) {
        return byId(c, "").v[i];
    }

    static String signature(Context c) {
        return signature(byId(c, ""));
    }

    static String signature(Profile p) {
        StringBuilder sb = new StringBuilder("\u05D1\u05D1\u05E8\u05DB\u05D4,");
        for (int i = 0; i < KEYS.length; i++) {
            String v = p.v[i];
            if (v.isEmpty()) continue;
            sb.append('\n');
            if (i == 3) sb.append("\u05D8\u05DC': ");
            if (i == 5) sb.append("\u05D7.\u05E4: ");
            sb.append(v);
        }
        return sb.toString();
    }

    /** Lines for the PDF "from" block. */
    static String block(Profile p) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < KEYS.length; i++) {
            String v = p.v[i];
            if (v.isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            if (i == 3) sb.append("\u05D8\u05DC': ");
            if (i == 5) sb.append("\u05D7.\u05E4: ");
            sb.append(v);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ UI

    interface Picked { void on(String id); }

    /** Pick an issuer for a quote; also lets you add or edit profiles. */
    static void choose(final Activity a, String current, final Picked cb) {
        final List<Profile> all = list(a);
        final String def = defaultId(a);
        String[] items = new String[all.size() + 2];
        for (int i = 0; i < all.size(); i++) {
            Profile p = all.get(i);
            items[i] = (p.id.equals(current) ? "\u2713 " : "\uD83E\uDDFE ") + p.label() + (p.id.equals(def) ? "  \u2B50" : "");
        }
        items[all.size()] = "\u2795 \u05DE\u05E6\u05D9\u05E2 \u05D7\u05D3\u05E9";
        items[all.size() + 1] = "\u270F\uFE0F \u05E2\u05E8\u05D9\u05DB\u05EA \u05D4\u05E8\u05E9\u05D9\u05DE\u05D4";
        new AlertDialog.Builder(a)
                .setTitle("\u05E4\u05E8\u05D8\u05D9 \u05D4\u05DE\u05E6\u05D9\u05E2 \u05DC\u05D4\u05E6\u05E2\u05D4")
                .setItems(items, (d, w) -> {
                    if (w < all.size()) cb.on(all.get(w).id);
                    else if (w == all.size()) edit(a, null, () -> {
                        List<Profile> now = list(a);
                        cb.on(now.get(now.size() - 1).id);
                    });
                    else manage(a, () -> choose(a, current, cb));
                })
                .show();
    }

    /** List of profiles: tap to edit, delete, or make default. */
    static void manage(final Activity a, final Runnable after) {
        final List<Profile> all = list(a);
        final String def = defaultId(a);
        String[] items = new String[all.size() + 1];
        for (int i = 0; i < all.size(); i++) items[i] = "\uD83E\uDDFE " + all.get(i).label() + (all.get(i).id.equals(def) ? "  \u2B50 \u05D1\u05E8\u05D9\u05E8\u05EA \u05DE\u05D7\u05D3\u05DC" : "");
        items[all.size()] = "\u2795 \u05DE\u05E6\u05D9\u05E2 \u05D7\u05D3\u05E9";
        new AlertDialog.Builder(a)
                .setTitle("\u05E8\u05E9\u05D9\u05DE\u05EA \u05DE\u05E6\u05D9\u05E2\u05D9\u05DD")
                .setItems(items, (d, w) -> edit(a, w < all.size() ? all.get(w) : null, () -> manage(a, after)))
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", (d, w) -> { if (after != null) after.run(); })
                .show();
    }

    private static void edit(final Activity a, final Profile existing, final Runnable after) {
        final Profile p = existing != null ? existing : new Profile();
        LinearLayout lay = new LinearLayout(a);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final EditText[] fields = new EditText[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            lay.addView(Ui.text(a, LABELS[i], 12, Ui.MUTED, false));
            EditText e = new EditText(a);
            e.setInputType(i == 3 ? InputType.TYPE_CLASS_PHONE : i == 4
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_TEXT);
            e.setText(p.v[i]);
            lay.addView(e);
            fields[i] = e;
        }
        ScrollView sv = new ScrollView(a);
        sv.addView(lay);
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(existing == null ? "\u05DE\u05E6\u05D9\u05E2 \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05DE\u05E6\u05D9\u05E2")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    for (int i = 0; i < KEYS.length; i++) p.v[i] = fields[i].getText().toString().trim();
                    if (p.v[0].isEmpty() && p.v[2].isEmpty()) {
                        Toast.makeText(a, "\u05D7\u05E1\u05E8 \u05E9\u05DD \u05D0\u05D5 \u05D0\u05E8\u05D2\u05D5\u05DF", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Profile> all = list(a);
                    if (existing == null) {
                        p.id = String.valueOf(System.currentTimeMillis());
                        all.add(p);
                    } else {
                        for (int k = 0; k < all.size(); k++) if (all.get(k).id.equals(p.id)) all.set(k, p);
                    }
                    save(a, all);
                    if (after != null) after.run();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (existing != null) {
            b.setNeutralButton("\u05E2\u05D5\u05D3\u2026", (d, w) -> new AlertDialog.Builder(a)
                    .setItems(new String[]{"\u2B50 \u05E7\u05D1\u05E2 \u05DB\u05D1\u05E8\u05D9\u05E8\u05EA \u05DE\u05D7\u05D3\u05DC", "\uD83D\uDDD1\uFE0F \u05DE\u05D7\u05E7"}, (d2, w2) -> {
                        List<Profile> all = list(a);
                        if (w2 == 0) {
                            CallLogger.prefs(a).edit().putString(KEY_DEFAULT, p.id).apply();
                            if (after != null) after.run();
                        } else if (all.size() <= 1) {
                            Toast.makeText(a, "\u05E6\u05E8\u05D9\u05DA \u05DC\u05D4\u05E9\u05D0\u05D9\u05E8 \u05DC\u05E4\u05D7\u05D5\u05EA \u05DE\u05E6\u05D9\u05E2 \u05D0\u05D7\u05D3", Toast.LENGTH_SHORT).show();
                        } else {
                            Auth.confirm(a, "\u05DE\u05D7\u05D9\u05E7\u05EA " + p.label(), () -> {
                                List<Profile> now = list(a);
                                for (int k = 0; k < now.size(); k++) if (now.get(k).id.equals(p.id)) { now.remove(k); break; }
                                save(a, now);
                                if (after != null) after.run();
                            });
                        }
                    })
                    .show());
        }
        b.show();
    }
}
