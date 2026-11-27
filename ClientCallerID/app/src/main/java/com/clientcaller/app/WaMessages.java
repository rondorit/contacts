package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/** WhatsApp message from a client -> notification with "Open ticket" (client + message filled in). */
final class WaMessages {
    static final String KEY_ENABLED = "wa_msg_ticket";
    private static final String CHANNEL = "wa_messages";
    private static final Map<String, String> recent = new HashMap<>();
    private static final Map<String, Long> recentTime = new HashMap<>();

    private WaMessages() { }

    static final String KEY_MODE = "wa_msg_mode";   // 0 = off, 1 = Excel clients only, 2 = also phone contacts

    static int mode(Context c) {
        android.content.SharedPreferences p = CallLogger.prefs(c);
        if (p.contains(KEY_MODE)) return p.getInt(KEY_MODE, 1);
        return p.getBoolean(KEY_ENABLED, true) ? 1 : 0;
    }

    static boolean enabled(Context c) {
        return mode(c) > 0;
    }

    /** Returns a short description for the diagnostics log. */
    static String onNotification(Context c, Notification n, String title) {
        if (!enabled(c)) return "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05DE\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA \u05DB\u05D1\u05D5\u05D9\u05D4";
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return "\u05E1\u05D9\u05DB\u05D5\u05DD \u05D4\u05D5\u05D3\u05E2\u05D5\u05EA - \u05D3\u05D9\u05DC\u05D5\u05D2";
        Bundle ex = n.extras;
        if (ex.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)) return "\u05E7\u05D1\u05D5\u05E6\u05D4 - \u05D3\u05D9\u05DC\u05D5\u05D2";

        String sender = title;
        String text = ex.getCharSequence(Notification.EXTRA_TEXT) == null ? ""
                : ex.getCharSequence(Notification.EXTRA_TEXT).toString();
        Parcelable[] msgs = ex.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (msgs != null && msgs.length > 0 && msgs[msgs.length - 1] instanceof Bundle) {
            Bundle last = (Bundle) msgs[msgs.length - 1];
            CharSequence t = last.getCharSequence("text");
            if (t != null) text = t.toString();
            CharSequence s = last.getCharSequence("sender");
            if (s != null && s.length() > 0) sender = s.toString();
        }
        text = text.trim();
        if (sender == null || sender.trim().isEmpty() || text.isEmpty()) return "\u05D0\u05D9\u05DF \u05E9\u05D5\u05DC\u05D7 \u05D0\u05D5 \u05D8\u05E7\u05E1\u05D8";

        ClientStore s = Data.get(c);
        int row = CallPopup.findClientRow(c, sender.trim());
        String phone = null;
        String name = sender.trim();
        if (row >= 0) {
            name = s.name(row);
            phone = s.firstPhone(row);
        } else {
            String digits = name.replaceAll("[^0-9]", "");
            if (digits.length() >= 8) phone = name;
            else if (!PhoneContacts.numbersForName(c, name).isEmpty()) phone = PhoneContacts.numbersForName(c, name).get(0);
        }
        if (row < 0 && phone == null) return "\u05D4\u05D5\u05D3\u05E2\u05D4 \u05DE\u05DE\u05D9 \u05E9\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7: " + name;
        if (row < 0 && mode(c) == 1) return "\u05DC\u05D0 \u05D1\u05D0\u05E7\u05E1\u05DC (\u05E8\u05E7 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05DE\u05D4\u05D0\u05E7\u05E1\u05DC): " + name;

        // Collect this sender's messages from the last 15 minutes into one description.
        long now = System.currentTimeMillis();
        String key = name;
        String prev = recent.get(key);
        Long prevT = recentTime.get(key);
        String desc = (prev != null && prevT != null && now - prevT < 15 * 60 * 1000)
                ? (prev.endsWith(text) ? prev : prev + "\n" + text) : text;
        if (desc.length() > 1500) desc = desc.substring(desc.length() - 1500);
        recent.put(key, desc);
        recentTime.put(key, now);

        Intent open = new Intent(c, TicketActivity.class)
                .putExtra(TicketActivity.EXTRA_DESC, "\u05D4\u05D5\u05D3\u05E2\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4:\n" + desc)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (row >= 0) open.putExtra(DetailActivity.EXTRA_ROW, row);
        else open.putExtra(TicketActivity.EXTRA_NAME, name).putExtra(TicketActivity.EXTRA_PHONE, phone);
        int id = 80000 + Math.abs(key.hashCode() % 10000);
        PendingIntent pi = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05DE\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4",
                NotificationManager.IMPORTANCE_DEFAULT));
        Notification note = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setContentTitle("\uD83D\uDCAC " + name + " \u05DB\u05EA\u05D1 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" + (row >= 0 ? "" : " (\u05D0\u05D9\u05E9 \u05E7\u05E9\u05E8)"))
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(desc))
                .setContentIntent(pi)
                .addAction(new Notification.Action.Builder(null, "\uD83D\uDCDD \u05E4\u05EA\u05D7 \u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA", pi).build())
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build();
        nm.notify(id, note);
        return "\u05D4\u05D5\u05E6\u05E2\u05D4 \u05E7\u05E8\u05D9\u05D0\u05D4: " + name;
    }
}
