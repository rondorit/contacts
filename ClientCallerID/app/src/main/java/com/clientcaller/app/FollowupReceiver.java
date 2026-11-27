package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Shows the follow-up reminder; also restores alarms after the phone restarts. */
public class FollowupReceiver extends BroadcastReceiver {
    static final String ACTION = "com.clientcaller.app.FOLLOWUP";
    static final String ACTION_QUOTE = "com.clientcaller.app.QUOTE_REMINDER";
    private static final String CHANNEL = "followups";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            Followups.rescheduleAll(context);
            Backup.scheduleReminder(context);
            return;
        }
        if (CallLogger.ACTION_SAVE.equals(action) || CallLogger.ACTION_SKIP.equals(action)) {
            CallLogger.onAction(context, intent);
            return;
        }
        if (Backup.ACTION_REMIND.equals(action)) {
            Backup.remindIfNeeded(context);
            return;
        }
        if (QuoteApprovals.ACTION_SET.equals(action)) {
            QuoteApprovals.setStatus(context, intent.getLongExtra(QuoteActivity.EXTRA_QUOTE, 0), intent.getStringExtra("status"));
            return;
        }
        if (ACTION_QUOTE.equals(action)) {
            quoteReminder(context, intent.getLongExtra(QuoteActivity.EXTRA_QUOTE, 0));
            return;
        }
        if (!ACTION.equals(action)) return;
        long id = intent.getLongExtra(TicketActivity.EXTRA_TICKET, 0);
        TicketDb.Ticket t = TicketDb.get(context).load(id);
        if (t == null || !t.isOpen()) return;
        try {
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "\u05DE\u05E2\u05E7\u05D1 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA", NotificationManager.IMPORTANCE_HIGH));
            Intent open = new Intent(context, TicketActivity.class)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(context, (int) t.id, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String who = t.clientName.isEmpty() ? "" : t.clientName + ": ";
            Notification n = new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("\u05DE\u05E2\u05E7\u05D1 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " - " + who + t.title)
                    .setContentText(He.status(t.status) + " | " + He.priority(t.priority))
                    .setStyle(new Notification.BigTextStyle().bigText(
                            He.status(t.status) + " | " + He.priority(t.priority) + (t.description.isEmpty() ? "" : "\n" + t.description)))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(50000 + (int) t.id, n);
        } catch (Exception ignored) { }
    }

    private static void quoteReminder(Context c, long id) {
        QuoteDb.Quote q = QuoteDb.get(c).load(id);
        if (q == null || !q.isOpen()) return;
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("quotes", "\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8", NotificationManager.IMPORTANCE_HIGH));
            Intent open = new Intent(c, QuoteActivity.class).putExtra(QuoteActivity.EXTRA_QUOTE, id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(c, 300000 + (int) id, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(c, "quotes")
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("\u05DC\u05D1\u05D3\u05D5\u05E7 \u05D0\u05D9\u05E9\u05D5\u05E8 \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + id)
                    .setContentText(q.clientName + " \u2022 " + QuoteDb.money(q.total()))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(300000 + (int) id, n);
        } catch (Exception ignored) { }
    }
}
