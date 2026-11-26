package com.clientcaller.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Message templates for outsourcing providers, with {fields} filled from the ticket and the Excel row. */
final class Templates {
    private static final String KEY = "templates";

    static final class Tpl {
        String name = "", subject = "", body = "";
    }

    /** Fields taken from the ticket. */
    static final String[] TICKET_FIELDS = {
            "{\u05DE\u05E1\u05E4\u05E8 \u05E7\u05E8\u05D9\u05D0\u05D4}", "{\u05DC\u05E7\u05D5\u05D7}", "{\u05D8\u05DC\u05E4\u05D5\u05DF}", "{\u05E0\u05D5\u05E9\u05D0}", "{\u05EA\u05D9\u05D0\u05D5\u05E8}", "{\u05E6\u05D9\u05D5\u05D3}", "{MAC}",
            "{\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4}", "{\u05D3\u05D7\u05D9\u05E4\u05D5\u05EA}", "{\u05E1\u05D8\u05D8\u05D5\u05E1}", "{\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA}", "{\u05E4\u05E8\u05D8\u05D9 \u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5}",
            "{\u05DE\u05E2\u05E7\u05D1}", "{\u05EA\u05D0\u05E8\u05D9\u05DA \u05E4\u05EA\u05D9\u05D7\u05D4}", "{\u05D4\u05D9\u05D5\u05DD}", "{\u05D4\u05E2\u05E8\u05D5\u05EA}"};

    private Templates() { }

    private static final String SEEDED = "templates_seed_1";

