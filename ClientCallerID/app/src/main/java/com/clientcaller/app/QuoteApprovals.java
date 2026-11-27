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
    /** Words/emoji anywhere in a short reply. */
    private static final String[] YES = {"\u05DE\u05D0\u05E9\u05E8", "\u05DE\u05D0\u05E9\u05E8\u05EA", "\u05DE\u05D0\u05E9\u05E8\u05D9\u05DD", "\u05DE\u05D0\u05D5\u05E9\u05E8", "\u05DE\u05D0\u05D5\u05E9\u05E8\u05EA", "\u05D0\u05D9\u05E9\u05D5\u05E8", "\u05DE\u05E7\u05D5\u05D1\u05DC", "\u05DE\u05E7\u05D5\u05D1\u05DC\u05EA", "\u05E1\u05D2\u05E8\u05E0\u05D5", "\u05E1\u05D2\u05D5\u05E8",
            "\u05DE\u05E1\u05DB\u05D9\u05DD", "\u05DE\u05E1\u05DB\u05D9\u05DE\u05D4", "\u05DE\u05E1\u05DB\u05D9\u05DE\u05D9\u05DD", "\u05E0\u05E9\u05DE\u05E2 \u05D8\u05D5\u05D1", "\u05E0\u05E9\u05DE\u05E2 \u05DE\u05E6\u05D5\u05D9\u05DF", "\u05E0\u05E9\u05DE\u05E2 \u05DE\u05E6\u05D5\u05D9\u05D9\u05DF", "\u05D0\u05E4\u05E9\u05E8 \u05DC\u05D4\u05EA\u05E7\u05D3\u05DD", "\u05D1\u05D5\u05D0 \u05E0\u05EA\u05E7\u05D3\u05DD", "\u05D1\u05D5\u05D0\u05D5 \u05E0\u05EA\u05E7\u05D3\u05DD",
            "\u05EA\u05EA\u05E7\u05D3\u05DD", "\u05EA\u05EA\u05E7\u05D3\u05DE\u05D5", "\u05DE\u05EA\u05E7\u05D3\u05DE\u05D9\u05DD", "\u05D9\u05D0\u05DC\u05DC\u05D4", "\u05D0\u05D9\u05DF \u05D1\u05E2\u05D9\u05D4", "\u05D1\u05E1\u05D3\u05E8 \u05D2\u05DE\u05D5\u05E8", "\u05D4\u05D5\u05DC\u05DA \u05E2\u05DC \u05D6\u05D4", "\u05D4\u05D5\u05DC\u05DB\u05D9\u05DD \u05E2\u05DC \u05D6\u05D4", "approve", "accept", "agreed",
            "\u05D0\u05D5\u05E7\u05D9\u05D9", "\u05D0\u05D5\u05E7\u05D9", "\u05E1\u05D1\u05D1\u05D4", "\uD83D\uDC4D", "\u2705", "\uD83D\uDC4C", "\uD83D\uDE4F", "\uD83D\uDCAF", "\u2714", "\u2764", "\u2665", "\uD83D\uDE0D", "\uD83E\uDD70", "\uD83D\uDC4F", "\uD83E\uDD1D", "\uD83D\uDCAA", "\uD83D\uDE4C", "\uD83D\uDE0A", "\uD83D\uDE00", "\uD83D\uDE03", "\uD83C\uDD97"};
    private static final String[] NO = {"\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8", "\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8\u05EA", "\u05DC\u05D0 \u05DE\u05D0\u05E9\u05E8\u05D9\u05DD", "\u05DC\u05D0 \u05DE\u05E7\u05D5\u05D1\u05DC", "\u05DC\u05D0 \u05DE\u05E2\u05D5\u05E0\u05D9\u05D9\u05DF", "\u05DC\u05D0 \u05DE\u05E2\u05D5\u05E0\u05D9\u05D9\u05E0\u05EA", "\u05DC\u05D0 \u05DE\u05E2\u05D5\u05E0\u05D9\u05D9\u05E0\u05D9\u05DD",
            "\u05DC\u05D0 \u05DE\u05EA\u05D0\u05D9\u05DD", "\u05DC\u05D0 \u05DE\u05EA\u05D0\u05D9\u05DE\u05D4", "\u05DC\u05D0 \u05E8\u05DC\u05D5\u05D5\u05E0\u05D8\u05D9", "\u05DC\u05D0 \u05EA\u05D5\u05D3\u05D4", "\u05DC\u05D0, \u05EA\u05D5\u05D3\u05D4", "\u05DC\u05D0 \u05E6\u05E8\u05D9\u05DA", "\u05DC\u05D0 \u05E0\u05D7\u05D5\u05E5", "\u05D9\u05E7\u05E8 \u05DE\u05D3\u05D9", "\u05D9\u05E7\u05E8 \u05DC\u05D9", "\u05D9\u05E7\u05E8 \u05DE\u05D9\u05D3\u05D9",
            "\u05E0\u05D5\u05D5\u05EA\u05E8", "\u05DE\u05D5\u05D5\u05EA\u05E8", "\u05DE\u05D5\u05D5\u05EA\u05E8\u05EA", "\u05DE\u05D5\u05D5\u05EA\u05E8\u05D9\u05DD", "\u05DE\u05D1\u05D8\u05DC", "\u05DE\u05D1\u05D8\u05DC\u05EA", "\u05DE\u05D1\u05D8\u05DC\u05D9\u05DD", "\u05D1\u05D9\u05D8\u05D5\u05DC", "\u05D3\u05D5\u05D7\u05D4", "decline", "reject",
            "\uD83D\uDC4E", "\u274C", "\u2716", "\uD83D\uDEAB", "\u26D4", "\uD83D\uDE21", "\uD83D\uDE20", "\uD83D\uDE45"};
    /** Whole-message short answers (after removing punctuation). */
    private static final String[] YES_WORDS = {"\u05DB\u05DF", "\u05DB\u05DF \u05EA\u05D5\u05D3\u05D4", "\u05DB\u05DF \u05D1\u05D1\u05E7\u05E9\u05D4", "\u05D1\u05E1\u05D3\u05E8", "\u05D8\u05D5\u05D1", "\u05D9\u05D5\u05E4\u05D9", "\u05DE\u05E2\u05D5\u05DC\u05D4", "\u05DE\u05E6\u05D5\u05D9\u05DF", "\u05DE\u05E6\u05D5\u05D9\u05D9\u05DF", "ok", "okay", "yes", "\u05E7\u05D3\u05D9\u05DE\u05D4", "\u05E1\u05D2\u05D5\u05E8", "\u05D1\u05D8\u05D7", "\u05D1\u05E8\u05D5\u05E8", "\u05D1\u05D4\u05D7\u05DC\u05D8", "\u05EA\u05D5\u05D3\u05D4 \u05DB\u05DF", "\u05DE\u05E2\u05D5\u05DC\u05D4 \u05EA\u05D5\u05D3\u05D4", "\u05D9\u05D5\u05E4\u05D9 \u05EA\u05D5\u05D3\u05D4", "\u05D1\u05E1\u05D3\u05E8 \u05EA\u05D5\u05D3\u05D4", "\u05D8\u05D5\u05D1 \u05EA\u05D5\u05D3\u05D4", "\u05DE\u05E6\u05D5\u05D9\u05DF \u05EA\u05D5\u05D3\u05D4", "\u05DE\u05E6\u05D5\u05D9\u05D9\u05DF \u05EA\u05D5\u05D3\u05D4", "\u05E1\u05D1\u05D1\u05D4 \u05EA\u05D5\u05D3\u05D4"};
    private static final String[] NO_WORDS = {"\u05DC\u05D0", "no", "\u05DC\u05D0 \u05DB\u05E8\u05D2\u05E2", "\u05DC\u05D0 \u05E2\u05DB\u05E9\u05D9\u05D5", "\u05E4\u05D7\u05D5\u05EA", "\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0"};
    private static final Pattern QUOTE_NO = Pattern.compile("\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8\\s*(?:\u05DE\u05E1['\u05F3]?\\s*)?(\\d+)");

    private QuoteApprovals() { }

    /** 1 = approve, -1 = decline, 0 = neither. Only short replies count, so ordinary messages aren't mistaken. */
    static int intent(String text) {
        String t = norm(text).replaceAll("[\uFE0F\u200D]", "").replaceAll("[\\x{1F3FB}-\\x{1F3FF}]", "");
        if (t.isEmpty()) return 0;
        // WhatsApp reaction ("\u05D4\u05D2\u05D9\u05D1/\u05D4 \uD83D\uDC4D \u05DC..."): judge only the emoji, not the quoted message.
        if (t.contains("\u05D4\u05D2\u05D9\u05D1") || t.contains("reacted")) {
            int q = indexOfAny(t, "\"", "\u201C", "\u201E", ":", "\u00AB");
            t = q > 0 ? t.substring(0, q) : t;
            t = t.replaceAll("[\\p{L}\\p{N}\\s/]", "");
            if (t.isEmpty()) return 0;
        }
        if (t.length() > 160) return 0;
        String plain = t.replaceAll("[.,!?;:\\-\"'\u05F4\u05F3()\u2026]+", " ").replaceAll("\\s+", " ").trim();
        for (String w : NO_WORDS) if (plain.equals(w)) return -1;
        for (String w : YES_WORDS) if (plain.equals(w)) return 1;
        if (t.contains("\u05D0\u05D9\u05DF \u05D1\u05E2\u05D9\u05D4") || t.contains("\u05DC\u05D0 \u05D1\u05E2\u05D9\u05D4")) return 1;
        for (String n : NO) if (t.contains(n)) return -1;
        for (String y : YES) if (t.contains(y)) return 1;
        // Short reply opening with a clear yes/no ("\u05DB\u05DF, \u05EA\u05EA\u05D7\u05D9\u05DC", "\u05DC\u05D0, \u05EA\u05D5\u05D3\u05D4 \u05E8\u05D1\u05D4").
        if (plain.length() <= 40) {
            if (plain.startsWith("\u05DB\u05DF ") || plain.startsWith("yes ")) return 1;
            if (plain.startsWith("\u05DC\u05D0 ") || plain.startsWith("no ")) return -1;
        }
        return 0;
    }

    private static int indexOfAny(String s, String... keys) {
        int best = -1;
        for (String k : keys) {
            int i = s.indexOf(k);
            if (i >= 0 && (best < 0 || i < best)) best = i;
        }
        return best;
    }


    /** Lower-case, drop invisible direction marks and collapse all kinds of spaces. */
    static String norm(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF]", "")
                .replaceAll("[\\s\\u00A0]+", " ").toLowerCase(Locale.ROOT).trim();
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
        if (yes == 0) {
            // Fall back to the plain notification text / big text.
            CharSequence big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
            for (CharSequence alt : new CharSequence[]{tx, big}) {
                if (alt != null && intent(alt.toString()) != 0) { text = alt.toString(); yes = intent(text); break; }
            }
        }
        if (yes == 0 || sender == null) return null;
        String phone = null;
        // WhatsApp may add a counter to the title, e.g. "Name (2 messages)".
        String name = sender.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069]", "")
                .replaceAll("\\s*\\(\\d+[^)]*\\)\\s*$", "").trim();
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
        apply(c, q, yes, "\u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4: " + text.trim());
        return "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + " \u05E2\u05D5\u05D3\u05DB\u05E0\u05D4: " + (yes > 0 ? "\u05D0\u05D5\u05E9\u05E8\u05D4" : "\u05E0\u05D3\u05D7\u05EA\u05D4");
    }

    /** Email notification (Gmail / Samsung Email / Outlook) replying to a quote. Returns a log line, or null if unrelated. */
    static String fromGmail(Context c, Notification n) {
        Bundle ex = n.extras;
        String sender = str(ex.getCharSequence(Notification.EXTRA_TITLE));
        String text = str(ex.getCharSequence(Notification.EXTRA_TEXT));
        String big = str(ex.getCharSequence(Notification.EXTRA_BIG_TEXT));
        StringBuilder lines = new StringBuilder();
        CharSequence[] tl = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (tl != null) for (CharSequence l : tl) if (l != null) lines.append(l).append('\n');
        String all = text + "\n" + big + "\n" + lines;
        Matcher m = QUOTE_NO.matcher(all);
        long id = m.find() ? Long.parseLong(m.group(1)) : 0;
        if (id == 0 && !all.contains("\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8")) return null; // not about a quote
        // The reply itself: big text (or text) without the subject line, cut before quoted history / signature.
        String body = big.isEmpty() ? text : big;
        int yes = 0;
        String said = "";
        for (String raw : body.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (QUOTE_NO.matcher(line).find() || line.toLowerCase(Locale.ROOT).startsWith("re:")
                    || line.toLowerCase(Locale.ROOT).startsWith("fwd:") || (!big.isEmpty() && line.equals(text.trim()) && QUOTE_NO.matcher(text).find())) continue;
            String head = line.split("(?i)(\u05D1\u05EA\u05D0\u05E8\u05D9\u05DA|\u05D1-?\u05D9\u05D5\u05DD [\u05D0-\u05E9]|\\bon .{0,80}wrote|wrote:|\u05DB\u05EA\u05D1:|\u05DB\u05EA\u05D1/\u05D4|\u05DB\u05EA\u05D1\u05D4:|-----|\\s>|^>|from:|\u05DE\u05D0\u05EA:|\u05E0\u05E9\u05DC\u05D7 \u05DE|sent from|\\s--\\s)")[0].trim();
            if (head.isEmpty()) break;
            yes = intent(head);
            said = head;
            break; // only the first line of the reply counts
        }
        if (yes == 0) return "\u05DE\u05D9\u05D9\u05DC \u05E2\u05DC \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8" + (id > 0 ? " " + id : "") + " \u05DE-" + sender + ": \u05DC\u05D0 \u05D6\u05D5\u05D4\u05D4 \u05D0\u05D9\u05E9\u05D5\u05E8/\u05D3\u05D7\u05D9\u05D9\u05D4 (" + cut(said) + ")";
        QuoteDb.Quote q = findQuote(c, sender, null, sender, id);
        if (q == null) return "\u05D6\u05D5\u05D4\u05D4 " + (yes > 0 ? "\u05D0\u05D9\u05E9\u05D5\u05E8" : "\u05D3\u05D7\u05D9\u05D9\u05D4") + " \u05D1\u05DE\u05D9\u05D9\u05DC \u05DE-" + sender
                + (id > 0 ? " \u05DC\u05D4\u05E6\u05E2\u05D4 " + id + ", \u05D0\u05D1\u05DC \u05D4\u05D9\u05D0 \u05DC\u05D0 \u05D1\u05DE\u05E6\u05D1 \u05E0\u05E9\u05DC\u05D7\u05D4/\u05D8\u05D9\u05D5\u05D8\u05D4" : ", \u05D0\u05D1\u05DC \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D4\u05E6\u05E2\u05D4 \u05E4\u05EA\u05D5\u05D7\u05D4 (\u05D0\u05D9\u05DF \u05DE\u05E1\u05E4\u05E8 \u05D4\u05E6\u05E2\u05D4 \u05D1\u05E0\u05D5\u05E9\u05D0)");
        apply(c, q, yes, "\u05D1\u05DE\u05D9\u05D9\u05DC: " + said);
        return "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + " \u05E2\u05D5\u05D3\u05DB\u05E0\u05D4 \u05DE\u05D4\u05DE\u05D9\u05D9\u05DC: " + (yes > 0 ? "\u05D0\u05D5\u05E9\u05E8\u05D4" : "\u05E0\u05D3\u05D7\u05EA\u05D4");
    }

    private static String str(CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }

    private static String cut(String s) {
        return s.length() > 40 ? s.substring(0, 40) + "\u2026" : s;
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

    /** Updates the quote status right away and posts a notice with an undo button. */
    private static void apply(Context c, QuoteDb.Quote q, int yes, String said) {
        String prev = q.status;
        String status = yes > 0 ? "Approved" : "Rejected";
        setStatus(c, q.id, status, false);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("quote_replies", "\u05EA\u05E9\u05D5\u05D1\u05D5\u05EA \u05DC\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8",
                NotificationManager.IMPORTANCE_HIGH));
        int nid = 400000 + (int) q.id;
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent undo = new Intent(c, FollowupReceiver.class).setAction(ACTION_SET)
                .putExtra(QuoteActivity.EXTRA_QUOTE, q.id).putExtra("status", prev);
        PendingIntent pUndo = PendingIntent.getBroadcast(c, nid, undo, flags);
        Intent open = new Intent(c, QuoteActivity.class).putExtra(QuoteActivity.EXTRA_QUOTE, q.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pOpen = PendingIntent.getActivity(c, nid, open, flags);
        Notification n = new Notification.Builder(c, "quote_replies")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle((yes > 0 ? "\u2705 " : "\u274C ") + "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + " \u05E1\u05D5\u05DE\u05E0\u05D4 \u05DB"
                        + QuoteDb.statusHe(status) + " - " + q.clientName)
                .setContentText(said)
                .setStyle(new Notification.BigTextStyle().bigText(said + "\n\u05E1\u05D4\"\u05DB: " + QuoteDb.money(q.total())))
                .setContentIntent(pOpen)
                .addAction(new Notification.Action.Builder(null, "\u21A9 \u05D1\u05D8\u05DC (\u05D4\u05D7\u05D6\u05E8 \u05DC" + QuoteDb.statusHe(prev) + ")", pUndo).build())
                .addAction(new Notification.Action.Builder(null, "\u05E4\u05EA\u05D7 \u05D4\u05E6\u05E2\u05D4", pOpen).build())
                .setAutoCancel(true)
                .build();
        nm.notify(nid, n);
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
        setStatus(c, id, status, true);
    }

    static void setStatus(Context c, long id, String status, boolean manual) {
        QuoteDb db = QuoteDb.get(c);
        QuoteDb.Quote q = db.load(id);
        if (q == null) return;
        q.status = status;
        db.save(q);
        QuoteActivity.cancelReminder(c, id);
        if (q.ticketId > 0) TicketDb.get(c).addNote(q.ticketId, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + id + ": " + QuoteDb.statusHe(status)
                + (manual ? "" : " (\u05DC\u05E4\u05D9 \u05EA\u05E9\u05D5\u05D1\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7)"));
        if (!manual) return;
        try { c.getSystemService(NotificationManager.class).cancel(400000 + (int) id); } catch (Exception ignored) { }
        android.widget.Toast.makeText(c, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + id + ": " + QuoteDb.statusHe(status), android.widget.Toast.LENGTH_SHORT).show();
    }
}
