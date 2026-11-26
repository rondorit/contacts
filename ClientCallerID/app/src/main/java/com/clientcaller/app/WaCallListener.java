package com.clientcaller.app;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.Locale;

/**
 * Reads WhatsApp's "incoming call" notification (WhatsApp calls don't go through the phone system)
 * and shows the client card for the caller.
 */
public class WaCallListener extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            String pkg = sbn.getPackageName();
            if (!"com.whatsapp".equals(pkg) && !"com.whatsapp.w4b".equals(pkg)) return;
            Notification n = sbn.getNotification();
            Bundle ex = n.extras;
            CharSequence titleCs = ex.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence textCs = ex.getCharSequence(Notification.EXTRA_TEXT);
            String title = titleCs == null ? "" : titleCs.toString().trim();
            String text = textCs == null ? "" : textCs.toString().toLowerCase(Locale.ROOT);
            boolean isCall = Notification.CATEGORY_CALL.equals(n.category);
            boolean incoming = n.fullScreenIntent != null || text.contains("incoming") || text.contains("\u05E0\u05DB\u05E0\u05E1");
            if (!isCall || !incoming || title.isEmpty()) return;
            CallPopup.onWhatsApp(this, title);
        } catch (Exception ignored) { }
    }
}