    /** Built-in templates added once (the user can edit or delete them). */
    private static List<Tpl> builtIn() {
        List<Tpl> out = new ArrayList<>();
        Tpl t = new Tpl();
        t.name = "\u05D4\u05D7\u05DC\u05E4\u05EA \u05DE\u05DE\u05D9\u05E8 \u05D1\u05E1\u05D8\u05E8\u05D9\u05DE\u05E8";
        t.subject = "\u05D1\u05E7\u05E9\u05EA \u05DC\u05E7\u05D5\u05D7 \u05DC\u05D4\u05D7\u05DC\u05E4\u05EA \u05DE\u05DE\u05D9\u05E8 \u05D1\u05E1\u05D8\u05E8\u05D9\u05DE\u05E8 - {\u05DC\u05E7\u05D5\u05D7} - \u05D0\u05EA\u05E8 {\u05D0\u05EA\u05E8}";
        t.body = "\u05E9\u05DC\u05D5\u05DD \u05E8\u05D1.\n\u05DC\u05D4\u05DC\u05DF, \u05D1\u05E7\u05E9\u05EA \u05DC\u05E7\u05D5\u05D7 \u05DC\u05D4\u05D7\u05DC\u05E4\u05EA \u05DE\u05DE\u05D9\u05E8 \u05D1\u05E1\u05D8\u05E8\u05D9\u05DE\u05E8\n\n\u05E9\u05DD: {\u05DC\u05E7\u05D5\u05D7}\n\u05DE\u05E1\u05E4\u05E8 \u05D0\u05EA\u05E8: {\u05D0\u05EA\u05E8}\n\u05D8\u05DC\u05E4\u05D5\u05DF: {\u05D8\u05DC\u05E4\u05D5\u05DF}\n\n\u05DE\u05E1\u05E4\u05E8\u05D9 \u05D4\u05DB\u05E8\u05D8\u05D9\u05E1\u05D9\u05DD \u05D1\u05DE\u05DE\u05D9\u05E8\u05D9\u05DD \u05D4\u05E7\u05D9\u05D9\u05DE\u05D9\u05DD:\n1. 0002-\n2. 0002-\n3. \n\n\u05E1\u05D4\"\u05DB, \u05D4\u05D7\u05DC\u05E4\u05EA ___ \u05DE\u05DE\u05D9\u05E8\u05D9\u05DD \u05D1\u05E1\u05D8\u05E8\u05D9\u05DE\u05E8\u05D9\u05DD, \u05DB\u05D5\u05DC\u05DC \u05D9\u05E6\u05D9\u05E8\u05EA/\u05E2\u05D3\u05DB\u05D5\u05DF \u05D4\u05DE\u05E0\u05D5\u05D9 \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4.\n\n[\u05D0\u05DD \u05D9\u05E9 \u05E6\u05D9\u05D5\u05D3 \u05E0\u05D5\u05E1\u05E3 \u05DC\u05D1\u05D9\u05D8\u05D5\u05DC: \u05E0\u05D0 \u05DC\u05D1\u05D8\u05DC \u05D2\u05DD \u05DE\u05DE\u05D9\u05E8/\u05E6\u05D9\u05D5\u05D3 \u05DE\u05E1\u05E4\u05E8 ______].\n\n\u05DC\u05EA\u05D0\u05DD \u05D1\u05D1\u05E7\u05E9\u05D4 \u05D0\u05EA \u05D4\u05D4\u05D7\u05DC\u05E4\u05D4/\u05D4\u05D0\u05D9\u05E1\u05D5\u05E3 \u05E2\u05DD \u05E9\u05DC\u05D9\u05D7 \u05D9\u05E9\u05D9\u05E8\u05D5\u05EA \u05DE\u05D5\u05DC \u05D4\u05DC\u05E7\u05D5\u05D7 \u05D1\u05E0\u05D9\u05D9\u05D3 \u05E9\u05DC\u05D5.\n\n\u05D1\u05D1\u05E8\u05DB\u05D4 \u05D5\u05EA\u05D5\u05D3\u05D4,\n\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF\n\u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA\n\u05E7.\u05DE\u05E6\u05E8";
        out.add(t);
        t = new Tpl();
        t.name = "\u05DE\u05E0\u05D5\u05D9 \u05D7\u05D3\u05E9";
        t.subject = "\u05D1\u05E7\u05E9\u05D4 \u05DC\u05D9\u05E6\u05D9\u05E8\u05EA \u05DE\u05E0\u05D5\u05D9 \u05D7\u05D3\u05E9 - {\u05DC\u05E7\u05D5\u05D7}";
        t.body = "\u05E9\u05DC\u05D5\u05DD \u05E8\u05D1.\n\n\u05D0\u05D1\u05E7\u05E9 \u05DC\u05D9\u05E6\u05D5\u05E8 \u05DE\u05E0\u05D5\u05D9 \u05D7\u05D3\u05E9:\n\n\u05E1\u05D5\u05D2 \u05DE\u05E1\u05DC\u05D5\u05DC: \n\u05E9\u05DD: {\u05DC\u05E7\u05D5\u05D7}\n\u05EA\u05E2\u05D5\u05D3\u05EA \u05D6\u05D4\u05D5\u05EA: {\u05D6\u05D4\u05D5\u05EA}\n\u05D8\u05DC\u05E4\u05D5\u05DF: {\u05D8\u05DC\u05E4\u05D5\u05DF}\n\n\u05E0\u05D0 \u05DC\u05D9\u05E6\u05D5\u05E8 \u05D2\u05DD \u05DE\u05E0\u05D5\u05D9 \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05E2\u05D1\u05D5\u05E8 ___ \u05D7\u05D9\u05D1\u05D5\u05E8\u05D9\u05DD \u05D1\u05D4\u05EA\u05D0\u05DD \u05DC\u05DE\u05E1\u05DC\u05D5\u05DC.\n\n[\u05D0\u05D9\u05DF \u05E6\u05D5\u05E8\u05DA \u05DC\u05E9\u05DC\u05D5\u05D7 \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8 - \u05D4\u05DC\u05E7\u05D5\u05D7 \u05D9\u05E9\u05EA\u05DE\u05E9 \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 / \u05E0\u05D0 \u05DC\u05E9\u05D9\u05D9\u05DA \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8 \u05DE\u05E1\u05E4\u05E8 ______].\n\n\u05D1\u05D1\u05E8\u05DB\u05D4 \u05D5\u05EA\u05D5\u05D3\u05D4,\n\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF - \u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA\n\u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8";
        out.add(t);
        t = new Tpl();
        t.name = "\u05E9\u05D9\u05E0\u05D5\u05D9 \u05DE\u05E1\u05DC\u05D5\u05DC \u05D5\u05E9\u05D9\u05D5\u05DA \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8";
        t.subject = "\u05E9\u05D9\u05E0\u05D5\u05D9 \u05DE\u05E1\u05DC\u05D5\u05DC \u05D5\u05E9\u05D9\u05D5\u05DA \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8 - {\u05DC\u05E7\u05D5\u05D7} - \u05D0\u05EA\u05E8 {\u05D0\u05EA\u05E8}";
        t.body = "\u05E9\u05DC\u05D5\u05DD \u05E8\u05D1.\n\n\u05DC\u05E7\u05D5\u05D7:\n\u05E9\u05DD: {\u05DC\u05E7\u05D5\u05D7}\n\u05DE\u05E1\u05F3 \u05D0\u05EA\u05E8: {\u05D0\u05EA\u05E8}\n\u05D8\u05DC\u05E4\u05D5\u05DF: {\u05D8\u05DC\u05E4\u05D5\u05DF}\n\n\u05DE\u05D1\u05E7\u05E9 \u05DC\u05E9\u05E0\u05D5\u05EA \u05D0\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7 \u05DC\u05DE\u05E1\u05DC\u05D5\u05DC ___ (\u05D0\u05F3/\u05D1\u05F3/\u05D2\u05F3/\u05D3\u05F3).\n\n\u05D1\u05E0\u05D5\u05E1\u05E3, \u05DC\u05E4\u05EA\u05D5\u05D7 \u05DE\u05E0\u05D5\u05D9 \u05DC\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05D5\u05DC\u05E9\u05D9\u05D9\u05DA \u05D0\u05EA \u05D4\u05E1\u05D8\u05E8\u05D9\u05DE\u05E8 \u05D4\u05D1\u05D0:\n\u05DE\u05E1\u05E4\u05E8 \u05E1\u05D9\u05D3\u05D5\u05E8\u05D9 \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8: ______\n\n\u05E1\u05D9\u05D3\u05D5\u05E8\u05D9 \u05DE\u05DE\u05D9\u05E8 \u05E7\u05D9\u05D9\u05DD:\n0002-\n\n\u05D1\u05DE\u05D9\u05D3\u05EA \u05D4\u05E6\u05D5\u05E8\u05DA \u05DC\u05EA\u05D0\u05DD \u05D0\u05D9\u05E1\u05D5\u05E3 \u05E9\u05DC \u05D4\u05DE\u05DE\u05D9\u05E8 \u05D4\u05E7\u05D9\u05D9\u05DD \u05DE\u05D5\u05DC \u05D4\u05DC\u05E7\u05D5\u05D7.\n\n\u05EA\u05D5\u05D3\u05D4.\n\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF - \u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA\n\u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8";
        out.add(t);
        t = new Tpl();
        t.name = "\u05D1\u05D9\u05D8\u05D5\u05DC \u05DE\u05E0\u05D5\u05D9";
        t.subject = "\u05D1\u05E7\u05E9\u05D4 \u05DC\u05D1\u05D9\u05D8\u05D5\u05DC \u05DE\u05E0\u05D5\u05D9 - {\u05DC\u05E7\u05D5\u05D7} - \u05D0\u05EA\u05E8 {\u05D0\u05EA\u05E8}";
        t.body = "\u05E9\u05DC\u05D5\u05DD \u05E8\u05D1.\n\n\u05D1\u05D1\u05E7\u05E9\u05D4 \u05DC\u05D1\u05D8\u05DC \u05D0\u05EA \u05D4\u05DE\u05E0\u05D5\u05D9 \u05D4\u05D1\u05D0:\n\n\u05E9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7: {\u05DC\u05E7\u05D5\u05D7}\n\u05DE\u05E1\u05E4\u05E8 \u05D0\u05EA\u05E8 / \u05DE\u05E0\u05D5\u05D9: {\u05D0\u05EA\u05E8}\n\u05DE\u05E1\u05E4\u05E8 \u05DB\u05E8\u05D8\u05D9\u05E1 \u05DE\u05DE\u05D9\u05E8: 0002-\n\n\u05E1\u05D9\u05D1\u05EA \u05D4\u05D1\u05D9\u05D8\u05D5\u05DC: ______ (\u05E2\u05D6\u05D1 \u05D0\u05EA \u05D4\u05E7\u05D9\u05D1\u05D5\u05E5 / \u05D1\u05E7\u05E9\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7 / \u05E1\u05D9\u05D1\u05D4 \u05D0\u05D7\u05E8\u05EA).\n\n\u05D1\u05D1\u05E7\u05E9\u05D4 \u05DC\u05E2\u05D3\u05DB\u05DF \u05DC\u05D0\u05D7\u05E8 \u05D1\u05D9\u05E6\u05D5\u05E2 \u05D4\u05D1\u05D9\u05D8\u05D5\u05DC \u05D5\u05DC\u05EA\u05D0\u05DD \u05D0\u05D9\u05E1\u05D5\u05E3 \u05E6\u05D9\u05D5\u05D3 \u05D1\u05DE\u05D9\u05D3\u05EA \u05D4\u05E6\u05D5\u05E8\u05DA.\n\n\u05EA\u05D5\u05D3\u05D4,\n\u05E8\u05D5\u05DF \u05E1\u05D5\u05DC\u05D5\u05DE\u05D5\u05DF\n\u05DE\u05E0\u05D4\u05DC \u05EA\u05E9\u05EA\u05D9\u05D5\u05EA \u05DE\u05D7\u05E9\u05D5\u05D1 \u05D5\u05EA\u05E7\u05E9\u05D5\u05E8\u05EA\n\u05E7\u05D9\u05D1\u05D5\u05E5 \u05DE\u05E6\u05E8";
        out.add(t);
        return out;
    }

