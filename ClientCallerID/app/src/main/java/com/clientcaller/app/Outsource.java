package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Outsourcing providers: ticket details by WhatsApp, and their status replies. */
final class Outsource {
    static final String ACTION_STATUS = "com.clientcaller.app.OUT_STATUS";
    static final String[] KEYS = {"", "Sent", "In progress", "Done", "Pickup", "Approval"};
    static final String[] LABELS = {"\u2014", "\u05E0\u05E9\u05DC\u05D7", "\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "\u05D8\u05D5\u05E4\u05DC", "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8"};
    private static final Pattern TICKET_NO = Pattern.compile("(?:#|\u05E7\u05E8\u05D9\u05D0\u05D4\\s*(?:\u05DE\u05E1['\u05F3]?\\s*)?#?)\\s*(\\d{1,6})");
    private static final String[] PICKUP = {"\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DE\u05DE\u05EA\u05D9\u05E0\u05D4 \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DE\u05D5\u05DB\u05DF \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DE\u05D5\u05DB\u05E0\u05D4 \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DE\u05D5\u05DB\u05E0\u05D9\u05DD \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05D0\u05E4\u05E9\u05E8 \u05DC\u05D0\u05E1\u05D5\u05E3",
            "\u05E0\u05D9\u05EA\u05DF \u05DC\u05D0\u05E1\u05D5\u05E3", "\u05DC\u05D0\u05D9\u05E1\u05D5\u05E3", "\u05DC\u05D1\u05D5\u05D0 \u05DC\u05D0\u05E1\u05D5\u05E3", "\u05D0\u05E4\u05E9\u05E8 \u05DC\u05D1\u05D5\u05D0 \u05DC\u05E7\u05D7\u05EA"};
    private static final String[] APPROVAL = {"\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DE\u05DE\u05EA\u05D9\u05E0\u05D4 \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DE\u05D7\u05DB\u05D4 \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DE\u05D7\u05DB\u05D9\u05DD \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05E6\u05E8\u05D9\u05DA \u05D0\u05D9\u05E9\u05D5\u05E8", "\u05E0\u05D3\u05E8\u05E9 \u05D0\u05D9\u05E9\u05D5\u05E8",
            "\u05D3\u05E8\u05D5\u05E9 \u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DC\u05D0\u05D9\u05E9\u05D5\u05E8\u05DB\u05DD", "\u05DC\u05D0\u05D9\u05E9\u05D5\u05E8\u05DA", "\u05DE\u05D0\u05E9\u05E8\u05D9\u05DD?", "\u05DE\u05D0\u05E9\u05E8?", "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8"};
    private static final String[] NOT_DONE = {"\u05DC\u05D0 \u05D8\u05D5\u05E4\u05DC", "\u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2", "\u05DC\u05D0 \u05EA\u05D5\u05E7\u05DF", "\u05E2\u05D5\u05D3 \u05DC\u05D0", "\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0", "\u05DC\u05D0 \u05D4\u05E6\u05DC\u05D7\u05E0\u05D5", "\u05DC\u05D0 \u05D4\u05E6\u05DC\u05D7\u05EA\u05D9", "\u05DC\u05D0 \u05E0\u05E4\u05EA\u05E8"};
    private static final String[] DONE = {"\u05D8\u05D5\u05E4\u05DC", "\u05D8\u05D5\u05E4\u05DC\u05D4", "\u05D1\u05D5\u05E6\u05E2", "\u05D1\u05D5\u05E6\u05E2\u05D4", "\u05EA\u05D5\u05E7\u05DF", "\u05EA\u05D5\u05E7\u05E0\u05D4", "\u05EA\u05D9\u05E7\u05E0\u05D5", "\u05EA\u05D9\u05E7\u05E0\u05EA\u05D9", "\u05D4\u05E1\u05EA\u05D9\u05D9\u05DD", "\u05D4\u05E1\u05EA\u05D9\u05D9\u05DE\u05D4", "\u05E1\u05D9\u05D9\u05DE\u05E0\u05D5",
            "\u05E1\u05D9\u05D9\u05DE\u05EA\u05D9", "\u05E0\u05E4\u05EA\u05E8", "\u05E0\u05E4\u05EA\u05E8\u05D4", "\u05D4\u05D5\u05E9\u05DC\u05DD", "\u05E2\u05D5\u05D1\u05D3 \u05E2\u05DB\u05E9\u05D9\u05D5", "\u05D7\u05D6\u05E8 \u05DC\u05E2\u05D1\u05D5\u05D3", "done", "fixed", "\u2705", "\u2714"};
    private static final String[] WORKING = {"\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "\u05DE\u05D8\u05E4\u05DC\u05D9\u05DD", "\u05DE\u05D8\u05E4\u05DC", "\u05DE\u05D8\u05E4\u05DC\u05EA", "\u05E0\u05D8\u05E4\u05DC", "\u05E7\u05D9\u05D1\u05DC\u05E0\u05D5", "\u05E7\u05D9\u05D1\u05DC\u05EA\u05D9", "\u05D4\u05EA\u05E7\u05D1\u05DC", "\u05D4\u05EA\u05E7\u05D1\u05DC\u05D4", "\u05E4\u05EA\u05D7\u05E0\u05D5 \u05E7\u05E8\u05D9\u05D0\u05D4",
            "\u05E0\u05E4\u05EA\u05D7\u05D4 \u05E7\u05E8\u05D9\u05D0\u05D4", "\u05D8\u05DB\u05E0\u05D0\u05D9", "\u05D1\u05D3\u05E8\u05DA", "\u05E0\u05D1\u05D3\u05D5\u05E7", "\u05D1\u05D1\u05D3\u05D9\u05E7\u05D4", "\uD83D\uDC4D", "\uD83D\uDC4C"};

    private Outsource() { }

    static String label(String key) {
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return LABELS[i];
        return key == null ? "" : key;
    }

    /** The relevant ticket details for the provider, without internal notes. */
    static String details(Context c, TicketDb.Ticket t) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA #").append(t.id).append('\n');
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
        if (!t.description.isEmpty()) sb.append("\u05EA\u05D9\u05D0\u05D5\u05E8: ").append(t.description).append('\n');
        if (!t.equipment.isEmpty()) sb.append("\u05E6\u05D9\u05D5\u05D3: ").append(t.equipment).append('\n');
        if (!t.mac.isEmpty()) sb.append("MAC: ").append(t.mac).append('\n');
        if (!t.outsource.isEmpty()) sb.append("\u05E4\u05E8\u05D8\u05D9\u05DD: ").append(t.outsource).append('\n');
        if ("Urgent".equals(t.priority)) sb.append("\uD83D\uDD34 \u05D3\u05D7\u05D5\u05E3\n");
        return sb.toString().trim();
    }

