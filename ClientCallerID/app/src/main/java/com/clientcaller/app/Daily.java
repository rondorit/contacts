package com.clientcaller.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import java.io.OutputStream;
import java.util.Calendar;

/** Once-a-day jobs: morning summary, automatic backup, recurring maintenance. */
final class Daily {
    static final String ACTION = "com.clientcaller.app.DAILY";
    static final String KEY_SUMMARY = "daily_summary";      // boolean
    static final String KEY_HOUR = "daily_hour";            // int, default 7
    static final String KEY_AUTO_URI = "auto_backup_uri";
    static final String KEY_AUTO_DAYS = "auto_backup_days"; // 0 off, 1 daily, 7 weekly
    static final String KEY_AUTO_ERR = "auto_backup_err";
    static final int REQ_AUTO_BACKUP = 40;

    private Daily() { }

    static void schedule(Context c) {
        try {
            int hour = CallLogger.prefs(c).getInt(KEY_HOUR, 7);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, 30);
            cal.set(Calendar.SECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_MONTH, 1);
            Intent i = new Intent(c, FollowupReceiver.class).setAction(ACTION);
            PendingIntent pi = PendingIntent.getBroadcast(c, 9990, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            c.getSystemService(AlarmManager.class).setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) { }
    }

    /** Called by the alarm. Heavy work runs off the main thread. */
    static void run(final Context c) {
        final android.content.BroadcastReceiver.PendingResult pr = null;
        new Thread(() -> {
            try { Recurring.runDue(c); } catch (Throwable ignored) { }
            try { if (CallLogger.prefs(c).getBoolean(KEY_SUMMARY, true)) summary(c); } catch (Throwable ignored) { }
            try { autoBackupIfDue(c); } catch (Throwable ignored) { }
            try { TicketsWidget.updateAll(c); } catch (Throwable ignored) { }
        }).start();
    }

    // ------------------------------------------------------------------ morning summary