    static List<Tpl> list(Context c) {
        List<Tpl> seeds = builtIn();
        List<Tpl> all = null;
        for (int i = 0; i < seeds.size(); i++) {
            String key = i == 0 ? SEEDED : "templates_seed_" + (i + 1);
            if (CallLogger.prefs(c).getBoolean(key, false)) continue;
            if (all == null) all = read(c);
            all.add(Math.min(i, all.size()), seeds.get(i));
            CallLogger.prefs(c).edit().putBoolean(key, true).apply();
        }
        if (all != null) save(c, all);
        // One-time fix: use the real Excel column names for site number and ID.
        if (!CallLogger.prefs(c).getBoolean("templates_fix_1", false)) {
            List<Tpl> fix = read(c);
            for (Tpl t : fix) {
                t.subject = t.subject.replace("{\u05DE\u05E1\u05E4\u05E8 \u05D0\u05EA\u05E8}", "{\u05D0\u05EA\u05E8}").replace("{\u05EA\u05E2\u05D5\u05D3\u05EA \u05D6\u05D4\u05D5\u05EA}", "{\u05D6\u05D4\u05D5\u05EA}");
                t.body = t.body.replace("{\u05DE\u05E1\u05E4\u05E8 \u05D0\u05EA\u05E8}", "{\u05D0\u05EA\u05E8}").replace("{\u05EA\u05E2\u05D5\u05D3\u05EA \u05D6\u05D4\u05D5\u05EA}", "{\u05D6\u05D4\u05D5\u05EA}");
            }
            save(c, fix);
            CallLogger.prefs(c).edit().putBoolean("templates_fix_1", true).apply();
        }
        return read(c);
    }

