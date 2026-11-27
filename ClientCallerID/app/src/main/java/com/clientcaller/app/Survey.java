package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "Rate us 1-5" after a ticket is closed; the client's WhatsApp reply is recorded. */
final class Survey {
    static final String LINE = "\n\n\u05E0\u05E9\u05DE\u05D7 \u05DC\u05D3\u05E2\u05EA \u05D0\u05D9\u05DA \u05D4\u05D9\u05D4 \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA: \u05D4\u05E9\u05D1 \u05D1\u05DE\u05E1\u05E4\u05E8 1 \u05E2\u05D3 5 \u2B50 (5 = \u05DE\u05E6\u05D5\u05D9\u05DF).";
    private static final String PENDING = "survey_";
    private static final String RATING = "rating_t";
    private static final long WINDOW = 7L * 24 * 60 * 60 * 1000;
    private static final Pattern DIGIT = Pattern.compile("^\\D{0,6}([1-5])(\\s*/\\s*5)?\\D{0,12}$");

    private Survey() { }

    /** Remembers that this client was asked to rate the ticket. */
    static void asked(Context c, TicketDb.Ticket t) {
        String k = ClientStore.key(t.clientPhone);
        if (k.length() < 7) return;
        CallLogger.prefs(c).edit().putString(PENDING + k, t.id + "|" + System.currentTimeMillis()).apply();
    }

    static List<Integer> ratings(Context c) {
        List<Integer> out = new ArrayList<>();
        for (Map.Entry<String, ?> e : CallLogger.prefs(c).getAll().entrySet()) {
            if (e.getKey().startsWith(RATING) && e.getValue() instanceof Integer) {
                int r = (Integer) e.getValue();
                if (r >= 1 && r <= 5) out.add(r);
            }
        }
        return out;
    }

    static int rating(Context c, long ticketId) {
        return CallLogger.prefs(c).getInt(RATING + ticketId, 0);
    }

    /** Parses "4", "4/5", "\u2B50\u2B50\u2B50\u2B50" \u2026 Returns 0 if it isn't a rating. */
    static int parse(String text) {
        String t = QuoteApprovals.norm(text).replaceAll("[\\uFE0F]", "");
        if (t.isEmpty() || t.length() > 24) return 0;
        Matcher m = DIGIT.matcher(t);
        if (m.find()) return Integer.parseInt(m.group(1));
        int stars = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == '\u2B50' || t.charAt(i) == '\u2605') stars++;
        String rest = t.replace("\u2B50", "").replace("\u2605", "").trim();
        return stars >= 1 && stars <= 5 && rest.isEmpty() ? stars : 0;
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
        int r = parse(text);
        if (r == 0 || sender == null) return null;
        String name = sender.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "")
                .replaceAll("\\s*\\(\\d+[^)]*\\)\\s*$", "").trim();
        List<String> keys = new ArrayList<>();
        if (name.replaceAll("[^0-9]", "").length() >= 8) keys.add(ClientStore.key(name));
        try {
            for (String num : PhoneContacts.numbersForName(c, name)) keys.add(ClientStore.key(num));
        } catch (Exception ignored) { }
        try {
            int row = CallPopup.findClientRow(c, name);
            if (row >= 0) keys.addAll(Data.get(c).keysForRow(row));
        } catch (Exception ignored) { }
        SharedPreferences p = CallLogger.prefs(c);
        for (String k : keys) {
            String v = p.getString(PENDING + k, null);
            if (v == null) continue;
            long id = Long.parseLong(v.substring(0, v.indexOf('|')));
            long when = Long.parseLong(v.substring(v.indexOf('|') + 1));
            if (System.currentTimeMillis() - when > WINDOW) {
                p.edit().remove(PENDING + k).apply();
                continue;
            }
            p.edit().remove(PENDING + k).putInt(RATING + id, r).apply();
            TicketDb.get(c).addNote(id, "\u2B50 \u05D3\u05D9\u05E8\u05D5\u05D2 \u05D4\u05DC\u05E7\u05D5\u05D7: " + r + "/5");
            notify(c, id, name, r);
            return "\u05D3\u05D9\u05E8\u05D5\u05D2 " + r + "/5 \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4 #" + id + " \u05DE-" + name;
        }
        return null;
    }

    private static void notify(Context c, long id, String who, int r) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("survey", "\u05D3\u05D9\u05E8\u05D5\u05D2\u05D9 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", NotificationManager.IMPORTANCE_DEFAULT));
            Intent open = new Intent(c, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(c, 700000 + (int) id, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            StringBuilder stars = new StringBuilder();
            for (int k = 0; k < r; k++) stars.append("\u2B50");
            nm.notify(700000 + (int) id, new Notification.Builder(c, "survey")
                    .setSmallIcon(android.R.drawable.btn_star_big_on)
                    .setContentTitle(stars + " " + who + " \u05D3\u05D9\u05E8\u05D2/\u05D4 \u05D0\u05EA \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA " + r + "/5")
                    .setContentText("\u05E7\u05E8\u05D9\u05D0\u05D4 #" + id)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build());
        } catch (Exception ignored) { }
    }
}
