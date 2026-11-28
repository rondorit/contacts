package com.clientcaller.app;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds a date/time in a spoken Hebrew sentence ("\u05DE\u05D7\u05E8 \u05D1\u05E2\u05E9\u05E8", "\u05D1\u05D9\u05D5\u05DD \u05E9\u05DC\u05D9\u05E9\u05D9 \u05D1-14:30", "\u05D1\u05E2\u05D5\u05D3 \u05E9\u05E2\u05EA\u05D9\u05D9\u05DD"). */
final class HebrewWhen {
    static final class Parsed {
        String rest = "";
        long when;
    }

    private static final String[] DAYS = {"\u05E8\u05D0\u05E9\u05D5\u05DF", "\u05E9\u05E0\u05D9", "\u05E9\u05DC\u05D9\u05E9\u05D9", "\u05E8\u05D1\u05D9\u05E2\u05D9", "\u05D7\u05DE\u05D9\u05E9\u05D9", "\u05E9\u05D9\u05E9\u05D9", "\u05E9\u05D1\u05EA"};
    private static final String[] NUMS = {"\u05D0\u05D7\u05EA", "\u05E9\u05EA\u05D9\u05D9\u05DD", "\u05E9\u05DC\u05D5\u05E9", "\u05D0\u05E8\u05D1\u05E2", "\u05D7\u05DE\u05E9", "\u05E9\u05E9", "\u05E9\u05D1\u05E2", "\u05E9\u05DE\u05D5\u05E0\u05D4", "\u05EA\u05E9\u05E2", "\u05E2\u05E9\u05E8", "\u05D0\u05D7\u05EA \u05E2\u05E9\u05E8\u05D4", "\u05E9\u05EA\u05D9\u05DD \u05E2\u05E9\u05E8\u05D4"};

    private HebrewWhen() { }

