package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes a client's reply approving (or declining) a sent price quote, in WhatsApp or Gmail,
 * and offers one tap to mark the quote.
 */
final class QuoteApprovals {
    static final String ACTION_SET = "com.clientcaller.app.QUOTE_SET_STATUS";
    private static final String[] YES = {"\u05DE\u05D0\u05E9\u05E8", "\u05DE\u05D0\u05E9\u05E8\u05EA", "\u05DE\u05D0\u05E9\u05E8\u05D9\u05DD", "\u05DE\u05D0\u05D5\u05E9\u05E8", "\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DE\u05E7\u05D5\u05D1\u05DC", "\u05DE\u05E7\u05D5\u05D1\u05DC\u05EA", "\u05E1\u05D2\u05E8\u05E0\u05D5",
            "approve", "approved", "accept", "ok", "\u05D0\u05D5\u05E7\u05D9\u05D9", "\u05D0\u05D5\u05E7\u05D9", "\uD83D\uDC4D", "\u2705"};
    private static final String[] NO = {"\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8", "\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8\u05EA", "\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8\u05D9\u05DD", "\u05DC\u05D0 \u05DE\u05E7\u05D5\u05D1\u05DC", "\u05DC\u05D0 \u05DE\u05E2\u05D5\u05E0\u05D9\u05D9\u05DF", "\u05DC\u05D0 \u05DE\u05E2\u05D5\u05E0\u05D9\u05D9\u05E0\u05EA",
            "\u05D3\u05D5\u05D7\u05D4", "\u05DE\u05D5\u05D5\u05EA\u05E8", "\u05DE\u05D5\u05D5\u05EA\u05E8\u05EA", "decline", "reject", "\uD83D\uDC4E"};
    private static final Pattern QUOTE_NO = Pattern.compile("\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8\\s*(?:\u05DE\u05E1['\u05F3]?\\s*)?(\\d+)");

    private QuoteApprovals() { }

    /** 1 = approve, -1 = decline, 0 = neither. Only short replies count, so ordinary messages aren't mistaken. */
    static int intent(String text) {
        String t = text.toLowerCase(Locale.ROOT).trim();
        if (t.isEmpty() || t.length() > 160) return 0;
        for (String n : NO) if (t.contains(n)) return -1;
        for (String y : YES) if (t.contains(y)) return 1;
        return 0;
    }

