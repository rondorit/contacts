package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Full client card with Call and WhatsApp buttons. */
public class DetailActivity extends Activity {
    static final String EXTRA_ROW = "row";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClientStore s = Data.get(this);
        int r = getIntent().getIntExtra(EXTRA_ROW, -1);
        if (r < 0 || r >= s.rows.size()) {
            finish();
            return;
        }
        List<String> row = s.rows.get(r);
        setTitle(s.name(r));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView name = new TextView(this);
        name.setText(s.name(r));
        name.setTextSize(24);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextIsSelectable(true);
        box.addView(name);

        // Quick actions: call, WhatsApp, email.
        final String phone1 = s.firstPhone(r);
        String mail = "";
        for (String v : row) {
            if (v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { mail = v.trim(); break; }
        }
        final String email1 = mail;
        LinearLayout quick = new LinearLayout(this);
        Button qCall = new Button(this);
        qCall.setAllCaps(false);
        qCall.setText("\u05D4\u05EA\u05E7\u05E9\u05E8");
        qCall.setEnabled(!phone1.isEmpty());
        qCall.setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone1)))));
        Button qWa = new Button(this);
        qWa.setAllCaps(false);
        qWa.setText("\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4");
        qWa.setEnabled(!phone1.isEmpty());
        qWa.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + whatsAppNumber(phone1)))));
        Button qMail = new Button(this);
        qMail.setAllCaps(false);
        qMail.setText("\u05DE\u05D9\u05D9\u05DC");
        qMail.setEnabled(!email1.isEmpty());
        qMail.setOnClickListener(v -> open(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email1))));
        quick.addView(qCall, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        quick.addView(qWa, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        quick.addView(qMail, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(quick);

        // Other clients on the same plot (\u05D7\u05DC\u05E7\u05D4)
        Button plot = new Button(this);
        plot.setAllCaps(false);
        int pc = plotColumn(s);
        String plotVal = pc >= 0 ? row.get(pc).trim() : "";
        int same = pc >= 0 ? samePlot(s, r, pc).size() : 0;
        plot.setText(pc < 0 ? "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E0\u05D5\u05E1\u05E4\u05D9\u05DD \u05D1\u05D0\u05D5\u05EA\u05D4 \u05D7\u05DC\u05E7\u05D4"
                : plotVal.isEmpty() ? "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D1\u05D0\u05D5\u05EA\u05D4 \u05D7\u05DC\u05E7\u05D4 (\u05D0\u05D9\u05DF \u05D7\u05DC\u05E7\u05D4 \u05DC\u05DC\u05E7\u05D5\u05D7)"
                : "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E0\u05D5\u05E1\u05E4\u05D9\u05DD \u05D1\u05D7\u05DC\u05E7\u05D4 " + plotVal + " (" + same + ")");
        final int plotRow = r;
        plot.setOnClickListener(v -> showSamePlot(plotRow));
        box.addView(plot);

        final int clientRow = r;
        Button edit = new Button(this);
        edit.setAllCaps(false);
        edit.setText("\u05E2\u05E8\u05D9\u05DB\u05EA \u05E4\u05E8\u05D8\u05D9 \u05D4\u05DC\u05E7\u05D5\u05D7");
        final int editRow = r;
        edit.setOnClickListener(v -> {
            startActivity(new Intent(this, ClientEditActivity.class).putExtra(EXTRA_ROW, editRow));
            finish();
        });
        box.addView(edit);

        Button newTicket = new Button(this);
        newTicket.setAllCaps(false);
        newTicket.setText("+ \u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA \u05D7\u05D3\u05E9\u05D4");
        newTicket.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)
                .putExtra(EXTRA_ROW, clientRow)));
        box.addView(newTicket);
        ticketBox = new LinearLayout(this);
        ticketBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(ticketBox);
        this.row = clientRow;

        for (int c = 0; c < s.headers.size(); c++) {
            String value = row.get(c);
            if (c == s.nameCol || value.isEmpty()) continue;

            TextView label = new TextView(this);
            label.setText(s.headers.get(c));
            label.setTextSize(13);
            label.setTextColor(0xFF777777);
            label.setPadding(0, dp(12), 0, 0);
            box.addView(label);

            TextView val = new TextView(this);
            val.setText(value);
            val.setTextSize(17);
            val.setTextIsSelectable(true);
            box.addView(val);

            if (s.phoneCols.contains(c)) {
                for (final String number : ClientStore.phoneParts(value)) {
                    LinearLayout line = new LinearLayout(this);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    Button call = new Button(this);
                    call.setAllCaps(false);
                    call.setText("\u05D4\u05EA\u05E7\u05E9\u05E8 " + number);
                    call.setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + Uri.encode(number)))));
                    Button wa = new Button(this);
                    wa.setAllCaps(false);
                    wa.setText("WhatsApp");
                    wa.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/" + whatsAppNumber(number)))));
                    line.addView(call, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
                    line.addView(wa, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    box.addView(line);
                }
            }
        }

        calBox = new LinearLayout(this);
        calBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(calBox);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
    }

    /** Israeli local numbers get the 972 country code; others are used as written. */
    static String whatsAppNumber(String number) {
        String k = ClientStore.key(number);
        String plain = number.replaceAll("[^0-9]", "");
        if (number.trim().startsWith("+") || plain.startsWith("00")) {
            return plain.startsWith("00") ? plain.substring(2) : plain;
        }
        return (k.length() == 8 || k.length() == 9) ? "972" + k : plain;
    }

    private static final String KEY_PLOT_COL = "plot_col";

    /** The plot column: chosen by the user, or a column whose title contains "\u05D7\u05DC\u05E7\u05D4". */
    private int plotColumn(ClientStore s) {
        String saved = CallLogger.prefs(this).getString(KEY_PLOT_COL, "");
        if (!saved.isEmpty() && s.headers.indexOf(saved) >= 0) return s.headers.indexOf(saved);
        for (int c = 0; c < s.headers.size(); c++) {
            if (s.headers.get(c).contains("\u05D7\u05DC\u05E7\u05D4")) return c;
        }
        return -1;
    }

    private List<Integer> samePlot(ClientStore s, int r, int col) {
        List<Integer> out = new java.util.ArrayList<>();
        String val = s.rows.get(r).get(col).trim();
        if (val.isEmpty()) return out;
        for (int i = 0; i < s.rows.size(); i++) {
            if (i != r && s.rows.get(i).get(col).trim().equalsIgnoreCase(val)) out.add(i);
        }
        return out;
    }

    private void showSamePlot(final int r) {
        final ClientStore s = Data.get(this);
        int col = plotColumn(s);
        if (col < 0) {
            choosePlotColumn(s, r);
            return;
        }
        String val = s.rows.get(r).get(col).trim();
        if (val.isEmpty()) {
            Toast.makeText(this, "\u05DC\u05DC\u05E7\u05D5\u05D7 \u05D6\u05D4 \u05D0\u05D9\u05DF \u05E2\u05E8\u05DA \u05D1\u05E2\u05DE\u05D5\u05D3\u05D4 " + s.headers.get(col), Toast.LENGTH_LONG).show();
            return;
        }
        final List<Integer> list = samePlot(s, r, col);
        if (list.isEmpty()) {
            Toast.makeText(this, "\u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E0\u05D5\u05E1\u05E4\u05D9\u05DD \u05D1\u05D7\u05DC\u05E7\u05D4 " + val, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            String ph = s.firstPhone(list.get(i));
            names[i] = s.name(list.get(i)) + (ph.isEmpty() ? "" : "   " + ph);
        }
        new AlertDialog.Builder(this)
                .setTitle("\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D1\u05D7\u05DC\u05E7\u05D4 " + val + " (" + list.size() + ")")
                .setItems(names, (d, w) -> startActivity(new Intent(this, DetailActivity.class)
                        .putExtra(EXTRA_ROW, list.get(w))))
                .setNeutralButton("\u05E9\u05E0\u05D4 \u05E2\u05DE\u05D5\u05D3\u05EA \u05D7\u05DC\u05E7\u05D4", (d, w) -> choosePlotColumn(s, r))
                .setPositiveButton("\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05DB\u05DC \u05D4\u05D7\u05DC\u05E7\u05D4", (d, w) -> {
                    int[] arr = new int[list.size() + 1];
                    arr[0] = r;
                    for (int i = 0; i < list.size(); i++) arr[i + 1] = list.get(i);
                    startActivity(new Intent(this, GroupSendActivity.class)
                            .putExtra(GroupSendActivity.EXTRA_ROWS, arr)
                            .putExtra(GroupSendActivity.EXTRA_TITLE, "\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05D7\u05DC\u05E7\u05D4 " + val));
                })
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                .show();
    }

    private void choosePlotColumn(final ClientStore s, final int r) {
        final String[] headers = s.headers.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("\u05D1\u05D0\u05D9\u05D6\u05D5 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05D1\u05D0\u05E7\u05E1\u05DC \u05E0\u05DE\u05E6\u05D0\u05EA \u05D4\u05D7\u05DC\u05E7\u05D4?")
                .setItems(headers, (d, w) -> {
                    CallLogger.prefs(this).edit().putString(KEY_PLOT_COL, headers[w]).apply();
                    showSamePlot(r);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private LinearLayout ticketBox, calBox;
    private int row = -1;

    @Override
    protected void onResume() {
        super.onResume();
        refreshTickets();
        refreshCalendar();
    }

    /** This client's calendar events (call log, follow-ups); tap one to delete it. */
    private void refreshCalendar() {
        if (calBox == null || row < 0) return;
        calBox.removeAllViews();
        ClientStore s = Data.get(this);
        if (row >= s.rows.size()) return;
        List<Followups.CalEvent> events = Followups.eventsFor(this, s.name(row));
        if (events.isEmpty()) return;
        TextView label = new TextView(this);
        label.setText("\u05D0\u05D9\u05E8\u05D5\u05E2\u05D9\u05DD \u05D1\u05D9\u05D5\u05DE\u05DF (" + events.size() + ") - \u05DC\u05D7\u05E5 \u05E2\u05DC \u05D0\u05D9\u05E8\u05D5\u05E2 \u05DB\u05D3\u05D9 \u05DC\u05DE\u05D7\u05D5\u05E7");
        label.setTextSize(13);
        label.setTextColor(0xFF777777);
        label.setPadding(0, dp(16), 0, dp(4));
        calBox.addView(label);
        for (final Followups.CalEvent e : events) {
            TextView tv = new TextView(this);
            tv.setText(TicketDb.fmt(e.start) + "\n" + e.title);
            tv.setTextSize(15);
            tv.setPadding(0, dp(8), 0, dp(8));
            tv.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle(e.title)
                    .setMessage(TicketDb.fmt(e.start) + (e.description.isEmpty() ? "" : "\n\n" + e.description))
                    .setPositiveButton("\u05DE\u05D7\u05E7 \u05DE\u05D4\u05D9\u05D5\u05DE\u05DF", (d, w) -> {
                        if (Followups.deleteEvent(this, e.id)) {
                            Toast.makeText(this, "\u05D4\u05D0\u05D9\u05E8\u05D5\u05E2 \u05E0\u05DE\u05D7\u05E7 \u05DE\u05D4\u05D9\u05D5\u05DE\u05DF", Toast.LENGTH_SHORT).show();
                        }
                        refreshCalendar();
                    })
                    .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                    .show());
            calBox.addView(tv);
            android.view.View line = new android.view.View(this);
            line.setBackgroundColor(0xFFDDDDDD);
            calBox.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        }
    }

    private void refreshTickets() {
        if (ticketBox == null || row < 0) return;
        ticketBox.removeAllViews();
        ClientStore s = Data.get(this);
        if (row >= s.rows.size()) return;
        List<TicketDb.Ticket> list = TicketDb.get(this).forClient(s.name(row), s.keysForRow(row));
        if (list.isEmpty()) return;
        TextView label = new TextView(this);
        label.setText("\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA (" + list.size() + ")");
        label.setTextSize(13);
        label.setTextColor(0xFF777777);
        label.setPadding(0, dp(8), 0, 0);
        ticketBox.addView(label);
        for (final TicketDb.Ticket t : list) {
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText((t.isOverdue() ? "\u26A0 " : "") + "#" + t.id + "  " + He.status(t.status) + "  \u2014  " + t.title);
            b.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)
                    .putExtra(TicketActivity.EXTRA_TICKET, t.id)));
            ticketBox.addView(b);
        }
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05DC\u05E4\u05E2\u05D5\u05DC\u05D4 \u05D6\u05D5", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