    static Parsed parse(String text) {
        Parsed p = new Parsed();
        String s = " " + text.trim() + " ";
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        boolean date = false, time = false;

        // in X minutes / hours / days
        Matcher m = Pattern.compile("\\s(?:\u05D1\u05E2\u05D5\u05D3|\u05E2\u05D5\u05D3)\\s+(\\d+|\u05E9\u05E2\u05D4|\u05E9\u05E2\u05EA\u05D9\u05D9\u05DD|\u05D7\u05E6\u05D9 \u05E9\u05E2\u05D4|\u05D9\u05D5\u05DE\u05D9\u05D9\u05DD|\u05E9\u05D1\u05D5\u05E2|\u05E8\u05D1\u05E2 \u05E9\u05E2\u05D4)\\s*(\u05D3\u05E7\u05D5\u05EA|\u05E9\u05E2\u05D5\u05EA|\u05D9\u05DE\u05D9\u05DD|\u05D9\u05D5\u05DD|\u05E9\u05D1\u05D5\u05E2\u05D5\u05EA)?\\s").matcher(s);
        if (m.find()) {
            String n = m.group(1), u = m.group(2) == null ? "" : m.group(2);
            long now = System.currentTimeMillis();
            long add;
            switch (n) {
                case "\u05E9\u05E2\u05D4": add = 3600_000L; break;
                case "\u05E9\u05E2\u05EA\u05D9\u05D9\u05DD": add = 7200_000L; break;
                case "\u05D7\u05E6\u05D9 \u05E9\u05E2\u05D4": add = 1800_000L; break;
                case "\u05E8\u05D1\u05E2 \u05E9\u05E2\u05D4": add = 900_000L; break;
                case "\u05D9\u05D5\u05DE\u05D9\u05D9\u05DD": add = 2 * 86400_000L; break;
                case "\u05E9\u05D1\u05D5\u05E2": add = 7 * 86400_000L; break;
                default:
                    long k = Long.parseLong(n);
                    add = u.startsWith("\u05D3\u05E7") ? k * 60_000L : u.startsWith("\u05E9\u05E2") ? k * 3600_000L
                            : u.startsWith("\u05E9\u05D1\u05D5\u05E2") ? k * 7 * 86400_000L : u.isEmpty() ? k * 60_000L : k * 86400_000L;
            }
            c.setTimeInMillis(now + add);
            s = s.substring(0, m.start()) + " " + s.substring(m.end());
            p.when = c.getTimeInMillis();
            p.rest = clean(s);
            return p;
        }

        // day words
        if (cut(s, "\u05DE\u05D7\u05E8\u05EA\u05D9\u05D9\u05DD") != null) { s = cut(s, "\u05DE\u05D7\u05E8\u05EA\u05D9\u05D9\u05DD"); c.add(Calendar.DAY_OF_MONTH, 2); date = true; }
        else if (cut(s, "\u05DE\u05D7\u05E8") != null) { s = cut(s, "\u05DE\u05D7\u05E8"); c.add(Calendar.DAY_OF_MONTH, 1); date = true; }
        else if (cut(s, "\u05D4\u05D9\u05D5\u05DD") != null) { s = cut(s, "\u05D4\u05D9\u05D5\u05DD"); date = true; }
        else {
            for (int i = 0; i < DAYS.length; i++) {
                Matcher d = Pattern.compile("\\s(?:\u05D1?\u05D9\u05D5\u05DD\\s+)?" + DAYS[i] + "(?:\\s+\u05D4\u05E7\u05E8\u05D5\u05D1)?\\s").matcher(s);
                if (d.find() && (s.contains("\u05D9\u05D5\u05DD " + DAYS[i]) || s.contains("\u05D1" + DAYS[i]) || i >= 2)) {
                    int target = i + 1; // Calendar.SUNDAY = 1
                    int diff = (target - c.get(Calendar.DAY_OF_WEEK) + 7) % 7;
                    if (diff == 0) diff = 7;
                    c.add(Calendar.DAY_OF_MONTH, diff);
                    s = s.substring(0, d.start()) + " " + s.substring(d.end());
                    date = true;
                    break;
                }
            }
        }
        // dd/mm
        Matcher dm = Pattern.compile("\\s\u05D1?-?(\\d{1,2})[./](\\d{1,2})\\s").matcher(s);
        if (dm.find()) {
            int day = Integer.parseInt(dm.group(1)), mon = Integer.parseInt(dm.group(2)) - 1;
            if (day >= 1 && day <= 31 && mon >= 0 && mon <= 11) {
                c.set(Calendar.MONTH, mon);
                c.set(Calendar.DAY_OF_MONTH, day);
                if (c.getTimeInMillis() < System.currentTimeMillis() - 86400_000L) c.add(Calendar.YEAR, 1);
                s = s.substring(0, dm.start()) + " " + s.substring(dm.end());
                date = true;
            }
        }

        // time: "\u05D1-14:30", "\u05D1\u05E9\u05E2\u05D4 10", "\u05D1\u05E2\u05E9\u05E8", "\u05D1 9"
        Matcher hm = Pattern.compile("\\s(?:\u05D1\u05E9\u05E2\u05D4|\u05D1-?|\u05D1\\s)\\s*(\\d{1,2})(?::(\\d{2}))?\\s").matcher(s);
        int hour = -1, min = 0;
        if (hm.find()) {
            hour = Integer.parseInt(hm.group(1));
            if (hm.group(2) != null) min = Integer.parseInt(hm.group(2));
            s = s.substring(0, hm.start()) + " " + s.substring(hm.end());
        } else {
            for (int i = NUMS.length - 1; i >= 0; i--) {
                Matcher w = Pattern.compile("\\s(?:\u05D1\u05E9\u05E2\u05D4\\s+|\u05D1)" + NUMS[i] + "(\\s+\u05D5\u05D7\u05E6\u05D9|\\s+\u05D5\u05E8\u05D1\u05E2)?\\s").matcher(s);
                if (w.find()) {
                    hour = i + 1;
                    if (w.group(1) != null) min = w.group(1).contains("\u05D7\u05E6\u05D9") ? 30 : 15;
                    s = s.substring(0, w.start()) + " " + s.substring(w.end());
                    break;
                }
            }
        }
        if (cut(s, "\u05D1\u05D1\u05D5\u05E7\u05E8") != null) { s = cut(s, "\u05D1\u05D1\u05D5\u05E7\u05E8"); if (hour < 0) hour = 9; }
        else if (cut(s, "\u05D1\u05E6\u05D4\u05E8\u05D9\u05D9\u05DD") != null) { s = cut(s, "\u05D1\u05E6\u05D4\u05E8\u05D9\u05D9\u05DD"); if (hour < 0) hour = 12; else if (hour < 6) hour += 12; }
        else if (cut(s, "\u05D0\u05D7\u05E8 \u05D4\u05E6\u05D4\u05E8\u05D9\u05D9\u05DD") != null) { s = cut(s, "\u05D0\u05D7\u05E8 \u05D4\u05E6\u05D4\u05E8\u05D9\u05D9\u05DD"); if (hour < 0) hour = 15; else if (hour < 12) hour += 12; }
        else if (cut(s, "\u05D1\u05E2\u05E8\u05D1") != null) { s = cut(s, "\u05D1\u05E2\u05E8\u05D1"); if (hour < 0) hour = 19; else if (hour < 12) hour += 12; }
        else if (hour >= 1 && hour <= 7) hour += 12; // "\u05D1\u05E9\u05DC\u05D5\u05E9" during work = 15:00
        if (hour >= 0 && hour <= 23) {
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            time = true;
            if (!date && c.getTimeInMillis() < System.currentTimeMillis()) c.add(Calendar.DAY_OF_MONTH, 1);
        } else if (date) {
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.set(Calendar.MINUTE, 0);
        }
        p.when = date || time ? c.getTimeInMillis() : 0;
        p.rest = clean(s);
        return p;
    }

    private static String cut(String s, String word) {
        Matcher m = Pattern.compile("\\s(?:\u05D5?\u05D1?-?)" + word + "\\s").matcher(s);
        return m.find() ? s.substring(0, m.start()) + " " + s.substring(m.end()) : null;
    }

    private static String clean(String s) {
        String r = s.replaceAll("\\s+", " ").trim();
        if (r.isEmpty()) return r;
        return r.substring(0, 1).toUpperCase() + r.substring(1);
    }
}