    /** WhatsApp message notification. Returns a log line if it was handled as a quote reply, else null. */
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
        int yes = intent(text);
        if (yes == 0 || sender == null) return null;
        String phone = null;
        String name = sender.trim();
        int row = CallPopup.findClientRow(c, name);
        ClientStore s = Data.get(c);
        if (row >= 0) {
            phone = s.firstPhone(row);
            name = s.name(row);
        } else if (name.replaceAll("[^0-9]", "").length() >= 8) {
            phone = name;
        } else if (!PhoneContacts.numbersForName(c, name).isEmpty()) {
            phone = PhoneContacts.numbersForName(c, name).get(0);
        }
        QuoteDb.Quote q = findQuote(c, name, phone, null, 0);
        if (q == null) {
            // Diagnostic for the log: an approval/decline word arrived but no open quote matched this sender.
            return "\u05D6\u05D5\u05D4\u05EA\u05D4 " + (yes > 0 ? "\u05D0\u05D9\u05E9\u05D5\u05E8" : "\u05D3\u05D7\u05D9\u05D9\u05D4")
                    + " \u05DE-" + name + (phone != null ? " (" + phone + ")" : "")
                    + ", \u05D0\u05D1\u05DC \u05D0\u05D9\u05DF \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 \u05E4\u05EA\u05D5\u05D7\u05D4 \u05E9\u05DE\u05EA\u05D0\u05D9\u05DE\u05D4 \u05DC\u05E9\u05D5\u05DC\u05D7";
        }
        offer(c, q, yes, "\u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4: " + text.trim());
        return "\u05EA\u05E9\u05D5\u05D1\u05D4 \u05DC\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + ": " + (yes > 0 ? "\u05D0\u05D9\u05E9\u05D5\u05E8" : "\u05D3\u05D7\u05D9\u05D9\u05D4");
    }

    /** Gmail notification (reply to a quote email). Returns a log line if handled, else null. */
    static String fromGmail(Context c, Notification n) {
        Bundle ex = n.extras;
        CharSequence ti = ex.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence tx = ex.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String sender = ti == null ? "" : ti.toString().trim();
        String all = (tx == null ? "" : tx.toString()) + "\n" + (big == null ? "" : big.toString());
        Matcher m = QUOTE_NO.matcher(all);
        long id = m.find() ? Long.parseLong(m.group(1)) : 0;
        // The reply text is what comes after the subject line.
        String body = big == null ? (tx == null ? "" : tx.toString()) : big.toString();
        String[] lines = body.split("\n");
        String reply = lines.length > 1 ? body.substring(body.indexOf('\n') + 1) : body;
        reply = reply.split("(?i)(\u05D1-.{0,40}\u05DB\u05EA\u05D1|wrote:|-----)")[0];
        int yes = intent(reply);
        if (yes == 0) return null;
        QuoteDb.Quote q = findQuote(c, sender, null, sender, id);
        if (q == null) return null;
        offer(c, q, yes, "\u05D1\u05DE\u05D9\u05D9\u05DC: " + reply.trim());
        return "\u05EA\u05E9\u05D5\u05D1\u05D4 \u05D1\u05DE\u05D9\u05D9\u05DC \u05DC\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id;
    }

    /** An open quote of this client (by quote number, phone, name or email); null if none or not certain. */
    static QuoteDb.Quote findQuote(Context c, String name, String phone, String email, long id) {
        List<QuoteDb.Quote> open = new ArrayList<>();
        for (QuoteDb.Quote q : QuoteDb.get(c).all()) if ("Sent".equals(q.status) || "Draft".equals(q.status)) open.add(q);
        if (id > 0) for (QuoteDb.Quote q : open) if (q.id == id) return q;
        String k = phone == null ? "" : ClientStore.key(phone);
        QuoteDb.Quote best = null;
        for (QuoteDb.Quote q : open) {
            boolean match = (k.length() >= 7 && k.equals(ClientStore.key(q.clientPhone)))
                    || (name != null && !name.isEmpty() && name.equalsIgnoreCase(q.clientName.trim()))
                    || (email != null && !q.clientEmail.isEmpty() && email.toLowerCase(Locale.ROOT).contains(q.clientEmail.toLowerCase(Locale.ROOT)));
            if (!match) continue;
            if (best == null || ("Sent".equals(q.status) && !"Sent".equals(best.status)) || q.updated > best.updated) best = q;
        }
        return best;
    }

    private static void offer(Context c, QuoteDb.Quote q, int yes, String said) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("quote_replies", "\u05EA\u05E9\u05D5\u05D1\u05D5\u05EA \u05DC\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8",
                NotificationManager.IMPORTANCE_HIGH));
        int nid = 400000 + (int) q.id;
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent set = new Intent(c, FollowupReceiver.class).setAction(ACTION_SET)
                .putExtra(QuoteActivity.EXTRA_QUOTE, q.id).putExtra("status", yes > 0 ? "Approved" : "Rejected");
        PendingIntent pSet = PendingIntent.getBroadcast(c, nid, set, flags);
        Intent open = new Intent(c, QuoteActivity.class).putExtra(QuoteActivity.EXTRA_QUOTE, q.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pOpen = PendingIntent.getActivity(c, nid, open, flags);
        Notification n = new Notification.Builder(c, "quote_replies")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle((yes > 0 ? "\u2705 " : "\u274C ") + q.clientName + (yes > 0 ? " \u05D0\u05D9\u05E9\u05E8/\u05D4" : " \u05D3\u05D7\u05D4/\u05EA\u05D4")
                        + " \u05D0\u05EA \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + "?")
                .setContentText(said)
                .setStyle(new Notification.BigTextStyle().bigText(said + "\n\u05E1\u05D4\"\u05DB: " + QuoteDb.money(q.total())))
                .setContentIntent(pOpen)
                .addAction(new Notification.Action.Builder(null, yes > 0 ? "\u2705 \u05E1\u05DE\u05DF \u05DB\u05D0\u05D5\u05E9\u05E8\u05D4" : "\u274C \u05E1\u05DE\u05DF \u05DB\u05E0\u05D3\u05D7\u05EA\u05D4", pSet).build())
                .addAction(new Notification.Action.Builder(null, "\u05E4\u05EA\u05D7 \u05D4\u05E6\u05E2\u05D4", pOpen).build())
                .setAutoCancel(true)
                .build();
        nm.notify(nid, n);
    }

    /** Called from the notification button. */
    static void setStatus(Context c, long id, String status) {
        QuoteDb db = QuoteDb.get(c);
        QuoteDb.Quote q = db.load(id);
        if (q == null) return;
        q.status = status;
        db.save(q);
        QuoteActivity.cancelReminder(c, id);
        if (q.ticketId > 0) TicketDb.get(c).addNote(q.ticketId, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + id + ": " + QuoteDb.statusHe(status)
                + " (\u05DC\u05E4\u05D9 \u05EA\u05E9\u05D5\u05D1\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7)");
        try { c.getSystemService(NotificationManager.class).cancel(400000 + (int) id); } catch (Exception ignored) { }
        android.widget.Toast.makeText(c, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + id + ": " + QuoteDb.statusHe(status), android.widget.Toast.LENGTH_SHORT).show();
    }
}