    /** Footer telling the provider how to update the status by replying. */
    static String footer(TicketDb.Ticket t) {
        return "\n\n\u2014 \u05DC\u05E2\u05D3\u05DB\u05D5\u05DF \u05E1\u05D8\u05D8\u05D5\u05E1 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " \u05D4\u05E9\u05D1: \"\u05D1\u05D8\u05D9\u05E4\u05D5\u05DC\" / \"\u05D8\u05D5\u05E4\u05DC\" / \"\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E1\u05D5\u05E3\" / \"\u05DE\u05DE\u05EA\u05D9\u05DF \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8\" (\u05D0\u05E4\u05E9\u05E8 \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05D4\u05E2\u05E8\u05D4).";
    }

    // ------------------------------------------------------------------ replies

    private static String clean(String s) {
        return s == null ? "" : s.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "")
                .replaceAll("\\s*\\(\\d+[^)]*\\)\\s*$", "").trim();
    }

    /** The provider who sent this message (by name or phone), or null. */
    static TicketDb.Provider sender(Context c, String sender) {
        String name = clean(sender);
        if (name.isEmpty()) return null;
        List<TicketDb.Provider> all = TicketDb.get(c).providers();
        for (TicketDb.Provider p : all) if (p.name.equalsIgnoreCase(name)) return p;
        List<String> keys = new ArrayList<>();
        if (name.replaceAll("[^0-9]", "").length() >= 8) keys.add(ClientStore.key(name));
        try {
            for (String n : PhoneContacts.numbersForName(c, name)) keys.add(ClientStore.key(n));
        } catch (Exception ignored) { }
        if (keys.isEmpty()) return null;
        for (TicketDb.Provider p : all) {
            String k = ClientStore.key(p.phone);
            if (k.length() >= 7 && keys.contains(k)) return p;
        }
        return null;
    }

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
        TicketDb.Provider p = sender(c, sender);
        if (p == null) return null;
        text = text.trim();
        if (text.isEmpty()) return null;

        TicketDb db = TicketDb.get(c);
        TicketDb.Ticket t = null;
        Matcher mt = TICKET_NO.matcher(text);
        if (mt.find()) {
            TicketDb.Ticket x = db.load(Long.parseLong(mt.group(1)));
            if (x != null && p.name.equals(x.provider)) t = x;
        }
        if (t == null) {
            for (TicketDb.Ticket x : db.all()) {
                if (!x.isOpen() || !p.name.equals(x.provider)) continue;
                if (t == null || x.updated > t.updated) t = x;
            }
        }
        if (t == null) return "\u05D4\u05D5\u05D3\u05E2\u05D4 \u05DE" + p.name + " \u2013 \u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D4 \u05E4\u05EA\u05D5\u05D7\u05D4 \u05D0\u05E6\u05DC\u05D5";

        String low = QuoteApprovals.norm(text).replaceAll("[\\uFE0F]", "");
        String to = null;
        for (String w : PICKUP) if (low.contains(w)) { to = "Pickup"; break; }
        if (to == null) for (String w : APPROVAL) if (low.contains(w)) { to = "Approval"; break; }
        if (to == null) {
            boolean notDone = false;
            for (String w : NOT_DONE) if (low.contains(w)) { notDone = true; break; }
            if (!notDone) for (String w : DONE) if (low.contains(w)) { to = "Done"; break; }
            if (to == null) for (String w : WORKING) if (low.contains(w)) { to = "In progress"; break; }
        }
        String prev = t.outStatus == null ? "" : t.outStatus;
        db.addNote(t.id, "\uD83C\uDFE2 " + p.name + ": " + text);
        if (to != null && !to.equals(prev)) {
            t.outStatus = to;
            db.save(t);
            db.addNote(t.id, "\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5: " + label(prev) + " \u2190 " + label(to));
        } else {
            to = null;
        }
        notify(c, t, p.name, text, prev, to);
        return p.name + " \u2192 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + (to == null ? ": \u05E0\u05D5\u05E1\u05E4\u05D4 \u05D4\u05E2\u05E8\u05D4" : ": " + label(to));
    }

    private static void notify(Context c, TicketDb.Ticket t, String who, String text, String prev, String to) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("out_replies", "\u05E2\u05D3\u05DB\u05D5\u05E0\u05D9\u05DD \u05DE\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5", NotificationManager.IMPORTANCE_HIGH));
        int nid = 600000 + (int) t.id;
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent open = new Intent(c, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pOpen = PendingIntent.getActivity(c, nid, open, flags);
        String icon = "Done".equals(to) ? "\u2705 " : "Pickup".equals(to) ? "\uD83D\uDCE6 " : "Approval".equals(to) ? "\u2753 " : "\uD83C\uDFE2 ";
        Notification.Builder b = new Notification.Builder(c, "out_replies")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle(icon + who + ": \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + (to == null ? "" : " \u2013 " + label(to)))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(t.clientName + " \u2013 " + t.title + "\n\u201E" + text + "\u201C"))
                .setContentIntent(pOpen)
                .setAutoCancel(true)
                .addAction(new Notification.Action.Builder(null, "\u05E4\u05EA\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D4", pOpen).build());
        if (to != null) {
            Intent undo = new Intent(c, FollowupReceiver.class).setAction(ACTION_STATUS)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id).putExtra("status", prev);
            b.addAction(new Notification.Action.Builder(null, "\u21A9 \u05D1\u05D8\u05DC", PendingIntent.getBroadcast(c, nid, undo, flags)).build());
        }
        if ("Done".equals(to)) {
            Intent close = new Intent(c, FollowupReceiver.class).setAction(Team.ACTION_STATUS)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id).putExtra("status", "Closed");
            b.addAction(new Notification.Action.Builder(null, "\u2714 \u05E1\u05D2\u05D5\u05E8 \u05E7\u05E8\u05D9\u05D0\u05D4", PendingIntent.getBroadcast(c, nid + 1, close, flags)).build());
        }
        nm.notify(nid, b.build());
    }

    /** Undo from the notification. */
    static void setStatus(Context c, long id, String status) {
        TicketDb db = TicketDb.get(c);
        TicketDb.Ticket t = db.load(id);
        if (t == null) return;
        String prev = t.outStatus;
        t.outStatus = status == null ? "" : status;
        db.save(t);
        db.addNote(id, "\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5: " + label(prev) + " \u2190 " + label(t.outStatus) + " (\u05D1\u05D5\u05D8\u05DC)");
        try { c.getSystemService(NotificationManager.class).cancel(600000 + (int) id); } catch (Exception ignored) { }
        Toast.makeText(c, "\u05E7\u05E8\u05D9\u05D0\u05D4 #" + id + ": " + label(t.outStatus), Toast.LENGTH_SHORT).show();
    }
}
