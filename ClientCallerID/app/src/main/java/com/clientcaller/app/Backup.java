package com.clientcaller.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** Backup of all app data (tickets, notes, providers, clients, settings) into one zip file. */
final class Backup {
    static final String KEY_LAST = "last_backup";
    static final String ACTION_REMIND = "com.clientcaller.app.BACKUP_REMINDER";
    static final String EXTRA_BACKUP = "do_backup";
    private static final String DB = "tickets.db";

    private Backup() { }

    static void write(Context c, OutputStream out) throws Exception {
        TicketDb.get(c).close();  // writes all pending changes into the database file
        SharedPreferences p = CallLogger.prefs(c);
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            addFile(z, DB, c.getDatabasePath(DB));
            addFile(z, Data.FILE, new File(c.getFilesDir(), Data.FILE));
            JSONObject j = new JSONObject();
            for (Map.Entry<String, ?> e : p.getAll().entrySet()) {
                Object v = e.getValue();
                JSONObject o = new JSONObject();
                if (v instanceof Long) o.put("t", "l");
                else if (v instanceof Boolean) o.put("t", "b");
                else if (v instanceof Integer) o.put("t", "i");
                else o.put("t", "s");
                o.put("v", String.valueOf(v));
                j.put(e.getKey(), o);
            }
            z.putNextEntry(new ZipEntry("settings.json"));
            z.write(j.toString().getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        p.edit().putLong(KEY_LAST, System.currentTimeMillis()).apply();
    }

    private static void addFile(ZipOutputStream z, String name, File f) throws IOException {
        if (!f.exists()) return;
        z.putNextEntry(new ZipEntry(name));
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) z.write(buf, 0, n);
        }
        z.closeEntry();
    }

    static void restore(Context c, InputStream in) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) files.put(e.getName(), ClientStore.readAll(z));
        }
        if (!files.containsKey(DB) && !files.containsKey(Data.FILE)) {
            throw new IOException("\u05D6\u05D4 \u05DC\u05D0 \u05E7\u05D5\u05D1\u05E5 \u05D2\u05D9\u05D1\u05D5\u05D9 \u05E9\u05DC \u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4");
        }
        TicketDb.get(c).close();
        if (files.containsKey(DB)) {
            File db = c.getDatabasePath(DB);
            File dir = db.getParentFile();
            if (dir != null) dir.mkdirs();
            for (String suffix : new String[]{"-wal", "-shm", "-journal"}) {
                new File(db.getPath() + suffix).delete();
            }
            writeFile(db, files.get(DB));
        }
        if (files.containsKey(Data.FILE)) writeFile(new File(c.getFilesDir(), Data.FILE), files.get(Data.FILE));
        if (files.containsKey("settings.json")) {
            JSONObject j = new JSONObject(new String(files.get("settings.json"), StandardCharsets.UTF_8));
            SharedPreferences.Editor ed = CallLogger.prefs(c).edit().clear();
            Iterator<String> keys = j.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                JSONObject o = j.getJSONObject(k);
                String t = o.getString("t"), v = o.getString("v");
                if ("l".equals(t)) ed.putLong(k, Long.parseLong(v));
                else if ("b".equals(t)) ed.putBoolean(k, Boolean.parseBoolean(v));
                else if ("i".equals(t)) ed.putInt(k, Integer.parseInt(v));
                else ed.putString(k, v);
            }
            ed.putLong(KEY_LAST, System.currentTimeMillis());
            ed.commit();
        }
        Data.reset();
        Followups.rescheduleAll(c);
    }

    private static void writeFile(File f, byte[] data) throws IOException {
        try (OutputStream o = new FileOutputStream(f)) {
            o.write(data);
        }
    }

    /** Daily check; reminds to back up when the last backup is a week old. */
    static void scheduleReminder(Context c) {
        try {
            Intent i = new Intent(c, FollowupReceiver.class).setAction(ACTION_REMIND);
            PendingIntent pi = PendingIntent.getBroadcast(c, 9999, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            c.getSystemService(AlarmManager.class).setInexactRepeating(AlarmManager.RTC,
                    System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_DAY, pi);
        } catch (Exception ignored) { }
    }

    static void remindIfNeeded(Context c) {
        long last = CallLogger.prefs(c).getLong(KEY_LAST, 0);
        if (System.currentTimeMillis() - last < 7L * AlarmManager.INTERVAL_DAY) return;
        if (TicketDb.get(c).all().isEmpty()) return;  // nothing to back up yet
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    "backup", "\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05D2\u05D9\u05D1\u05D5\u05D9", NotificationManager.IMPORTANCE_DEFAULT));
            Intent open = new Intent(c, MainActivity.class).putExtra(EXTRA_BACKUP, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(c, 9998, open,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = new Notification.Builder(c, "backup")
                    .setSmallIcon(android.R.drawable.ic_menu_save)
                    .setContentTitle("\u05D6\u05DE\u05DF \u05DC\u05D2\u05D1\u05D5\u05EA \u05D0\u05EA \u05D4\u05E0\u05EA\u05D5\u05E0\u05D9\u05DD")
                    .setContentText(last == 0 ? "\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2 \u05D2\u05D9\u05D1\u05D5\u05D9. \u05DC\u05D7\u05E5 \u05DC\u05D2\u05D9\u05D1\u05D5\u05D9."
                            : "\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05D4\u05D0\u05D7\u05E8\u05D5\u05DF: " + TicketDb.fmt(last) + ". \u05DC\u05D7\u05E5 \u05DC\u05D2\u05D9\u05D1\u05D5\u05D9.")
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(9997, n);
        } catch (Exception ignored) { }
    }
}
