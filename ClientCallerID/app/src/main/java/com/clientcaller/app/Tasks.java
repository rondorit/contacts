package com.clientcaller.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Personal tasks / to-dos: due date with reminder, priority, optional client, team member and ticket link. */
final class Tasks {
    static final String ACTION_REMIND = "com.clientcaller.app.TASK_REMIND";
    static final String ACTION_DONE = "com.clientcaller.app.TASK_DONE";
    static final String ACTION_SNOOZE = "com.clientcaller.app.TASK_SNOOZE";
    static final String EXTRA_ID = "task_id";
    static final String[] PRIORITIES = {"Low", "Normal", "High"};
    static final String[] PRIORITY_HE = {"\u05E0\u05DE\u05D5\u05DB\u05D4", "\u05E8\u05D2\u05D9\u05DC\u05D4", "\u05D2\u05D1\u05D5\u05D4\u05D4"};

    static final class Task {
        long id, dueAt, created, doneAt, ticketId;
        boolean remind = true;
        String title = "", notes = "", priority = "Normal", client = "", phone = "", assignee = "", place = "";
        double lat, lng;
        int radius;

        boolean hasPlace() {
            return !place.isEmpty() && (lat != 0 || lng != 0);
        }

        boolean isDone() {
            return doneAt > 0;
        }

        boolean isOverdue() {
            return !isDone() && dueAt > 0 && dueAt < System.currentTimeMillis();
        }

        boolean isToday() {
            if (dueAt == 0) return false;
            Calendar a = Calendar.getInstance(), b = Calendar.getInstance();
            b.setTimeInMillis(dueAt);
            return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
        }
    }

    private Tasks() { }

    static void create(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tasks (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, notes TEXT, priority TEXT,"
                + " due_at INTEGER, remind INTEGER, client TEXT, phone TEXT, assignee TEXT, ticket_id INTEGER,"
                + " created INTEGER, done_at INTEGER, place TEXT, lat REAL, lng REAL, radius INTEGER)");
    }

    static void addPlace(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN place TEXT");
        db.execSQL("ALTER TABLE tasks ADD COLUMN lat REAL");
        db.execSQL("ALTER TABLE tasks ADD COLUMN lng REAL");
        db.execSQL("ALTER TABLE tasks ADD COLUMN radius INTEGER");
    }

    private static final String[] COLS = {"_id", "title", "notes", "priority", "due_at", "remind", "client", "phone", "assignee",
            "ticket_id", "created", "done_at", "place", "lat", "lng", "radius"};

    private static Task read(Cursor c) {
        Task t = new Task();
        t.id = c.getLong(0);
        t.title = s(c, 1);
        t.notes = s(c, 2);
        t.priority = s(c, 3).isEmpty() ? "Normal" : s(c, 3);
        t.dueAt = c.getLong(4);
        t.remind = c.getInt(5) != 0;
        t.client = s(c, 6);
        t.phone = s(c, 7);
        t.assignee = s(c, 8);
        t.ticketId = c.getLong(9);
        t.created = c.getLong(10);
        t.doneAt = c.getLong(11);
        t.place = s(c, 12);
        t.lat = c.getDouble(13);
        t.lng = c.getDouble(14);
        t.radius = c.getInt(15);
        return t;
    }

    private static String s(Cursor c, int i) {
        String v = c.getString(i);
        return v == null ? "" : v;
    }

    static List<Task> all(Context c) {
        List<Task> out = new ArrayList<>();
        try (Cursor cur = TicketDb.get(c).getReadableDatabase().query("tasks", COLS, null, null, null, null, null)) {
            while (cur.moveToNext()) out.add(read(cur));
        }
        // Open first (by due date, no date last), then done (newest first).
        java.util.Collections.sort(out, (a, b) -> {
            if (a.isDone() != b.isDone()) return a.isDone() ? 1 : -1;
            if (a.isDone()) return Long.compare(b.doneAt, a.doneAt);
            long da = a.dueAt == 0 ? Long.MAX_VALUE : a.dueAt, db = b.dueAt == 0 ? Long.MAX_VALUE : b.dueAt;
            if (da != db) return Long.compare(da, db);
            return Integer.compare(rank(b.priority), rank(a.priority));
        });
        return out;
    }

    static int rank(String p) {
        return "High".equals(p) ? 2 : "Low".equals(p) ? 0 : 1;
    }

    static String priorityHe(String p) {
        return PRIORITY_HE[rank(p)];
    }

