package com.clientcaller.app;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Ticket report by opening date, provider, category and status; export to Excel or share. */
public class ReportActivity extends Activity {
    private static final int REQ_EXPORT = 1;
    private static final String ALL = "\u05D4\u05DB\u05DC";
    private static final String NO_PROVIDER = "\u05DC\u05DC\u05D0 \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA";
    private static final String OPEN_ALL = "\u05DB\u05DC \u05D4\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA (\u05DC\u05D0 \u05E1\u05D2\u05D5\u05E8\u05D5\u05EA)";

    private long from, to;
    private Button bFrom, bTo;
    private Spinner sProvider, sCategory, sStatus;
    private TextView summary;
    private LinearLayout results;
    private final List<TicketDb.Ticket> found = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = new TextView(this);
        head.setText("\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA");
        head.setTextSize(22);
        head.setTypeface(null, Typeface.BOLD);
        box.addView(head);

        label(box, "\u05EA\u05D0\u05E8\u05D9\u05DA \u05E4\u05EA\u05D9\u05D7\u05D4");
        LinearLayout dRow = new LinearLayout(this);
        bFrom = btn(dRow, "", v -> pickDate(true));
        bTo = btn(dRow, "", v -> pickDate(false));
        box.addView(dRow);
        LinearLayout qRow = new LinearLayout(this);
        btn(qRow, "\u05D4\u05D7\u05D5\u05D3\u05E9", v -> range(0));
        btn(qRow, "\u05D7\u05D5\u05D3\u05E9 \u05E7\u05D5\u05D3\u05DD", v -> range(1));
        btn(qRow, "\u05D4\u05E9\u05E0\u05D4", v -> range(2));
        btn(qRow, "\u05D4\u05DB\u05DC", v -> range(3));
        box.addView(qRow);
        range(0);

        TicketDb db = TicketDb.get(this);
        List<String> providers = new ArrayList<>();
        providers.add(ALL);
        providers.add(NO_PROVIDER);
        for (TicketDb.Provider pr : db.providers()) providers.add(pr.name);
        List<String> cats = new ArrayList<>();
        cats.add(ALL);
        cats.addAll(Categories.list(this));
        List<String> statuses = new ArrayList<>();
        statuses.add(ALL);
        statuses.add(OPEN_ALL);
        for (String st : He.STATUS) statuses.add(st);
        for (TicketDb.Ticket t : db.all()) {   // values used in old tickets
            if (!t.provider.isEmpty() && !providers.contains(t.provider)) providers.add(t.provider);
            String c = He.category(t.category);
            if (!c.isEmpty() && !cats.contains(c)) cats.add(c);
        }
        label(box, "\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA");
        sProvider = spinner(box, providers);
        label(box, "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4");
        sCategory = spinner(box, cats);
        label(box, "\u05E1\u05D8\u05D8\u05D5\u05E1");
        sStatus = spinner(box, statuses);

        Button run = new Button(this);
        run.setAllCaps(false);
        run.setTextSize(18);
        run.setText("\u05D4\u05E4\u05E7 \u05D3\u05D5\u05D7");
        run.setOnClickListener(v -> runReport());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        box.addView(run, lp);

        LinearLayout eRow = new LinearLayout(this);
        btn(eRow, "\u05D9\u05D9\u05E6\u05D5\u05D0 \u05DC\u05D0\u05E7\u05E1\u05DC", v -> export());
        btn(eRow, "\u05E9\u05D9\u05EA\u05D5\u05E3 (\u05DE\u05D9\u05D9\u05DC / \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4)", v -> share());
        box.addView(eRow);

        summary = new TextView(this);
        summary.setTextSize(15);
        summary.setPadding(0, dp(12), 0, dp(8));
        summary.setTextIsSelectable(true);
        box.addView(summary);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        box.addView(results);

