package com.clientcaller.app;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.provider.CallLog;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Writes each finished call with a client into the phone's calendar. */
final class CallLogger {
    static final String PREFS = "settings";
    static final String KEY_CAL_ID = "calendar_id";      // 0 = automatic, -1 = off
    static final String KEY_CAL_NAME = "calendar_name";
    static final String KEY_UNKNOWN = "log_unknown";
    private static final String KEY_LAST_CALL = "last_call_id";

    private CallLogger() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean hasPermissions(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                && c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                && c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    /** Called a few seconds after a call ends: reads the newest call-log entry and logs it. */
    static void logLatestCall(Context c) {
        try {
            if (!hasPermissions(c)) return;
            SharedPreferences p = prefs(c);
            long calId = p.getLong(KEY_CAL_ID, 0);
            if (calId < 0) return;
            if (calId == 0) calId = defaultCalendar(c);
            if (calId <= 0) return;

            long id, date, duration;
            String number;
            int type;
            String[] proj = {CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                    CallLog.Calls.DATE, CallLog.Calls.DURATION};
            try (Cursor cur = c.getContentResolver().query(CallLog.Calls.CONTENT_URI, proj,
                    null, null, CallLog.Calls.DATE + " DESC")) {
                if (cur == null || !cur.moveToFirst()) return;
                id = cur.getLong(0);
                number = cur.getString(1);
                type = cur.getInt(2);
                date = cur.getLong(3);
                duration = cur.getLong(4);
            }
            if (id <= p.getLong(KEY_LAST_CALL, 0)) return;  // already logged
            long endedAt = date + duration * 1000;
            if (System.currentTimeMillis() - endedAt > 10 * 60 * 1000) return;  // old entry
            p.edit().putLong(KEY_LAST_CALL, id).apply();

            ClientStore s = Data.get(c);
            int row = number == null ? -1 : s.find(number);
            if (row < 0 && !p.getBoolean(KEY_UNKNOWN, false)) return;

            String who = row >= 0 ? s.name(row)
                    : (number == null || number.isEmpty() ? "Private number" : number);
            String kind;
            if (type == CallLog.Calls.INCOMING_TYPE) kind = "Incoming call";
            else if (type == CallLog.Calls.OUTGOING_TYPE) kind = "Outgoing call";
            else if (type == CallLog.Calls.MISSED_TYPE) kind = "Missed call";
            else if (type == CallLog.Calls.REJECTED_TYPE) kind = "Rejected call";
            else if (type == CallLog.Calls.BLOCKED_TYPE) kind = "Blocked call";
            else kind = "Call";

            StringBuilder desc = new StringBuilder();
            desc.append("Phone: ").append(number == null ? "" : number);
            if (duration > 0) {
                desc.append("\nDuration: ").append(String.format(Locale.US, "%d:%02d", duration / 60, duration % 60));
            }
            if (row >= 0) desc.append("\n\n").append(s.details(row));

            ContentValues v = new ContentValues();
            v.put(CalendarContract.Events.CALENDAR_ID, calId);
            v.put(CalendarContract.Events.TITLE, "📞 " + kind + ": " + who);
            v.put(CalendarContract.Events.DESCRIPTION, desc.toString());
            v.put(CalendarContract.Events.DTSTART, date);
            v.put(CalendarContract.Events.DTEND, date + Math.max(duration * 1000, 15 * 60 * 1000));
            v.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, v);
        } catch (Exception ignored) {
        }
    }

    /** Writable calendars on the phone: ids and "Name (account)" labels. */
    static void listCalendars(Context c, List<Long> ids, List<String> labels) {
        String[] proj = {CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME};
        String sel = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        try (Cursor cur = c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                proj, sel, null, null)) {
            while (cur != null && cur.moveToNext()) {
                ids.add(cur.getLong(0));
                String name = cur.getString(1);
                String acc = cur.getString(2);
                labels.add((name == null ? "Calendar" : name)
                        + (acc == null || acc.equals(name) ? "" : "  (" + acc + ")"));
            }
        } catch (Exception ignored) {
        }
    }

    /** The phone's main calendar: a primary Google calendar if there is one. */
    static long defaultCalendar(Context c) {
        String[] proj = {CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.ACCOUNT_TYPE};
        String sel = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR;
        long first = -1, primary = -1;
        try (Cursor cur = c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                proj, sel, null, null)) {
            while (cur != null && cur.moveToNext()) {
                long id = cur.getLong(0);
                boolean isPrimary = cur.getInt(1) == 1;
                boolean google = "com.google".equals(cur.getString(2));
                if (first < 0) first = id;
                if (isPrimary && (primary < 0 || google)) primary = id;
            }
        } catch (Exception ignored) {
        }
        return primary > 0 ? primary : first;
    }
}