    static Task load(Context c, long id) {
        try (Cursor cur = TicketDb.get(c).getReadableDatabase().query("tasks", COLS, "_id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return cur.moveToFirst() ? read(cur) : null;
        }
    }

    static void save(Context c, Task t) {
        ContentValues v = new ContentValues();
        v.put("title", t.title);
        v.put("notes", t.notes);
        v.put("priority", t.priority);
        v.put("due_at", t.dueAt);
        v.put("remind", t.remind ? 1 : 0);
        v.put("client", t.client);
        v.put("phone", t.phone);
        v.put("assignee", t.assignee);
        v.put("ticket_id", t.ticketId);
        if (t.created == 0) t.created = System.currentTimeMillis();
        v.put("created", t.created);
        v.put("done_at", t.doneAt);
        v.put("place", t.place);
        v.put("lat", t.lat);
        v.put("lng", t.lng);
        v.put("radius", t.radius);
        if (t.id == 0) t.id = TicketDb.get(c).getWritableDatabase().insert("tasks", null, v);
        else TicketDb.get(c).getWritableDatabase().update("tasks", v, "_id=?", new String[]{String.valueOf(t.id)});
        schedule(c, t);
        Geo.sync(c, t);
        TicketsWidget.updateAll(c);
        TasksWidget.updateAll(c);
    }

    static void delete(Context c, long id) {
        cancel(c, id);
        Geo.remove(c, id);
        TicketDb.get(c).getWritableDatabase().delete("tasks", "_id=?", new String[]{String.valueOf(id)});
    }

    static void setDone(Context c, long id, boolean done) {
        Task t = load(c, id);
        if (t == null) return;
        t.doneAt = done ? System.currentTimeMillis() : 0;
        save(c, t);
        try { c.getSystemService(NotificationManager.class).cancel(800000 + (int) id); } catch (Exception ignored) { }
    }

    static int openCount(Context c) {
        int n = 0;
        for (Task t : all(c)) if (!t.isDone()) n++;
        return n;
    }

    // ------------------------------------------------------------------ reminders

    private static PendingIntent remindIntent(Context c, long id) {
        Intent i = new Intent(c, FollowupReceiver.class).setAction(ACTION_REMIND).putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, 800000 + (int) id, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void schedule(Context c, Task t) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            PendingIntent pi = remindIntent(c, t.id);
            am.cancel(pi);
            if (!t.isDone() && t.remind && t.dueAt > System.currentTimeMillis())
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.dueAt, pi);
        } catch (Exception ignored) { }
    }

    static void cancel(Context c, long id) {
        try { c.getSystemService(AlarmManager.class).cancel(remindIntent(c, id)); } catch (Exception ignored) { }
    }

    static void rescheduleAll(Context c) {
        for (Task t : all(c)) {
            schedule(c, t);
            Geo.sync(c, t);
        }
    }

    /** Receiver entry point for the reminder / done / snooze actions. */
    static void onReceive(Context c, Intent intent) {
        long id = intent.getLongExtra(EXTRA_ID, 0);
        String a = intent.getAction();
        if (ACTION_DONE.equals(a)) {
            setDone(c, id, true);
            return;
        }
        Task t = load(c, id);
        if (t == null || t.isDone()) return;
        if (ACTION_SNOOZE.equals(a)) {
            t.dueAt = System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR;
            save(c, t);
            try { c.getSystemService(NotificationManager.class).cancel(800000 + (int) id); } catch (Exception ignored) { }
            return;
        }
        notify(c, t);
    }

    static void notify(Context c, Task t) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("tasks", "\u05EA\u05D6\u05DB\u05D5\u05E8\u05D5\u05EA \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA", NotificationManager.IMPORTANCE_HIGH));
        int nid = 800000 + (int) t.id;
        int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Intent open = new Intent(c, TasksActivity.class).putExtra(EXTRA_ID, t.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent done = new Intent(c, FollowupReceiver.class).setAction(ACTION_DONE).putExtra(EXTRA_ID, t.id);
        Intent snooze = new Intent(c, FollowupReceiver.class).setAction(ACTION_SNOOZE).putExtra(EXTRA_ID, t.id);
        String sub = (t.client.isEmpty() ? "" : t.client + " \u2022 ") + (t.notes.isEmpty() ? "" : t.notes);
        nm.notify(nid, new Notification.Builder(c, "tasks")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setContentTitle(("High".equals(t.priority) ? "\uD83D\uDD34 " : "\u2705 ") + t.title)
                .setContentText(sub.isEmpty() ? "\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05DE\u05E9\u05D9\u05DE\u05D4" : sub)
                .setStyle(new Notification.BigTextStyle().bigText(sub.isEmpty() ? "\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05DE\u05E9\u05D9\u05DE\u05D4" : sub))
                .setContentIntent(PendingIntent.getActivity(c, nid, open, flags))
                .addAction(new Notification.Action.Builder(null, "\u2714 \u05D1\u05D5\u05E6\u05E2", PendingIntent.getBroadcast(c, nid + 1, done, flags)).build())
                .addAction(new Notification.Action.Builder(null, "\u23F0 \u05E2\u05D5\u05D3 \u05E9\u05E2\u05D4", PendingIntent.getBroadcast(c, nid + 2, snooze, flags)).build())
                .setAutoCancel(true)
                .addAction(t.phone.isEmpty() ? null : new Notification.Action.Builder(null, "\uD83D\uDCDE \u05D7\u05D9\u05D9\u05D2",
                        PendingIntent.getActivity(c, nid + 3, new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + t.phone))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags)).build())
                .build());
    }
}
