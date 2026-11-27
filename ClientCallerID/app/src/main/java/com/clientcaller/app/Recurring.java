package com.clientcaller.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Recurring maintenance: opens a service ticket automatically every N days. */
final class Recurring {
    static final int[] DAYS = {7, 14, 30, 60, 90, 180, 365};
    static final String[] DAY_LABELS = {"\u05DB\u05DC \u05E9\u05D1\u05D5\u05E2", "\u05DB\u05DC \u05E9\u05D1\u05D5\u05E2\u05D9\u05D9\u05DD", "\u05DB\u05DC \u05D7\u05D5\u05D3\u05E9", "\u05DB\u05DC \u05D7\u05D5\u05D3\u05E9\u05D9\u05D9\u05DD", "\u05DB\u05DC \u05E8\u05D1\u05E2\u05D5\u05DF", "\u05DB\u05DC \u05D7\u05E6\u05D9 \u05E9\u05E0\u05D4", "\u05DB\u05DC \u05E9\u05E0\u05D4"};

    static final class Task {
        long id, nextAt;
        int everyDays = 30;
        boolean active = true;
        String title = "", client = "", phone = "", category = "";

        String every() {
            for (int i = 0; i < DAYS.length; i++) if (DAYS[i] == everyDays) return DAY_LABELS[i];
            return "\u05DB\u05DC " + everyDays + " \u05D9\u05DE\u05D9\u05DD";
        }
    }

    private Recurring() { }

    static List<Task> all(Context c) {
        List<Task> out = new ArrayList<>();
        try (Cursor cur = TicketDb.get(c).getReadableDatabase().query("recurring",
                new String[]{"_id", "title", "client", "phone", "category", "every_days", "next_at", "active"},
                null, null, null, null, "next_at")) {
            while (cur.moveToNext()) {
                Task t = new Task();
                t.id = cur.getLong(0);
                t.title = s(cur, 1);
                t.client = s(cur, 2);
                t.phone = s(cur, 3);
                t.category = s(cur, 4);
                t.everyDays = Math.max(1, cur.getInt(5));
                t.nextAt = cur.getLong(6);
                t.active = cur.getInt(7) != 0;
                out.add(t);
            }
        }
        return out;
    }

    private static String s(Cursor c, int i) {
        String v = c.getString(i);
        return v == null ? "" : v;
    }

    static void save(Context c, Task t) {
        ContentValues v = new ContentValues();
        v.put("title", t.title);
        v.put("client", t.client);
        v.put("phone", t.phone);
        v.put("category", t.category);
        v.put("every_days", t.everyDays);
        v.put("next_at", t.nextAt);
        v.put("active", t.active ? 1 : 0);
        if (t.id == 0) t.id = TicketDb.get(c).getWritableDatabase().insert("recurring", null, v);
        else TicketDb.get(c).getWritableDatabase().update("recurring", v, "_id=?", new String[]{String.valueOf(t.id)});
    }

