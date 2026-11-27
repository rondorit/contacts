package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Team members who get service tickets by WhatsApp, and their replies ("\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC" / "\u05D1\u05D5\u05E6\u05E2"). */
final class Team {
    static final String KEY = "team";
    static final String ACTION_STATUS = "com.clientcaller.app.TEAM_TICKET_STATUS";
    private static final Pattern TICKET_NO = Pattern.compile("(?:#|\u05E7\u05E8\u05D9\u05D0\u05D4\\s*(?:\u05DE\u05E1['\u05F3]?\\s*)?#?)\\s*(\\d{1,6})");
    private static final String[] DONE = {"\u05D1\u05D5\u05E6\u05E2", "\u05D1\u05D5\u05E6\u05E2\u05D4", "\u05D8\u05D5\u05E4\u05DC", "\u05D8\u05D5\u05E4\u05DC\u05D4", "\u05E1\u05D9\u05D9\u05DE\u05EA\u05D9", "\u05E1\u05D9\u05D9\u05DE\u05E0\u05D5", "\u05D4\u05E1\u05EA\u05D9\u05D9\u05DD", "\u05D4\u05E1\u05EA\u05D9\u05D9\u05DE\u05D4", "\u05EA\u05D5\u05E7\u05DF", "\u05EA\u05D5\u05E7\u05E0\u05D4",
            "\u05EA\u05D9\u05E7\u05E0\u05EA\u05D9", "\u05EA\u05D9\u05E7\u05E0\u05D5", "\u05D2\u05DE\u05E8\u05EA\u05D9", "\u05D2\u05DE\u05E8\u05E0\u05D5", "\u05D4\u05D5\u05E9\u05DC\u05DD", "\u05D4\u05D5\u05E9\u05DC\u05DE\u05D4", "\u05E0\u05E4\u05EA\u05E8", "\u05E0\u05E4\u05EA\u05E8\u05D4", "\u05E4\u05EA\u05E8\u05EA\u05D9", "\u05E2\u05D5\u05D1\u05D3 \u05E2\u05DB\u05E9\u05D9\u05D5", "done", "fixed", "\u2705", "\u2714"};
    private static final String[] NOT_DONE = {"\u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2", "\u05DC\u05D0 \u05D8\u05D5\u05E4\u05DC", "\u05DC\u05D0 \u05E1\u05D9\u05D9\u05DE\u05EA\u05D9", "\u05E2\u05D5\u05D3 \u05DC\u05D0", "\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0", "\u05DC\u05D0 \u05EA\u05D5\u05E7\u05DF", "\u05DC\u05D0 \u05D4\u05E6\u05DC\u05D7\u05EA\u05D9", "\u05DC\u05D0 \u05E2\u05D5\u05D1\u05D3", "\u05DC\u05D0 \u05E0\u05E4\u05EA\u05E8"};
    private static final String[] WORKING = {"\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "\u05DE\u05D8\u05E4\u05DC", "\u05DE\u05D8\u05E4\u05DC\u05EA", "\u05DE\u05D8\u05E4\u05DC\u05D9\u05DD", "\u05D0\u05D8\u05E4\u05DC", "\u05E0\u05D8\u05E4\u05DC", "\u05E7\u05D9\u05D1\u05DC\u05EA\u05D9", "\u05E7\u05D9\u05D1\u05DC\u05E0\u05D5", "\u05D1\u05D3\u05E8\u05DA", "\u05DE\u05D2\u05D9\u05E2", "\u05DE\u05D2\u05D9\u05E2\u05D4",
            "\u05DE\u05D2\u05D9\u05E2\u05D9\u05DD", "\u05E2\u05DC \u05D6\u05D4", "\u05D0\u05E0\u05D9 \u05E2\u05DC", "\u05E1\u05D1\u05D1\u05D4", "\u05D0\u05D5\u05E7\u05D9\u05D9", "\u05D0\u05D5\u05E7\u05D9", "ok", "\uD83D\uDC4D", "\uD83D\uDC4C", "\uD83D\uDE4F"};

    static final class Member {
        String name = "", phone = "";
    }

    private Team() { }