        ScrollView sv = new ScrollView(this);
        sv.addView(Ui.wrapCard(this, box));
        setContentView(sv);
        He.rtl(this);
        runReport();
    }

    // ------------------------------------------------------------------ dates

    private void range(int kind) {
        Calendar c = Calendar.getInstance();
        if (kind == 3) {
            from = 0;
            to = 0;
        } else {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.DAY_OF_MONTH, 1);
            if (kind == 1) start.add(Calendar.MONTH, -1);
            if (kind == 2) start.set(Calendar.MONTH, Calendar.JANUARY);
            from = startOfDay(start);
            if (kind == 1) {
                Calendar end = (Calendar) start.clone();
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
                to = startOfDay(end);
            } else {
                to = startOfDay(c);
            }
        }
        showDates();
        runReport();
    }

    private static long startOfDay(Calendar c) {
        Calendar d = (Calendar) c.clone();
        d.set(Calendar.HOUR_OF_DAY, 0);
        d.set(Calendar.MINUTE, 0);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis();
    }

    private void showDates() {
        bFrom.setText("\u05DE\u05EA\u05D0\u05E8\u05D9\u05DA: " + (from == 0 ? "\u05D4\u05EA\u05D7\u05DC\u05D4" : day(from)));
        bTo.setText("\u05E2\u05D3 \u05EA\u05D0\u05E8\u05D9\u05DA: " + (to == 0 ? "\u05D4\u05D9\u05D5\u05DD" : day(to)));
    }

    private static String day(long t) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date(t));
    }

    private void pickDate(final boolean isFrom) {
        Calendar c = Calendar.getInstance();
        long cur = isFrom ? from : to;
        if (cur > 0) c.setTimeInMillis(cur);
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar r = Calendar.getInstance();
            r.set(y, m, d);
            if (isFrom) from = startOfDay(r); else to = startOfDay(r);
            showDates();
            runReport();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ------------------------------------------------------------------ report

    private void runReport() {
        if (summary == null) return;
        String prov = String.valueOf(sProvider.getSelectedItem());
        String cat = String.valueOf(sCategory.getSelectedItem());
        String st = String.valueOf(sStatus.getSelectedItem());
        long end = to == 0 ? Long.MAX_VALUE : to + 24L * 3600 * 1000;
        found.clear();
        for (TicketDb.Ticket t : TicketDb.get(this).all()) {
            if (t.created < from || t.created >= end) continue;
            if (prov.equals(NO_PROVIDER) ? !t.provider.isEmpty() : !prov.equals(ALL) && !prov.equals(t.provider)) continue;
            if (!cat.equals(ALL) && !cat.equals(He.category(t.category))) continue;
            if (st.equals(OPEN_ALL) ? !t.isOpen() : !st.equals(ALL) && !st.equals(He.status(t.status))) continue;
            found.add(t);
        }
        java.util.Collections.sort(found, (a, b) -> Long.compare(b.created, a.created));

        Map<String, Integer> byStatus = new LinkedHashMap<>();
        Map<String, Integer> byCat = new LinkedHashMap<>();
        Map<String, Integer> byProv = new LinkedHashMap<>();
        int overdue = 0;
        for (TicketDb.Ticket t : found) {
            inc(byStatus, He.status(t.status));
            inc(byCat, He.category(t.category));
            inc(byProv, t.provider.isEmpty() ? NO_PROVIDER : t.provider);
            if (t.isOverdue()) overdue++;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(header()).append('\n');
        sb.append("\u05E1\u05D4\"\u05DB \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA: ").append(found.size());
        if (overdue > 0) sb.append("   |   \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8: ").append(overdue);
        sb.append("\n\n\u05DC\u05E4\u05D9 \u05E1\u05D8\u05D8\u05D5\u05E1:").append(lines(byStatus));
        sb.append("\n\n\u05DC\u05E4\u05D9 \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4:").append(lines(byCat));
        sb.append("\n\n\u05DC\u05E4\u05D9 \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA:").append(lines(byProv));
        summary.setText(sb.toString());

        results.removeAllViews();
        for (final TicketDb.Ticket t : found) {
            TextView tv = Theme.body(this, "#" + t.id + "  " + day(t.created) + "  " + He.status(t.status)
                    + "\n" + (t.clientName.isEmpty() ? "(\u05DC\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7)" : t.clientName) + " \u2014 " + t.title
                    + "\n" + He.category(t.category) + (t.provider.isEmpty() ? "" : "  |  " + t.provider), Theme.TEXT_PRIMARY);
            tv.setPadding(dp(12), dp(8), dp(12), dp(8));
            tv.setBackground(Theme.raisedBg(this));
            tv.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id)));
            results.addView(tv);
            View gap = new View(this);
            results.addView(gap, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        }
    }

    private String header() {
        return "\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA " + (from == 0 ? "\u05DE\u05D4\u05D4\u05EA\u05D7\u05DC\u05D4" : day(from)) + " - " + (to == 0 ? "\u05D4\u05D9\u05D5\u05DD" : day(to))
                + "\n\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA: " + sProvider.getSelectedItem() + " | \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4: " + sCategory.getSelectedItem()
                + " | \u05E1\u05D8\u05D8\u05D5\u05E1: " + sStatus.getSelectedItem();
    }

    private static void inc(Map<String, Integer> m, String k) {
        m.put(k, m.containsKey(k) ? m.get(k) + 1 : 1);
    }

    private static String lines(Map<String, Integer> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : m.entrySet()) sb.append("\n  ").append(e.getKey()).append(": ").append(e.getValue());
        return sb.length() == 0 ? "\n  -" : sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        runReport();
    }

    // ------------------------------------------------------------------ export / share

    private void export() {
        if (found.isEmpty()) {
            Toast.makeText(this, "\u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05D1\u05D3\u05D5\u05D7", Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE, "tickets-report-" + date + ".xlsx");
        startActivityForResult(i, REQ_EXPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_EXPORT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        final ClientStore table = new ClientStore();
        String[] cols = {"\u05DE\u05E1\u05E4\u05E8 \u05E7\u05E8\u05D9\u05D0\u05D4", "\u05EA\u05D0\u05E8\u05D9\u05DA \u05E4\u05EA\u05D9\u05D7\u05D4", "\u05DC\u05E7\u05D5\u05D7", "\u05D8\u05DC\u05E4\u05D5\u05DF", "\u05E0\u05D5\u05E9\u05D0", "\u05EA\u05D9\u05D0\u05D5\u05E8", "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4", "\u05D3\u05D7\u05D9\u05E4\u05D5\u05EA",
                "\u05E1\u05D8\u05D8\u05D5\u05E1", "\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA", "\u05E4\u05E8\u05D8\u05D9 \u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5", "\u05E6\u05D9\u05D5\u05D3", "MAC", "\u05DE\u05E2\u05E7\u05D1", "\u05E2\u05D3\u05DB\u05D5\u05DF \u05D0\u05D7\u05E8\u05D5\u05DF"};
        for (String c : cols) table.headers.add(c);
        for (TicketDb.Ticket t : found) {
            List<String> r = new ArrayList<>();
            r.add(String.valueOf(t.id));
            r.add(TicketDb.fmt(t.created));
            r.add(t.clientName);
            r.add(t.clientPhone);
            r.add(t.title);
            r.add(t.description);
            r.add(He.category(t.category));
            r.add(He.priority(t.priority));
            r.add(He.status(t.status));
            r.add(t.provider);
            r.add(t.outsource);
            r.add(t.equipment);
            r.add(t.mac);
            r.add(t.followupAt > 0 ? TicketDb.fmt(t.followupAt) : "");
            r.add(TicketDb.fmt(t.updated));
            table.rows.add(r);
        }
        new Thread(() -> {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new java.io.IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05DB\u05EA\u05D5\u05D1 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                table.writeXlsx(out, "\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA");
                runOnUiThread(() -> Toast.makeText(this, "\u05D4\u05D3\u05D5\u05D7 \u05E0\u05E9\u05DE\u05E8", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                runOnUiThread(() -> Toast.makeText(this, "\u05D4\u05D9\u05D9\u05E6\u05D5\u05D0 \u05E0\u05DB\u05E9\u05DC: " + msg, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void share() {
        StringBuilder sb = new StringBuilder(summary.getText());
        if (!found.isEmpty()) {
            sb.append("\n\n\u05E4\u05D9\u05E8\u05D5\u05D8:");
            for (TicketDb.Ticket t : found) {
                sb.append("\n#").append(t.id).append(" ").append(day(t.created)).append(" | ")
                        .append(t.clientName).append(" | ").append(t.title).append(" | ")
                        .append(He.status(t.status));
                if (!t.provider.isEmpty()) sb.append(" | ").append(t.provider);
            }
        }
        Intent i = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA")
                .putExtra(Intent.EXTRA_TEXT, sb.toString());
        try {
            startActivity(Intent.createChooser(i, "\u05E9\u05D9\u05EA\u05D5\u05E3 \u05D4\u05D3\u05D5\u05D7"));
        } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------ UI helpers

    private Button btn(LinearLayout row, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setOnClickListener(l);
        row.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return b;
    }

    private Spinner spinner(LinearLayout box, List<String> items) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                runReport();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        box.addView(s);
        return s;
    }

    private void label(LinearLayout box, String text) {
        TextView l = Theme.label(this, text, Theme.TEXT_SECONDARY);
        l.setPadding(0, dp(12), 0, 0);
        box.addView(l);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
