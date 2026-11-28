package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

/** Task list with filters; tap to edit, tick to complete. */
public class TasksActivity extends Activity {
    private static final String[] FILTERS = {"\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", "\u05D4\u05D9\u05D5\u05DD", "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8", "\u05D1\u05D5\u05E6\u05E2\u05D5", "\u05D4\u05DB\u05DC"};
    private int filter = 0;
    private LinearLayout list;
    private final Button[] tabs = new Button[FILTERS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05DE\u05E9\u05D9\u05DE\u05D5\u05EA");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 12);
        root.setPadding(p, p, p, 0);
        root.addView(Theme.header(this, "\u2705 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA"));

        Button add = new Button(this);
        add.setAllCaps(false);
        add.setTextSize(18);
        add.setText("+ \u05DE\u05E9\u05D9\u05DE\u05D4 \u05D7\u05D3\u05E9\u05D4");
        add.setOnClickListener(v -> edit(new Tasks.Task()));
        root.addView(add);

        android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout tabRow = new LinearLayout(this);
        for (int i = 0; i < FILTERS.length; i++) {
            final int f = i;
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setOnClickListener(v -> { filter = f; refresh(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 6), Ui.dp(this, 3), Ui.dp(this, 6));
            tabRow.addView(b, lp);
            tabs[i] = b;
        }
        hs.addView(tabRow);
        root.addView(hs);

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, 0, 0, Ui.dp(this, 24));
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        He.rtl(this);

        long id = getIntent().getLongExtra(Tasks.EXTRA_ID, 0);
        if (id > 0) {
            Tasks.Task t = Tasks.load(this, id);
            if (t != null) edit(t);
        } else if (getIntent().getBooleanExtra("new", false)) {
            Tasks.Task t = new Tasks.Task();
            t.client = str(getIntent().getStringExtra(TicketActivity.EXTRA_NAME));
            t.phone = str(getIntent().getStringExtra(TicketActivity.EXTRA_PHONE));
            t.ticketId = getIntent().getLongExtra(TicketActivity.EXTRA_TICKET, 0);
            t.title = str(getIntent().getStringExtra("title"));
            edit(t);
        }
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        TasksWidget.updateAll(this);
    }

    private boolean matches(Tasks.Task t, int f) {
        switch (f) {
            case 0: return !t.isDone();
            case 1: return !t.isDone() && t.isToday();
            case 2: return t.isOverdue();
            case 3: return t.isDone();
            default: return true;
        }
    }

    private void refresh() {
        List<Tasks.Task> all = Tasks.all(this);
        for (int i = 0; i < FILTERS.length; i++) {
            int n = 0;
            for (Tasks.Task t : all) if (matches(t, i)) n++;
            tabs[i].setText(FILTERS[i] + " (" + n + ")");
            boolean sel = i == filter;
            tabs[i].setTextColor(sel ? 0xFF0D1117 : (i == 2 && n > 0 ? Ui.RED : Ui.TEXT));
            tabs[i].setBackground(Ui.rounded(sel ? Ui.PRIMARY : 0x22FFFFFF, 20, this));
        }
        list.removeAllViews();
        int shown = 0;
        for (final Tasks.Task t : all) {
            if (!matches(t, filter)) continue;
            shown++;
            list.addView(row(t));
        }
        if (shown == 0) {
            TextView e = Ui.text(this, filter == 3 ? "\u05E2\u05D5\u05D3 \u05DC\u05D0 \u05D4\u05D5\u05E9\u05DC\u05DE\u05D5 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA" : "\u05D0\u05D9\u05DF \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA \u05DB\u05D0\u05DF \uD83C\uDF89", 16, Ui.MUTED, false);
            e.setPadding(0, Ui.dp(this, 24), 0, 0);
            e.setGravity(android.view.Gravity.CENTER);
            list.addView(e);
        }
    }