    static List<Member> list(Context c) {
        List<Member> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(CallLogger.prefs(c).getString(KEY, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Member m = new Member();
                m.name = o.optString("name");
                m.phone = o.optString("phone");
                out.add(m);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static void save(Context c, List<Member> list) {
        JSONArray a = new JSONArray();
        try {
            for (Member m : list) a.put(new JSONObject().put("name", m.name).put("phone", m.phone));
        } catch (Exception ignored) { }
        CallLogger.prefs(c).edit().putString(KEY, a.toString()).apply();
    }

    static Member byName(Context c, String name) {
        for (Member m : list(c)) if (m.name.equalsIgnoreCase(name.trim())) return m;
        return null;
    }

    // ------------------------------------------------------------------ management UI

    static void manage(final Activity a) {
        final List<Member> all = list(a);
        String[] items = new String[all.size() + 1];
        for (int i = 0; i < all.size(); i++) items[i] = "\uD83D\uDC77 " + all.get(i).name + (all.get(i).phone.isEmpty() ? "" : "  \u2022  " + all.get(i).phone);
        items[all.size()] = "\u2795 \u05D4\u05D5\u05E1\u05E3 \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA";
        new AlertDialog.Builder(a)
                .setTitle("\u05D0\u05E0\u05E9\u05D9 \u05E6\u05D5\u05D5\u05EA")
                .setItems(items, (d, w) -> edit(a, w < all.size() ? w : -1, () -> manage(a)))
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                .show();
    }

    private static void edit(final Activity a, final int index, final Runnable after) {
        final List<Member> all = list(a);
        final Member m = index >= 0 ? all.get(index) : new Member();
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * a.getResources().getDisplayMetrics().density);
        box.setPadding(p, p / 2, p, 0);
        final EditText name = new EditText(a);
        name.setHint("\u05E9\u05DD");
        name.setText(m.name);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(name);
        final EditText phone = new EditText(a);
        phone.setHint("\u05D8\u05DC\u05E4\u05D5\u05DF (\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4)");
        phone.setText(m.phone);
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        box.addView(phone);
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(index >= 0 ? "\u05E2\u05E8\u05D9\u05DB\u05EA \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA" : "\u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA \u05D7\u05D3\u05E9")
                .setView(box)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    m.name = name.getText().toString().trim();
                    m.phone = phone.getText().toString().trim();
                    if (m.name.isEmpty()) {
                        Toast.makeText(a, "\u05D7\u05E1\u05E8 \u05E9\u05DD", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (index < 0) all.add(m);
                    save(a, all);
                    if (after != null) after.run();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (index >= 0) b.setNeutralButton("\u05DE\u05D7\u05E7", (d, w) -> Auth.confirm(a, "\u05DE\u05D7\u05D9\u05E7\u05EA " + m.name, () -> {
            all.remove(index);
            save(a, all);
            if (after != null) after.run();
        }));
        b.show();
    }

    /** Lets the user pick a team member (or none). Returns "" for none. */
    interface Picked { void on(String name); }

    static void choose(final Activity a, String current, final Picked cb) {
        final List<Member> all = list(a);
        if (all.isEmpty()) {
            new AlertDialog.Builder(a)
                    .setMessage("\u05E2\u05D5\u05D3 \u05D0\u05D9\u05DF \u05D0\u05E0\u05E9\u05D9 \u05E6\u05D5\u05D5\u05EA. \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05E2\u05DB\u05E9\u05D9\u05D5?")
                    .setPositiveButton("\u05D4\u05D5\u05E1\u05E3", (d, w) -> edit(a, -1, () -> choose(a, current, cb)))
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show();
            return;
        }
        String[] items = new String[all.size() + 2];
        for (int i = 0; i < all.size(); i++) items[i] = (all.get(i).name.equals(current) ? "\u2713 " : "\uD83D\uDC77 ") + all.get(i).name;
        items[all.size()] = "\u2795 \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA \u05D7\u05D3\u05E9";
        items[all.size() + 1] = "\u2716 \u05DC\u05DC\u05D0 \u05E9\u05D9\u05D5\u05DA";
        new AlertDialog.Builder(a)
                .setTitle("\u05E9\u05D9\u05D5\u05DA \u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05DC\u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA")
                .setItems(items, (d, w) -> {
                    if (w < all.size()) cb.on(all.get(w).name);
                    else if (w == all.size()) edit(a, -1, () -> choose(a, current, cb));
                    else cb.on("");
                })
                .show();
    }

    // ------------------------------------------------------------------ sending

    static String message(Context c, TicketDb.Ticket t) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDEE0\uFE0F \u05DE\u05E9\u05D9\u05DE\u05D4 \u2013 \u05E7\u05E8\u05D9\u05D0\u05D4 #").append(t.id).append('\n');
        if (!t.clientName.isEmpty()) sb.append("\u05DC\u05E7\u05D5\u05D7: ").append(t.clientName).append('\n');
        if (!t.clientPhone.isEmpty()) sb.append("\u05D8\u05DC\u05E4\u05D5\u05DF: ").append(t.clientPhone).append('\n');
        String addr = "";
        try {
            ClientStore s = Data.get(c);
            int row = t.clientPhone.isEmpty() ? -1 : s.find(t.clientPhone);
            if (row < 0 && !t.clientName.isEmpty())
                for (int r = 0; r < s.rows.size(); r++) if (s.name(r).equalsIgnoreCase(t.clientName)) { row = r; break; }
            if (row >= 0) addr = Nav.address(c, s, row);
        } catch (Exception ignored) { }
        if (!addr.isEmpty()) sb.append("\u05DB\u05EA\u05D5\u05D1\u05EA: ").append(addr).append("\n\u05E0\u05D9\u05D5\u05D5\u05D8: https://waze.com/ul?q=").append(Uri.encode(addr)).append("&navigate=yes\n");
        sb.append("\u05E0\u05D5\u05E9\u05D0: ").append(t.title).append('\n');
        if (!t.description.isEmpty()) sb.append("\u05E4\u05D9\u05E8\u05D5\u05D8: ").append(t.description).append('\n');
        if (!t.equipment.isEmpty()) sb.append("\u05E6\u05D9\u05D5\u05D3: ").append(t.equipment).append('\n');
        if (!t.mac.isEmpty()) sb.append("MAC: ").append(t.mac).append('\n');
        if ("Urgent".equals(t.priority)) sb.append("\uD83D\uDD34 \u05D3\u05D7\u05D5\u05E3\n");
        sb.append("\n\u05DC\u05E2\u05D3\u05DB\u05D5\u05DF \u05D4\u05E9\u05D1 \u05DC\u05D4\u05D5\u05D3\u05E2\u05D4: \"\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC\" \u05D0\u05D5 \"\u05D1\u05D5\u05E6\u05E2\" (\u05D0\u05E4\u05E9\u05E8 \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05D4\u05E2\u05E8\u05D4).");
        return sb.toString();
    }

    static void send(Activity a, TicketDb.Ticket t) {
        Member m = byName(a, t.assignee);
        if (m == null || m.phone.trim().isEmpty()) {
            Toast.makeText(a, "\u05D0\u05D9\u05DF \u05DE\u05E1\u05E4\u05E8 \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05D0\u05D9\u05E9 \u05D4\u05E6\u05D5\u05D5\u05EA", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + DetailActivity.whatsAppNumber(m.phone)
                    + "?text=" + Uri.encode(message(a, t)))));
            TicketDb.get(a).addNote(t.id, "\u05E0\u05E9\u05DC\u05D7 \u05DC\u05D0\u05D9\u05E9 \u05D4\u05E6\u05D5\u05D5\u05EA " + m.name + " \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4");
        } catch (Exception e) {
            Toast.makeText(a, "\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05D0 \u05E0\u05E4\u05EA\u05D7: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ replies

    /** The team member who sent this WhatsApp notification, or null. */
    static Member sender(Context c, String sender) {
        if (sender == null) return null;
        String name = sender.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "")
                .replaceAll("\\s*\\(\\d+[^)]*\\)\\s*$", "").trim();
        List<Member> all = list(c);
        if (all.isEmpty()) return null;
        for (Member m : all) if (m.name.equalsIgnoreCase(name)) return m;
        List<String> keys = new ArrayList<>();
        if (name.replaceAll("[^0-9]", "").length() >= 8) keys.add(ClientStore.key(name));
        try {
            for (String n : PhoneContacts.numbersForName(c, name)) keys.add(ClientStore.key(n));
        } catch (Exception ignored) { }
        for (Member m : all) {
            String k = ClientStore.key(m.phone);
            if (k.length() >= 7 && keys.contains(k)) return m;
        }
        return null;
    }

    /** Handles a WhatsApp message from a team member. Returns a log line, or null if the sender isn't on the team. */
    static String fromWhatsApp(Context c, Notification n, String title) {
        Bundle ex = n.extras;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return null;
        if (ex.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) return null;
        String sender = title;
        CharSequence tx = ex.getCharSequence(Notification.EXTRA_TEXT);
        String text = tx == null ? "" : tx.toString();
        Parcelable[] msgs = ex.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (msgs != null && msgs.length > 0 && msgs[msgs.length - 1] instanceof Bundle) {
            Bundle last = (Bundle) msgs[msgs.length - 1];
            CharSequence t = last.getCharSequence("text");
            if (t != null) text = t.toString();
            CharSequence s = last.getCharSequence("sender");
            if (s != null && s.length() > 0) sender = s.toString();
        }
        Member m = sender(c, sender);
        if (m == null) return null;
        text = text.trim();
        if (text.isEmpty()) return "\u05D4\u05D5\u05D3\u05E2\u05D4 \u05E8\u05D9\u05E7\u05D4 \u05DE\u05D0\u05D9\u05E9 \u05D4\u05E6\u05D5\u05D5\u05EA " + m.name;

        TicketDb db = TicketDb.get(c);
        TicketDb.Ticket t = null;
        Matcher mt = TICKET_NO.matcher(text);
        if (mt.find()) t = db.load(Long.parseLong(mt.group(1)));
        if (t == null) {
            for (TicketDb.Ticket x : db.all()) {
                if (!x.isOpen() || !m.name.equalsIgnoreCase(x.assignee)) continue;
                if (t == null || x.updated > t.updated) t = x;
            }
        }
        if (t == null) return "\u05D4\u05D5\u05D3\u05E2\u05D4 \u05DE\u05D0\u05D9\u05E9 \u05D4\u05E6\u05D5\u05D5\u05EA " + m.name + " \u2013 \u05D0\u05D9\u05DF \u05DC\u05D5 \u05E7\u05E8\u05D9\u05D0\u05D4 \u05E4\u05EA\u05D5\u05D7\u05D4 \u05DE\u05E9\u05D5\u05D9\u05DB\u05EA";

        String low = QuoteApprovals.norm(text).replaceAll("[\\uFE0F]", "");
        String to = null;
        boolean notDone = false;
        for (String w : NOT_DONE) if (low.contains(w)) { notDone = true; break; }
        if (!notDone) {
            for (String w : DONE) if (low.contains(w)) { to = "Closed"; break; }
            if (to == null && low.length() <= 80) for (String w : WORKING) if (low.contains(w)) { to = "In progress"; break; }
        }
        String prev = t.status;
        db.addNote(t.id, "\uD83D\uDC77 " + m.name + ": " + text);
        if (to != null && !to.equals(prev)) {
            t.status = to;
            db.save(t);
            db.addNote(t.id, "\u05E1\u05D8\u05D8\u05D5\u05E1: " + He.status(prev) + " \u2190 " + He.status(to) + " (\u05DC\u05E4\u05D9 " + m.name + ")");
            try { Followups.schedule(c, t); } catch (Exception ignored) { }
            try { Followups.syncCalendar(c, t); } catch (Exception ignored) { }
        } else {
            to = null;
        }
        notify(c, t, m, text, prev, to);
        return "\u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA " + m.name + " \u2192 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + (to == null ? ": \u05E0\u05D5\u05E1\u05E4\u05D4 \u05D4\u05E2\u05E8\u05D4" : ": " + He.status(to));
    }

    private static void notify(Context c, TicketDb.Ticket t, Member m, String text, String prev, String to) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("team_replies", "\u05E2\u05D3\u05DB\u05D5\u05E0\u05D9\u05DD \u05DE\u05D0\u05E0\u05E9\u05D9 \u05D4\u05E6\u05D5\u05D5\u05EA", NotificationManager.IMPORTANCE_HIGH));
        int nid = 500000 + (int) t.id;
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent open = new Intent(c, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pOpen = PendingIntent.getActivity(c, nid, open, flags);
        String head = to == null ? "\uD83D\uDC77 " + m.name + " \u2013 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id
                : ("Closed".equals(to) ? "\u2705 " : "\uD83D\uDD27 ") + m.name + ": \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " " + He.status(to);
        Notification.Builder b = new Notification.Builder(c, "team_replies")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(head)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(t.clientName + " \u2013 " + t.title + "\n\u201E" + text + "\u201C"))
                .setContentIntent(pOpen)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(null, "\u05E4\u05EA\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D4", pOpen).build());
        if (to != null) {
            Intent undo = new Intent(c, FollowupReceiver.class).setAction(ACTION_STATUS)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id).putExtra("status", prev);
            b.addAction(new Notification.Action.Builder(null, "\u21A9 \u05D1\u05D8\u05DC (" + He.status(prev) + ")",
                    PendingIntent.getBroadcast(c, nid, undo, flags)).build());
        }
        if ("Closed".equals(to) && !t.clientPhone.isEmpty()) {
            Intent sum = new Intent(c, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id)
                    .putExtra(TicketActivity.EXTRA_SUMMARY, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            b.addAction(new Notification.Action.Builder(null, "\uD83D\uDCAC \u05E1\u05D9\u05DB\u05D5\u05DD \u05DC\u05DC\u05E7\u05D5\u05D7", PendingIntent.getActivity(c, nid + 1, sum, flags)).build());
        }
        nm.notify(nid, b.build());
    }

    /** Undo from the notification. */
    static void setStatus(Context c, long id, String status) {
        TicketDb db = TicketDb.get(c);
        TicketDb.Ticket t = db.load(id);
        if (t == null || status == null) return;
        String prev = t.status;
        t.status = status;
        db.save(t);
        db.addNote(id, "\u05E1\u05D8\u05D8\u05D5\u05E1: " + He.status(prev) + " \u2190 " + He.status(status) + " (\u05D1\u05D5\u05D8\u05DC)");
        try { Followups.schedule(c, t); } catch (Exception ignored) { }
        try { c.getSystemService(NotificationManager.class).cancel(500000 + (int) id); } catch (Exception ignored) { }
        Toast.makeText(c, "\u05E7\u05E8\u05D9\u05D0\u05D4 #" + id + ": " + He.status(status), Toast.LENGTH_SHORT).show();
    }
}
