package com.clientcaller.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.CalendarContract;

import java.util.TimeZone;

/** Follow-up reminders (phone alarm) and follow-up entries in the calendar. */
final class Followups {
    private Followups() { }

    private static PendingIntent alarmIntent(Context c, long ticketId) {
        Intent i = new Intent(c, FollowupReceiver.class)
                .setAction(FollowupReceiver.ACTION)
                .putExtra(TicketActivity.EXTRA_TICKET, ticketId);
        return PendingIntent.getBroadcast(c, (int) ticketId, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Sets (or cancels) the reminder alarm for a ticket. */
    static void schedule(Context c, TicketDb.Ticket t) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            PendingIntent pi = alarmIntent(c, t.id);
            am.cancel(pi);
            if (t.isOpen() && t.followupAt > System.currentTimeMillis()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.followupAt, pi);
            }
        } catch (Exception ignored) { }
    }

    static void cancel(Context c, long ticketId) {
        try {
            c.getSystemService(AlarmManager.class).cancel(alarmIntent(c, ticketId));
        } catch (Exception ignored) { }
    }

    /** After a reboot alarms are lost: set them again for all open tickets. */
    static void rescheduleAll(Context c) {
        for (TicketDb.Ticket t : TicketDb.get(c).all()) schedule(c, t);
    }

    private static boolean calendarAllowed(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                && c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Creates, moves or removes the ticket's follow-up entry in the calendar.
     * Updates t.eventId; the caller saves the ticket afterwards.
     */
    static void syncCalendar(Context c, TicketDb.Ticket t) {
        try {
            if (!calendarAllowed(c)) return;
            ContentResolver cr = c.getContentResolver();
            if (!t.isOpen() || t.followupAt == 0) {
                removeEvent(c, t);
                return;
            }
            String title = "\uD83D\uDD27 \u05DE\u05E2\u05E7\u05D1: "
                    + (t.clientName.isEmpty() ? "" : t.clientName + " - ") + t.title;
            ContentValues v = new ContentValues();
            v.put(CalendarContract.Events.DESCRIPTION, TicketDb.get(c).summary(t));
            v.put(CalendarContract.Events.TITLE, CallLogger.putTime(c, v, title, t.followupAt, t.followupAt + 30 * 60 * 1000L));
            if (t.eventId > 0) {
                Uri u = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, t.eventId);
                if (cr.update(u, v, null, null) > 0) return;
            }
            long calId = CallLogger.prefs(c).getLong(CallLogger.KEY_CAL_ID, 0);
            if (calId <= 0) calId = CallLogger.defaultCalendar(c);
            if (calId <= 0) return;
            v.put(CalendarContract.Events.CALENDAR_ID, calId);
            Uri created = cr.insert(CalendarContract.Events.CONTENT_URI, v);
            if (created != null) t.eventId = ContentUris.parseId(created);
        } catch (Exception ignored) { }
    }

    static final class CalEvent {
        long id, start;
        String title = "", description = "";
    }

    /** Calendar events whose title mentions this client (call logs, follow-ups...). */
    static java.util.List<CalEvent> eventsFor(Context c, String name) {
        java.util.List<CalEvent> out = new java.util.ArrayList<>();
        if (name == null || name.trim().isEmpty() || !calendarAllowed(c)) return out;
        String[] proj = {CalendarContract.Events._ID, CalendarContract.Events.DTSTART,
                CalendarContract.Events.TITLE, CalendarContract.Events.DESCRIPTION};
        String sel = CalendarContract.Events.TITLE + " LIKE ? AND " + CalendarContract.Events.DELETED + "=0";
        try (android.database.Cursor cur = c.getContentResolver().query(CalendarContract.Events.CONTENT_URI,
                proj, sel, new String[]{"%" + name.trim() + "%"}, CalendarContract.Events.DTSTART + " DESC")) {
            while (cur != null && cur.moveToNext() && out.size() < 100) {
                CalEvent e = new CalEvent();
                e.id = cur.getLong(0);
                e.start = cur.getLong(1);
                e.title = cur.getString(2) == null ? "" : cur.getString(2);
                e.description = cur.getString(3) == null ? "" : cur.getString(3);
                out.add(e);
            }
        } catch (Exception ignored) { }
        return out;
    }

    static boolean deleteEvent(Context c, long eventId) {
        try {
            return calendarAllowed(c) && c.getContentResolver().delete(
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), null, null) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    static void removeEvent(Context c, TicketDb.Ticket t) {
        try {
            if (t.eventId > 0 && calendarAllowed(c)) {
                c.getContentResolver().delete(
                        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, t.eventId), null, null);
            }
        } catch (Exception ignored) { }
        t.eventId = 0;
    }
}