    private View row(final Tasks.Task t) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(android.view.Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.rounded(Theme.SURFACE, 14, this));
        int p = Ui.dp(this, 10);
        card.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        card.setLayoutParams(lp);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(t.isDone());
        cb.setOnClickListener(v -> {
            Tasks.setDone(this, t.id, cb.isChecked());
            refresh();
        });
        card.addView(cb);

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, ("High".equals(t.priority) ? "\uD83D\uDD34 " : "Low".equals(t.priority) ? "\u26AA " : "") + t.title, 16, Ui.TEXT, true);
        if (t.isDone()) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            title.setTextColor(Ui.MUTED);
        }
        txt.addView(title);
        StringBuilder sub = new StringBuilder();
        if (t.dueAt > 0) sub.append(t.isOverdue() ? "\u23F0 \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8 \u2022 " : "\uD83D\uDCC5 ").append(TicketDb.fmt(t.dueAt));
        if (!t.client.isEmpty()) sub.append(sub.length() > 0 ? "  \u2022  " : "").append("\uD83D\uDC64 ").append(t.client);
        if (!t.assignee.isEmpty()) sub.append(sub.length() > 0 ? "  \u2022  " : "").append("\uD83D\uDC77 ").append(t.assignee);
        if (t.hasPlace()) sub.append(sub.length() > 0 ? "  \u2022  " : "").append("\uD83D\uDCCD ").append(t.place);
        if (t.ticketId > 0) sub.append(sub.length() > 0 ? "  \u2022  " : "").append("\uD83C\uDFAB #").append(t.ticketId);
        if (sub.length() > 0) txt.addView(Ui.text(this, sub.toString(), 13, t.isOverdue() ? Ui.RED : Ui.MUTED, false));
        if (!t.notes.isEmpty()) txt.addView(Ui.text(this, t.notes, 13, Ui.MUTED, false));
        card.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.setOnClickListener(v -> edit(t));
        return card;
    }

    // ------------------------------------------------------------------ editor

    private void edit(final Tasks.Task t) {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final EditText title = new EditText(this);
        title.setHint("\u05DE\u05D4 \u05E6\u05E8\u05D9\u05DA \u05DC\u05E2\u05E9\u05D5\u05EA?");
        title.setText(t.title);
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        lay.addView(title);
        final EditText notes = new EditText(this);
        notes.setHint("\u05E4\u05E8\u05D8\u05D9\u05DD (\u05DC\u05D0 \u05D7\u05D5\u05D1\u05D4)");
        notes.setText(t.notes);
        notes.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        lay.addView(notes);

        final long[] due = {t.dueAt};
        final TextView dueTv = link(lay, due[0] > 0 ? "\uD83D\uDCC5 " + TicketDb.fmt(due[0]) + " \u25BE" : "\uD83D\uDCC5 \u05EA\u05D0\u05E8\u05D9\u05DA \u05D5\u05E9\u05E2\u05D4 \u25BE");
        dueTv.setOnClickListener(v -> pickDue(due, dueTv));
        final CheckBox remind = new CheckBox(this);
        remind.setText("\uD83D\uDD14 \u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05D1\u05D6\u05DE\u05DF");
        remind.setChecked(t.remind);
        lay.addView(remind);

        final String[] place = {t.place};
        final double[] ll = {t.lat, t.lng};
        final int[] rad = {t.radius};
        final TextView plTv = link(lay, t.hasPlace() ? "\uD83D\uDCCD \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC" + t.place + " \u25BE" : "\uD83D\uDCCD \u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC\u05DE\u05E7\u05D5\u05DD \u25BE");
        plTv.setOnClickListener(v -> Geo.pick(this, client(lay), (name, lat, lng, r) -> {
            place[0] = name;
            ll[0] = lat;
            ll[1] = lng;
            rad[0] = r;
            plTv.setText(name.isEmpty() ? "\uD83D\uDCCD \u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC\u05DE\u05E7\u05D5\u05DD \u25BE" : "\uD83D\uDCCD \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC" + name + " \u25BE");
        }));
        final String[] pr = {t.priority};
        final TextView prTv = link(lay, "\u05E2\u05D3\u05D9\u05E4\u05D5\u05EA: " + Tasks.priorityHe(pr[0]) + " \u25BE");
        prTv.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(Tasks.PRIORITY_HE, (d, w) -> {
            pr[0] = Tasks.PRIORITIES[w];
            prTv.setText("\u05E2\u05D3\u05D9\u05E4\u05D5\u05EA: " + Tasks.PRIORITY_HE[w] + " \u25BE");
        }).show());

        final EditText client = new EditText(this);
        client.setHint("\u05DC\u05E7\u05D5\u05D7 (\u05DC\u05D0 \u05D7\u05D5\u05D1\u05D4)");
        client.setText(t.client);
        lay.addView(client);

        final String[] who = {t.assignee};
        final TextView whoTv = link(lay, who[0].isEmpty() ? "\uD83D\uDC77 \u05E9\u05D9\u05D5\u05DA \u05DC\u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA \u25BE" : "\uD83D\uDC77 " + who[0] + " \u25BE");
        whoTv.setOnClickListener(v -> Team.choose(this, who[0], name -> {
            who[0] = name;
            whoTv.setText(name.isEmpty() ? "\uD83D\uDC77 \u05E9\u05D9\u05D5\u05DA \u05DC\u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA \u25BE" : "\uD83D\uDC77 " + name + " \u25BE");
        }));

        ScrollView sv = new ScrollView(this);
        sv.addView(lay);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(t.id == 0 ? "\u05DE\u05E9\u05D9\u05DE\u05D4 \u05D7\u05D3\u05E9\u05D4" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05DE\u05E9\u05D9\u05DE\u05D4")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    t.title = title.getText().toString().trim();
                    if (t.title.isEmpty()) return;
                    t.notes = notes.getText().toString().trim();
                    t.dueAt = due[0];
                    t.remind = remind.isChecked();
                    t.priority = pr[0];
                    t.client = client.getText().toString().trim();
                    t.place = place[0];
                    t.lat = ll[0];
                    t.lng = ll[1];
                    t.radius = rad[0];
                    boolean newAssignee = !who[0].isEmpty() && !who[0].equals(t.assignee);
                    t.assignee = who[0];
                    Tasks.save(this, t);
                    refresh();
                    if (newAssignee) askSend(t);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (t.id > 0) b.setNeutralButton("\u05E2\u05D5\u05D3\u2026", (d, w) -> more(t));
        b.show();
    }

    /** The client typed in the editor (the EditText after the "client" hint). */
    private String client(LinearLayout lay) {
        for (int i = 0; i < lay.getChildCount(); i++) {
            View v = lay.getChildAt(i);
            if (v instanceof EditText && "\u05DC\u05E7\u05D5\u05D7 (\u05DC\u05D0 \u05D7\u05D5\u05D1\u05D4)".contentEquals(((EditText) v).getHint())) return ((EditText) v).getText().toString().trim();
        }
        return "";
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        super.onRequestPermissionsResult(req, perms, res);
        if (req == Geo.REQ_FINE) {
            android.widget.Toast.makeText(this, Geo.fineGranted(this) ? "\u05D4\u05D4\u05E8\u05E9\u05D0\u05D4 \u05D0\u05D5\u05E9\u05E8\u05D4 \u2013 \u05D1\u05D7\u05E8 \u05E9\u05D5\u05D1 \u05DE\u05E7\u05D5\u05DD" : "\u05D1\u05DC\u05D9 \u05D4\u05E8\u05E9\u05D0\u05EA \u05DE\u05D9\u05E7\u05D5\u05DD \u05D0\u05D9\u05DF \u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05DC\u05E4\u05D9 \u05DE\u05E7\u05D5\u05DD",
                    android.widget.Toast.LENGTH_LONG).show();
        } else if (req == Geo.REQ_BACKGROUND) {
            for (Tasks.Task t : Tasks.all(this)) Geo.sync(this, t);
        }
    }

    private TextView link(LinearLayout lay, String text) {
        TextView tv = Ui.text(this, text, 16, Ui.PRIMARY, false);
        tv.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        lay.addView(tv);
        return tv;
    }

    private void pickDue(final long[] due, final TextView tv) {
        final Calendar c = Calendar.getInstance();
        if (due[0] > 0) c.setTimeInMillis(due[0]);
        else { c.add(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 9); c.set(Calendar.MINUTE, 0); }
        String[] quick = {"\u05D4\u05D9\u05D5\u05DD 17:00", "\u05DE\u05D7\u05E8 09:00", "\u05D1\u05E2\u05D5\u05D3 \u05E9\u05D1\u05D5\u05E2", "\uD83D\uDCC5 \u05D1\u05D7\u05E8 \u05EA\u05D0\u05E8\u05D9\u05DA \u05D5\u05E9\u05E2\u05D4\u2026", "\u2716 \u05DC\u05DC\u05D0 \u05EA\u05D0\u05E8\u05D9\u05DA"};
        new AlertDialog.Builder(this).setItems(quick, (d, w) -> {
            Calendar q = Calendar.getInstance();
            q.set(Calendar.SECOND, 0);
            if (w == 0) { q.set(Calendar.HOUR_OF_DAY, 17); q.set(Calendar.MINUTE, 0); }
            else if (w == 1) { q.add(Calendar.DAY_OF_MONTH, 1); q.set(Calendar.HOUR_OF_DAY, 9); q.set(Calendar.MINUTE, 0); }
            else if (w == 2) { q.add(Calendar.DAY_OF_MONTH, 7); q.set(Calendar.HOUR_OF_DAY, 9); q.set(Calendar.MINUTE, 0); }
            else if (w == 4) { due[0] = 0; tv.setText("\uD83D\uDCC5 \u05EA\u05D0\u05E8\u05D9\u05DA \u05D5\u05E9\u05E2\u05D4 \u25BE"); return; }
            else {
                new DatePickerDialog(this, (dp, y, m, day) -> new TimePickerDialog(this, (tp, h, min) -> {
                    Calendar x = Calendar.getInstance();
                    x.set(y, m, day, h, min, 0);
                    due[0] = x.getTimeInMillis();
                    tv.setText("\uD83D\uDCC5 " + TicketDb.fmt(due[0]) + " \u25BE");
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(),
                        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                return;
            }
            due[0] = q.getTimeInMillis();
            tv.setText("\uD83D\uDCC5 " + TicketDb.fmt(due[0]) + " \u25BE");
        }).show();
    }

    private void more(final Tasks.Task t) {
        String[] opts = {t.isDone() ? "\u21A9 \u05D4\u05D7\u05D6\u05E8 \u05DC\u05E4\u05EA\u05D5\u05D7\u05D4" : "\u2714 \u05E1\u05DE\u05DF \u05DB\u05D1\u05D5\u05E6\u05E2\u05D4", "\uD83C\uDFAB \u05E4\u05EA\u05D7 \u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA \u05DE\u05D4\u05DE\u05E9\u05D9\u05DE\u05D4",
                t.assignee.isEmpty() ? "\uD83D\uDCAC \u05E9\u05DC\u05D7 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : "\uD83D\uDCAC \u05E9\u05DC\u05D7 \u05DC" + t.assignee, "\uD83D\uDDD1\uFE0F \u05DE\u05D7\u05E7"};
        new AlertDialog.Builder(this).setItems(opts, (d, w) -> {
            if (w == 0) {
                Tasks.setDone(this, t.id, !t.isDone());
                refresh();
            } else if (w == 1) {
                if (t.ticketId > 0) {
                    startActivity(new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.ticketId));
                    return;
                }
                TicketDb.Ticket k = new TicketDb.Ticket();
                k.title = t.title;
                k.description = t.notes;
                k.clientName = t.client;
                k.clientPhone = t.phone;
                k.assignee = t.assignee;
                k.followupAt = t.dueAt;
                TicketDb.get(this).save(k);
                TicketDb.get(this).addNote(k.id, "\u05E0\u05E4\u05EA\u05D7\u05D4 \u05DE\u05DE\u05E9\u05D9\u05DE\u05D4");
                t.ticketId = k.id;
                Tasks.save(this, t);
                startActivity(new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, k.id));
            } else if (w == 2) {
                send(t);
            } else {
                Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05D4\u05DE\u05E9\u05D9\u05DE\u05D4", () -> { Tasks.delete(this, t.id); refresh(); });
            }
        }).show();
    }

    private void askSend(final Tasks.Task t) {
        new AlertDialog.Builder(this)
                .setMessage("\u05DC\u05E9\u05DC\u05D5\u05D7 \u05D0\u05EA \u05D4\u05DE\u05E9\u05D9\u05DE\u05D4 \u05DC" + t.assignee + " \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4?")
                .setPositiveButton("\uD83D\uDCAC \u05E9\u05DC\u05D7", (d, w) -> send(t))
                .setNegativeButton("\u05DC\u05D0 \u05E2\u05DB\u05E9\u05D9\u05D5", null)
                .show();
    }

    private void send(Tasks.Task t) {
        StringBuilder sb = new StringBuilder("\u2705 \u05DE\u05E9\u05D9\u05DE\u05D4: ").append(t.title);
        if (!t.notes.isEmpty()) sb.append('\n').append(t.notes);
        if (!t.client.isEmpty()) sb.append("\n\u05DC\u05E7\u05D5\u05D7: ").append(t.client);
        if (t.dueAt > 0) sb.append("\n\u05E2\u05D3: ").append(TicketDb.fmt(t.dueAt));
        String num = "";
        Team.Member m = t.assignee.isEmpty() ? null : Team.byName(this, t.assignee);
        if (m != null) num = DetailActivity.whatsAppNumber(m.phone);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/" + num + "?text="
                    + android.net.Uri.encode(sb.toString()))));
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05D0 \u05E0\u05E4\u05EA\u05D7", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