    static void summary(Context c) {
        TicketDb db = TicketDb.get(c);
        long now = System.currentTimeMillis();
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        int open = 0, overdue = 0, today = 0, stuck = 0;
        StringBuilder lines = new StringBuilder();
        for (TicketDb.Ticket t : db.all()) {
            if (!t.isOpen()) continue;
            open++;
            if (t.isOverdue()) {
                overdue++;
                if (overdue <= 5) lines.append("\uD83D\uDD34 #").append(t.id).append(" ").append(t.clientName).append(" \u2013 ").append(t.title).append('\n');
            } else if (t.followupAt > 0 && t.followupAt <= end.getTimeInMillis()) {
                today++;
                lines.append("\uD83D\uDCC5 #").append(t.id).append(" ").append(t.clientName).append(" \u2013 ").append(t.title).append('\n');
            }
            if ("Waiting for outsourcing".equals(t.status) && now - t.updated > 3L * AlarmManager.INTERVAL_DAY) stuck++;
        }
        int quotes = 0;
        for (QuoteDb.Quote q : QuoteDb.get(c).all()) if ("Sent".equals(q.status)) quotes++;
        if (open == 0 && quotes == 0) return;
        StringBuilder head = new StringBuilder();
        head.append(open).append(" \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA");
        if (overdue > 0) head.append(" \u2022 ").append(overdue).append(" \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8");
        if (today > 0) head.append(" \u2022 ").append(today).append(" \u05DC\u05D4\u05D9\u05D5\u05DD");
        StringBuilder more = new StringBuilder(lines);
        if (stuck > 0) more.append("\uD83C\uDFE2 ").append(stuck).append(" \u05D0\u05E6\u05DC \u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5 \u05DE\u05E2\u05DC 3 \u05D9\u05DE\u05D9\u05DD \u05D1\u05DC\u05D9 \u05E2\u05D3\u05DB\u05D5\u05DF\n");
        if (quotes > 0) more.append("\uD83E\uDDFE ").append(quotes).append(" \u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8 \u05DE\u05DE\u05EA\u05D9\u05E0\u05D5\u05EA \u05DC\u05EA\u05E9\u05D5\u05D1\u05D4\n");
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("daily", "\u05E1\u05D9\u05DB\u05D5\u05DD \u05D1\u05D5\u05E7\u05E8", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open1 = new Intent(c, TicketsActivity.class)
                .putExtra(TicketsActivity.EXTRA_FILTER, overdue > 0 ? TicketsActivity.OVERDUE : TicketsActivity.OPEN)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 9989, open1, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(c, "daily")
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("\u2600\uFE0F \u05D1\u05D5\u05E7\u05E8 \u05D8\u05D5\u05D1 \u2013 " + head)
                .setContentText(more.length() == 0 ? "\u05D0\u05D9\u05DF \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA \u05D3\u05D7\u05D5\u05E4\u05D5\u05EA \u05DC\u05D4\u05D9\u05D5\u05DD" : more.toString().split("\n")[0])
                .setStyle(new Notification.BigTextStyle().bigText(more.length() == 0 ? "\u05D0\u05D9\u05DF \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA \u05D3\u05D7\u05D5\u05E4\u05D5\u05EA \u05DC\u05D4\u05D9\u05D5\u05DD" : more.toString().trim()))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();
        nm.notify(9988, n);
    }

    // ------------------------------------------------------------------ automatic backup

    static void autoBackupIfDue(Context c) {
        SharedPreferences p = CallLogger.prefs(c);
        int days = p.getInt(KEY_AUTO_DAYS, 0);
        String uri = p.getString(KEY_AUTO_URI, "");
        if (days <= 0 || uri.isEmpty()) return;
        long last = p.getLong(Backup.KEY_LAST, 0);
        if (System.currentTimeMillis() - last < days * AlarmManager.INTERVAL_DAY - 2 * AlarmManager.INTERVAL_HOUR) return;
        backupNow(c);
    }

    /** Writes the backup over the chosen file. Returns an error message or null. */
    static String backupNow(Context c) {
        SharedPreferences p = CallLogger.prefs(c);
        String uri = p.getString(KEY_AUTO_URI, "");
        if (uri.isEmpty()) return "\u05DC\u05D0 \u05E0\u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05D2\u05D9\u05D1\u05D5\u05D9";
        Exception last = null;
        for (String mode : new String[]{"wt", "w", "rwt"}) {
            try (OutputStream out = c.getContentResolver().openOutputStream(Uri.parse(uri), mode)) {
                if (out == null) continue;
                Backup.write(c, out);
                p.edit().remove(KEY_AUTO_ERR).apply();
                return null;
            } catch (Exception e) {
                last = e;
            }
        }
        String msg = last == null ? "\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05DB\u05EA\u05D5\u05D1 \u05DC\u05E7\u05D5\u05D1\u05E5" : String.valueOf(last.getMessage());
        p.edit().putString(KEY_AUTO_ERR, msg).apply();
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("backup", "\u05D2\u05D9\u05D1\u05D5\u05D9", NotificationManager.IMPORTANCE_DEFAULT));
            nm.notify(9987, new Notification.Builder(c, "backup")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle("\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05D4\u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9 \u05E0\u05DB\u05E9\u05DC")
                    .setContentText(msg)
                    .setAutoCancel(true)
                    .build());
        } catch (Exception ignored) { }
        return msg;
    }

    // ------------------------------------------------------------------ settings UI (from MainActivity)

    static void summarySettings(final Activity a, final Runnable after) {
        final SharedPreferences p = CallLogger.prefs(a);
        boolean on = p.getBoolean(KEY_SUMMARY, true);
        int hour = p.getInt(KEY_HOUR, 7);
        String[] items = {on ? "\uD83D\uDD15 \u05DB\u05D1\u05D4 \u05D0\u05EA \u05E1\u05D9\u05DB\u05D5\u05DD \u05D4\u05D1\u05D5\u05E7\u05E8" : "\uD83D\uDD14 \u05D4\u05E4\u05E2\u05DC \u05D0\u05EA \u05E1\u05D9\u05DB\u05D5\u05DD \u05D4\u05D1\u05D5\u05E7\u05E8", "\uD83D\uDD56 \u05E9\u05E2\u05D4: " + hour + ":30", "\uD83D\uDC40 \u05D4\u05E6\u05D2 \u05E1\u05D9\u05DB\u05D5\u05DD \u05E2\u05DB\u05E9\u05D9\u05D5"};
        new AlertDialog.Builder(a)
                .setTitle("\u2600\uFE0F \u05E1\u05D9\u05DB\u05D5\u05DD \u05D1\u05D5\u05E7\u05E8")
                .setItems(items, (d, w) -> {
                    if (w == 0) {
                        p.edit().putBoolean(KEY_SUMMARY, !on).apply();
                    } else if (w == 1) {
                        String[] hours = {"6:30", "7:30", "8:30", "9:30"};
                        new AlertDialog.Builder(a).setItems(hours, (d2, w2) -> {
                            p.edit().putInt(KEY_HOUR, 6 + w2).apply();
                            schedule(a);
                            if (after != null) after.run();
                        }).show();
                        return;
                    } else {
                        new Thread(() -> summary(a)).start();
                    }
                    if (after != null) after.run();
                })
                .show();
    }

    static void backupSettings(final Activity a, final Runnable after) {
        final SharedPreferences p = CallLogger.prefs(a);
        int days = p.getInt(KEY_AUTO_DAYS, 0);
        boolean hasFile = !p.getString(KEY_AUTO_URI, "").isEmpty();
        String[] items = {
                (days == 1 ? "\u2713 " : "") + "\u05D9\u05D5\u05DE\u05D9",
                (days == 7 ? "\u2713 " : "") + "\u05E9\u05D1\u05D5\u05E2\u05D9",
                (days == 0 ? "\u2713 " : "") + "\u05DB\u05D1\u05D5\u05D9",
                hasFile ? "\uD83D\uDCC1 \u05D4\u05D7\u05DC\u05E3 \u05E7\u05D5\u05D1\u05E5 \u05D9\u05E2\u05D3" : "\uD83D\uDCC1 \u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05D9\u05E2\u05D3 (\u05DC\u05DE\u05E9\u05DC \u05D1-Google Drive)",
                "\u25B6\uFE0F \u05D2\u05D1\u05D4 \u05E2\u05DB\u05E9\u05D9\u05D5"};
        new AlertDialog.Builder(a)
                .setTitle("\u2601\uFE0F \u05D2\u05D9\u05D1\u05D5\u05D9 \u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9")
                .setItems(items, (d, w) -> {
                    if (w <= 2) {
                        int nd = w == 0 ? 1 : w == 1 ? 7 : 0;
                        p.edit().putInt(KEY_AUTO_DAYS, nd).apply();
                        if (nd > 0 && !hasFile) pickFile(a);
                    } else if (w == 3) {
                        pickFile(a);
                    } else {
                        new Thread(() -> {
                            String err = backupNow(a);
                            a.runOnUiThread(() -> {
                                android.widget.Toast.makeText(a, err == null ? "\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05E0\u05E9\u05DE\u05E8 \u2713" : "\u05E0\u05DB\u05E9\u05DC: " + err, android.widget.Toast.LENGTH_LONG).show();
                                if (after != null) after.run();
                            });
                        }).start();
                    }
                    if (after != null) after.run();
                })
                .show();
    }

    private static void pickFile(Activity a) {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/zip")
                .putExtra(Intent.EXTRA_TITLE, "client-caller-auto-backup.zip")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        a.startActivityForResult(i, REQ_AUTO_BACKUP);
    }

    /** MainActivity.onActivityResult hook. */
    static boolean onResult(Activity a, int req, int res, Intent data) {
        if (req != REQ_AUTO_BACKUP) return false;
        if (res != Activity.RESULT_OK || data == null || data.getData() == null) return true;
        Uri u = data.getData();
        try {
            a.getContentResolver().takePersistableUriPermission(u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception ignored) { }
        SharedPreferences p = CallLogger.prefs(a);
        p.edit().putString(KEY_AUTO_URI, u.toString()).apply();
        if (p.getInt(KEY_AUTO_DAYS, 0) == 0) p.edit().putInt(KEY_AUTO_DAYS, 1).apply();
        new Thread(() -> {
            String err = backupNow(a);
            a.runOnUiThread(() -> android.widget.Toast.makeText(a,
                    err == null ? "\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05D4\u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9 \u05D4\u05D5\u05D2\u05D3\u05E8 \u05D5\u05E0\u05E9\u05DE\u05E8 \u05D2\u05D9\u05D1\u05D5\u05D9 \u05E8\u05D0\u05E9\u05D5\u05DF \u2713" : "\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E0\u05D1\u05D7\u05E8, \u05D0\u05D1\u05DC \u05D4\u05DB\u05EA\u05D9\u05D1\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + err,
                    android.widget.Toast.LENGTH_LONG).show());
        }).start();
        return true;
    }
}