    static void delete(Context c, long id) {
        TicketDb.get(c).getWritableDatabase().delete("recurring", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Opens tickets for every task that is due; called daily and when the app opens. */
    static synchronized void runDue(Context c) {
        long now = System.currentTimeMillis();
        List<String> opened = new ArrayList<>();
        for (Task r : all(c)) {
            if (!r.active || r.nextAt == 0 || r.nextAt > now) continue;
            TicketDb.Ticket t = new TicketDb.Ticket();
            t.clientName = r.client;
            t.clientPhone = r.phone;
            t.title = "\uD83D\uDD01 " + r.title;
            t.description = "\u05EA\u05D7\u05D6\u05D5\u05E7\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA (" + r.every() + ")";
            if (!r.category.isEmpty()) t.category = r.category;
            t.followupAt = r.nextAt;
            TicketDb.get(c).save(t);
            TicketDb.get(c).addNote(t.id, "\u05E0\u05E4\u05EA\u05D7\u05D4 \u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9\u05EA \u05DE\u05EA\u05D7\u05D6\u05D5\u05E7\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA");
            try { Followups.schedule(c, t); } catch (Exception ignored) { }
            while (r.nextAt <= now) r.nextAt += r.everyDays * AlarmManager.INTERVAL_DAY;
            save(c, r);
            opened.add("#" + t.id + " " + r.title + (r.client.isEmpty() ? "" : " \u2013 " + r.client));
        }
        if (opened.isEmpty()) return;
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("recurring", "\u05EA\u05D7\u05D6\u05D5\u05E7\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA", NotificationManager.IMPORTANCE_DEFAULT));
            Intent open = new Intent(c, TicketsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(c, 9986, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            nm.notify(9985, new Notification.Builder(c, "recurring")
                    .setSmallIcon(android.R.drawable.ic_popup_sync)
                    .setContentTitle("\uD83D\uDD01 \u05E0\u05E4\u05EA\u05D7\u05D5 " + opened.size() + " \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05EA\u05D7\u05D6\u05D5\u05E7\u05D4")
                    .setStyle(new Notification.BigTextStyle().bigText(String.join("\n", opened)))
                    .setContentText(opened.get(0))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build());
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ UI

    static void manage(final Activity a) {
        final List<Task> list = all(a);
        String[] items = new String[list.size() + 1];
        for (int i = 0; i < list.size(); i++) {
            Task t = list.get(i);
            items[i] = (t.active ? "\uD83D\uDD01 " : "\u23F8\uFE0F ") + t.title + (t.client.isEmpty() ? "" : " \u2013 " + t.client)
                    + "\n" + t.every() + " \u2022 \u05D4\u05D1\u05D0: " + (t.nextAt == 0 ? "-" : TicketDb.fmt(t.nextAt).substring(0, 10));
        }
        items[list.size()] = "\u2795 \u05DE\u05E9\u05D9\u05DE\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA \u05D7\u05D3\u05E9\u05D4";
        new AlertDialog.Builder(a)
                .setTitle("\u05EA\u05D7\u05D6\u05D5\u05E7\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA")
                .setItems(items, (d, w) -> edit(a, w < list.size() ? list.get(w) : new Task()))
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                .show();
    }

    private static void edit(final Activity a, final Task t) {
        LinearLayout lay = new LinearLayout(a);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        lay.addView(Ui.text(a, "\u05DE\u05D4 \u05DC\u05E2\u05E9\u05D5\u05EA", 12, Ui.MUTED, false));
        final EditText title = new EditText(a);
        title.setHint("\u05DC\u05DE\u05E9\u05DC: \u05D1\u05D3\u05D9\u05E7\u05EA \u05D2\u05D9\u05D1\u05D5\u05D9 \u05E9\u05E8\u05EA");
        title.setText(t.title);
        lay.addView(title);
        lay.addView(Ui.text(a, "\u05DC\u05E7\u05D5\u05D7 (\u05DC\u05D0 \u05D7\u05D5\u05D1\u05D4)", 12, Ui.MUTED, false));
        final EditText client = new EditText(a);
        client.setText(t.client);
        lay.addView(client);
        final EditText phone = new EditText(a);
        phone.setHint("\u05D8\u05DC\u05E4\u05D5\u05DF (\u05DC\u05D0 \u05D7\u05D5\u05D1\u05D4)");
        phone.setInputType(InputType.TYPE_CLASS_PHONE);
        phone.setText(t.phone);
        lay.addView(phone);
        final int[] every = {t.everyDays};
        final TextView ev = Ui.text(a, t.every() + " \u25BE", 16, Ui.PRIMARY, true);
        ev.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        ev.setOnClickListener(v -> new AlertDialog.Builder(a).setItems(DAY_LABELS, (d, w) -> {
            every[0] = DAYS[w];
            ev.setText(DAY_LABELS[w] + " \u25BE");
        }).show());
        lay.addView(ev);
        final long[] next = {t.nextAt > 0 ? t.nextAt : System.currentTimeMillis() + AlarmManager.INTERVAL_DAY};
        final TextView nx = Ui.text(a, "\u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D4\u05D1\u05D0\u05D4: " + TicketDb.fmt(next[0]).substring(0, 10) + " \u25BE", 16, Ui.PRIMARY, false);
        nx.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        nx.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(next[0]);
            new DatePickerDialog(a, (dp, y, m, d) -> {
                Calendar c2 = Calendar.getInstance();
                c2.set(y, m, d, 8, 0, 0);
                next[0] = c2.getTimeInMillis();
                nx.setText("\u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D4\u05D1\u05D0\u05D4: " + TicketDb.fmt(next[0]).substring(0, 10) + " \u25BE");
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        lay.addView(nx);
        final String[] cat = {t.category};
        final List<String> cats = Categories.list(a);
        final TextView ct = Ui.text(a, (t.category.isEmpty() ? "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4" : He.category(t.category)) + " \u25BE", 15, Ui.PRIMARY, false);
        ct.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        ct.setOnClickListener(v -> {
            String[] names = new String[cats.size()];
            for (int i = 0; i < cats.size(); i++) names[i] = He.category(cats.get(i));
            new AlertDialog.Builder(a).setItems(names, (d, w) -> {
                cat[0] = cats.get(w);
                ct.setText(names[w] + " \u25BE");
            }).show();
        });
        lay.addView(ct);
        ScrollView sv = new ScrollView(a);
        sv.addView(lay);
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(t.id == 0 ? "\u05DE\u05E9\u05D9\u05DE\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA \u05D7\u05D3\u05E9\u05D4" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05DE\u05E9\u05D9\u05DE\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    t.title = title.getText().toString().trim();
                    if (t.title.isEmpty()) return;
                    t.client = client.getText().toString().trim();
                    t.phone = phone.getText().toString().trim();
                    t.everyDays = every[0];
                    t.nextAt = next[0];
                    t.category = cat[0];
                    save(a, t);
                    manage(a);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (t.id > 0) b.setNeutralButton("\u05E2\u05D5\u05D3\u2026", (d, w) -> new AlertDialog.Builder(a)
                .setItems(new String[]{t.active ? "\u23F8\uFE0F \u05D4\u05E9\u05D4\u05D4" : "\u25B6\uFE0F \u05D4\u05E4\u05E2\u05DC", "\uD83D\uDDD1\uFE0F \u05DE\u05D7\u05E7"}, (d2, w2) -> {
                    if (w2 == 0) {
                        t.active = !t.active;
                        save(a, t);
                        manage(a);
                    } else {
                        Auth.confirm(a, "\u05DE\u05D7\u05D9\u05E7\u05EA " + t.title, () -> { delete(a, t.id); manage(a); });
                    }
                }).show());
        b.show();
    }
}
