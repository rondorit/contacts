package com.clientcaller.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Turns missed calls (phone and WhatsApp) into a "call back" task for the same work day. */
final class MissedCalls {
    static final String KEY_MODE = "missed_task_mode"; // 0 off, 1 clients & contacts, 2 every number
    private static final String KEY_LAST = "missed_task_last";

    private MissedCalls() { }

    static int mode(Context c) {
        return CallLogger.prefs(c).getInt(KEY_MODE, 1);
    }

    /** Called a few seconds after a call ends. */
    static void check(Context c) {
        if (mode(c) == 0) return;
        if (c.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return;
        SharedPreferences p = CallLogger.prefs(c);
        String[] proj = {CallLog.Calls._ID, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE};
        try (Cursor cur = c.getContentResolver().query(CallLog.Calls.CONTENT_URI, proj, null, null, CallLog.Calls.DATE + " DESC")) {
            if (cur == null || !cur.moveToFirst()) return;
            long id = cur.getLong(0);
            String number = cur.getString(1);
            int type = cur.getInt(2);
            long date = cur.getLong(3);
            if (id <= p.getLong(KEY_LAST, 0)) return;
            if (System.currentTimeMillis() - date > 10 * 60 * 1000) return;
            p.edit().putLong(KEY_LAST, id).apply();
            if (type != CallLog.Calls.MISSED_TYPE) return;
            if (number == null || number.trim().isEmpty()) return;
            ClientStore s = Data.get(c);
            int row = s.find(number);
            String contact = row < 0 ? PhoneContacts.name(c, number) : null;
            if (row < 0 && contact == null && mode(c) != 2) return;
            String who = row >= 0 ? s.name(row) : contact != null ? contact : number;
            add(c, who, number, "\uD83D\uDCDE", date);
        } catch (Exception ignored) { }
    }

    /** WhatsApp "missed voice/video call" notification. */
    static String fromWhatsApp(Context c, List<String> names) {
        if (mode(c) == 0) return "\u05E9\u05D9\u05D7\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 (\u05D9\u05E6\u05D9\u05E8\u05EA \u05DE\u05E9\u05D9\u05DE\u05D4 \u05DB\u05D1\u05D5\u05D9\u05D4)";
        String who = null, phone = "";
        for (String n : names) {
            int row = CallPopup.findClientRow(c, n);
            if (row >= 0) {
                ClientStore s = Data.get(c);
                who = s.name(row);
                List<String> keys = s.keysForRow(row);
                if (!keys.isEmpty()) phone = "0" + keys.get(0);
                break;
            }
        }
        if (who == null) {
            for (String n : names) {
                try {
                    List<String> nums = PhoneContacts.numbersForName(c, n);
                    if (!nums.isEmpty()) { who = n; phone = nums.get(0); break; }
                } catch (Exception ignored) { }
            }
        }
        if (who == null && mode(c) != 2) return "\u05E9\u05D9\u05D7\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 \u05DE\u05DE\u05E1\u05E4\u05E8 \u05DC\u05D0 \u05DE\u05D5\u05DB\u05E8 \u2013 \u05DC\u05D0 \u05E0\u05D5\u05E6\u05E8\u05D4 \u05DE\u05E9\u05D9\u05DE\u05D4";
        if (who == null) who = names.isEmpty() ? "?" : names.get(0);
        add(c, who, phone, "\uD83D\uDCAC", System.currentTimeMillis());
        return "\u05E9\u05D9\u05D7\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 \u05DE" + who + " \u2192 \u05DE\u05E9\u05D9\u05DE\u05D4";
    }

    /** Creates the task, or adds to today's open one for the same caller. */
    private static void add(Context c, String who, String phone, String icon, long when) {
        String hm = String.format(Locale.ROOT, "%tH:%<tM", when);
        String key = ClientStore.key(phone);
        Calendar today = Calendar.getInstance();
        for (Tasks.Task t : Tasks.all(c)) {
            if (t.isDone() || !t.title.startsWith("\uD83D\uDCDE \u05DC\u05D7\u05D6\u05D5\u05E8")) continue;
            boolean same = (!key.isEmpty() && key.length() >= 7 && key.equals(ClientStore.key(t.phone))) || t.client.equalsIgnoreCase(who);
            Calendar cr = Calendar.getInstance();
            cr.setTimeInMillis(t.created);
            if (same && cr.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) && cr.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
                t.notes = t.notes + "\n" + icon + " \u05E9\u05D9\u05D7\u05D4 \u05E0\u05D5\u05E1\u05E4\u05EA \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 \u05D1-" + hm;
                Tasks.save(c, t);
                return;
            }
        }
        Tasks.Task t = new Tasks.Task();
        t.title = "\uD83D\uDCDE \u05DC\u05D7\u05D6\u05D5\u05E8 \u05DC" + who;
        t.client = who;
        t.phone = phone == null ? "" : phone;
        t.notes = icon + " \u05E9\u05D9\u05D7\u05D4 \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 \u05D1-" + hm + (t.phone.isEmpty() ? "" : " \u2022 " + t.phone);
        t.priority = "High";
        t.dueAt = dueTime(when);
        t.remind = true;
        Tasks.save(c, t);
    }

    /**
     * Same work day: one hour after the call, but not after the end of the day
     * (Sun\u2013Thu 17:00, Fri 13:00). Outside work hours: the next work day at 09:00.
     */
    static long dueTime(long when) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(when);
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int endHour = dow == Calendar.FRIDAY ? 13 : 17;
        boolean workDay = dow != Calendar.SATURDAY;
        Calendar end = (Calendar) c.clone();
        end.set(Calendar.HOUR_OF_DAY, endHour);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        if (workDay && c.before(end)) {
            long due = when + 60 * 60 * 1000L;
            Calendar start = (Calendar) c.clone();
            start.set(Calendar.HOUR_OF_DAY, 9);
            start.set(Calendar.MINUTE, 0);
            if (due < start.getTimeInMillis()) due = start.getTimeInMillis();
            return Math.min(due, end.getTimeInMillis());
        }
        Calendar next = (Calendar) c.clone();
        do {
            next.add(Calendar.DAY_OF_MONTH, 1);
        } while (next.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY);
        next.set(Calendar.HOUR_OF_DAY, 9);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        return next.getTimeInMillis();
    }

    static void settings(final Activity a, final Runnable after) {
        String[] opts = {"\u05DB\u05D1\u05D5\u05D9", "\u05E8\u05E7 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8", "\u05DB\u05DC \u05DE\u05E1\u05E4\u05E8"};
        new AlertDialog.Builder(a)
                .setTitle("\u05E9\u05D9\u05D7\u05D4 \u05E9\u05DC\u05D0 \u05E0\u05E2\u05E0\u05EA\u05D4 \u2190 \u05DE\u05E9\u05D9\u05DE\u05D4 \u05DC\u05D4\u05D9\u05D5\u05DD")
                .setSingleChoiceItems(opts, mode(a), (d, w) -> {
                    CallLogger.prefs(a).edit().putInt(KEY_MODE, w).apply();
                    d.dismiss();
                    if (after != null) after.run();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }
}
