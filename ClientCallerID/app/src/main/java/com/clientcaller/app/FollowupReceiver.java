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
        if (Backup.ACTION_REMIND.equals(action)) {
            Backup.remindIfNeeded(context);
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
}
