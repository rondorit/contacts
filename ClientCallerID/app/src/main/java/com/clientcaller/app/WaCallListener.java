package com.clientcaller.app;

import android.app.Notification;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads WhatsApp's call notification (WhatsApp calls don't go through the phone system)
 * and shows the client card for the caller. Keeps a short log for troubleshooting.
 */
public class WaCallListener extends NotificationListenerService {
    static final String KEY_LOG = "wa_log";
    static final String KEY_CONNECTED = "wa_connected";

    @Override
    public void onListenerConnected() {
        CallLogger.prefs(this).edit().putLong(KEY_CONNECTED, System.currentTimeMillis()).apply();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            if ("com.google.android.gm".equals(pkg) || "com.samsung.android.email.provider".equals(pkg)
                    || "com.microsoft.office.outlook".equals(pkg)) {
                Notification gn = sbn.getNotification();
                if ((gn.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
                String r = QuoteApprovals.fromGmail(this, gn);
                if (r != null) log((pkg.endsWith(".gm") ? "Gmail" : "\u05DE\u05D9\u05D9\u05DC") + " \u2192 " + r);
                return;
            }
            if (!"com.whatsapp".equals(pkg) && !"com.whatsapp.w4b".equals(pkg)) return;
            Notification n = sbn.getNotification();
            Bundle ex = n.extras;
            String title = str(ex.getCharSequence(Notification.EXTRA_TITLE));
            String bigTitle = str(ex.getCharSequence(Notification.EXTRA_TITLE_BIG));
            String conv = str(ex.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE));
            String text = str(ex.getCharSequence(Notification.EXTRA_TEXT));
            String person = "";
            int callType = 0;
            if (Build.VERSION.SDK_INT >= 31) {
                android.app.Person p = ex.getParcelable(Notification.EXTRA_CALL_PERSON);
                if (p != null && p.getName() != null) person = p.getName().toString();
                callType = ex.getInt(Notification.EXTRA_CALL_TYPE, 0);
            }
            String low = (text + " " + title).toLowerCase(Locale.ROOT);
            boolean isMessage = Notification.CATEGORY_MESSAGE.equals(n.category)
                    || ex.getParcelableArray(Notification.EXTRA_MESSAGES) != null;
            boolean isCall = Notification.CATEGORY_CALL.equals(n.category) || callType > 0 || !person.isEmpty()
                    || (!isMessage && (low.contains("call") || low.contains("\u05E9\u05D9\u05D7\u05D4") || low.contains("\u05E9\u05D9\u05D7\u05EA")));
            String result;
            if (!isCall) {
                String q = Team.fromWhatsApp(this, n, title);
                if (q == null) q = Outsource.fromWhatsApp(this, n, title);
                if (q == null) q = QuoteApprovals.fromWhatsApp(this, n, title);
                if (q == null) q = Survey.fromWhatsApp(this, n, title);
                result = q != null ? q : WaMessages.onNotification(this, n, title);
            } else {
                List<String> names = new ArrayList<>();
                for (String c : new String[]{person, title, bigTitle, conv}) {
                    if (!c.isEmpty() && !names.contains(c)) names.add(c);
                }
                result = names.isEmpty() ? "\u05D0\u05D9\u05DF \u05E9\u05DD \u05D1\u05D4\u05EA\u05E8\u05D0\u05D4" : CallPopup.onWhatsApp(this, names);
            }
            log("\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4=" + n.category + " | \u05E1\u05D5\u05D2=" + callType + " | \u05D0\u05D3\u05DD=" + person + " | \u05DB\u05D5\u05EA\u05E8\u05EA=" + title
                    + " | \u05D8\u05E7\u05E1\u05D8=" + (text.length() > 40 ? text.substring(0, 40) : text) + "\n\u2192 " + result);
        } catch (Exception e) {
            log("\u05E9\u05D2\u05D9\u05D0\u05D4: " + e);
        }
    }

    private static String str(CharSequence c) {
        return c == null ? "" : c.toString().trim();
    }

    private void log(String line) {
        SharedPreferences p = CallLogger.prefs(this);
        String old = p.getString(KEY_LOG, "");
        String entry = TicketDb.fmt(System.currentTimeMillis()) + "  " + line;
        String all = entry + (old.isEmpty() ? "" : "\n\n" + old);
        String[] parts = all.split("\n\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(parts.length, 8); i++) {
            if (i > 0) sb.append("\n\n");
            sb.append(parts[i]);
        }
        p.edit().putString(KEY_LOG, sb.toString()).apply();
    }
}