    private static List<Tpl> read(Context c) {
        List<Tpl> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(CallLogger.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Tpl t = new Tpl();
                t.name = o.optString("name");
                t.subject = o.optString("subject");
                t.body = o.optString("body");
                out.add(t);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static void save(Context c, List<Tpl> items) {
        JSONArray a = new JSONArray();
        try {
            for (Tpl t : items) {
                JSONObject o = new JSONObject();
                o.put("name", t.name);
                o.put("subject", t.subject);
                o.put("body", t.body);
                a.put(o);
            }
        } catch (Exception ignored) { }
        CallLogger.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    /** All fields that can be used: ticket fields, then the Excel column titles. */
    static List<String> allFields(Context c) {
        List<String> out = new ArrayList<>();
        for (String f : TICKET_FIELDS) out.add(f);
        for (String h : Data.get(c).headers) {
            String f = "{" + h + "}";
            if (!out.contains(f)) out.add(f);
        }
        return out;
    }

    /** Replaces the {fields} in text with this ticket's data and the client's Excel row. */
    static String fill(Context c, TicketDb.Ticket t, String text) {
        TicketDb db = TicketDb.get(c);
        StringBuilder notes = new StringBuilder();
        for (TicketDb.Note n : db.notes(t.id)) {
            notes.append(TicketDb.fmt(n.time)).append(" - ").append(n.text).append('\n');
        }
        String[] values = {
                String.valueOf(t.id), t.clientName, t.clientPhone, t.title, t.description, t.equipment, t.mac,
                He.category(t.category), He.priority(t.priority), He.status(t.status), t.provider, t.outsource,
                t.followupAt > 0 ? TicketDb.fmt(t.followupAt) : "",
                t.created > 0 ? TicketDb.fmt(t.created) : "",
                TicketDb.fmt(System.currentTimeMillis()).substring(0, 10),
                notes.toString().trim()};
        String out = text;
        for (int i = 0; i < TICKET_FIELDS.length; i++) out = out.replace(TICKET_FIELDS[i], values[i]);

        // Excel columns of this client (found by phone, or by name).
        ClientStore s = Data.get(c);
        int row = s.find(t.clientPhone);
        if (row < 0) {
            for (int r = 0; r < s.rows.size(); r++) {
                if (!t.clientName.isEmpty() && s.name(r).equalsIgnoreCase(t.clientName)) { row = r; break; }
            }
        }
        for (int col = 0; col < s.headers.size(); col++) {
            String v = row >= 0 ? s.rows.get(row).get(col) : "";
            out = out.replace("{" + s.headers.get(col) + "}", v);
        }
        return out;
    }
}
