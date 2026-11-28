package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
        final int clientRow = r;
        this.row = clientRow;

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(24));

        // Header card
        final String phone1 = s.firstPhone(r);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setBackground(Theme.raisedBg(this));
        head.setPadding(dp(20), dp(16), dp(20), dp(16));
        TextView name = Theme.text(this, s.name(r), 24, Theme.TEXT_PRIMARY, 2);
        name.setTextIsSelectable(true);
        head.addView(name);
        int pc = plotColumn(s);
        String plotVal = pc >= 0 ? row.get(pc).trim() : "";
        String sub = (phone1.isEmpty() ? "" : "\uD83D\uDCDE " + phone1) + (plotVal.isEmpty() ? "" : "    \uD83C\uDFD8\uFE0F \u05D7\u05DC\u05E7\u05D4 " + plotVal);
        if (!sub.isEmpty()) head.addView(Theme.body(this, sub, Theme.TEXT_SECONDARY));
        box.addView(head, Ui.cardParams(this));

        // Action tiles
        String mail = "";
        for (String v : row) {
            if (v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { mail = v.trim(); break; }
        }
        final String email1 = mail;
        int same = pc >= 0 ? samePlot(s, r, pc).size() : 0;
        List<View> acts = new java.util.ArrayList<>();
        acts.add(dim(Ui.tile(this, "\uD83D\uDCDE", "\u05D4\u05EA\u05E7\u05E9\u05E8", "", 0,
                v -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone1))))), phone1.isEmpty()));
        acts.add(dim(Ui.tile(this, "\uD83D\uDCAC", "\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", "", 0,
                v -> open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + whatsAppNumber(phone1))))), phone1.isEmpty()));
        acts.add(dim(Ui.tile(this, "\u2709\uFE0F", "\u05DE\u05D9\u05D9\u05DC", email1.isEmpty() ? "\u05D0\u05D9\u05DF \u05DB\u05EA\u05D5\u05D1\u05EA" : "", Ui.MUTED,
                v -> open(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email1)))), email1.isEmpty()));
        acts.add(Ui.tile(this, "\uD83D\uDCDD", "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4", "", 0, v -> startActivity(new Intent(this, TicketActivity.class)
                .putExtra(EXTRA_ROW, clientRow))));
        acts.add(Ui.tile(this, "\uD83C\uDFD8\uFE0F", "\u05D1\u05D0\u05D5\u05EA\u05D4 \u05D7\u05DC\u05E7\u05D4", pc < 0 ? "" : plotVal.isEmpty() ? "\u05D0\u05D9\u05DF \u05D7\u05DC\u05E7\u05D4" : same + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA",
                Ui.MUTED, v -> showSamePlot(clientRow)));
        final String navAddr = Nav.address(this, s, r);
        final boolean navColSet = Nav.column(this, s) >= 0;
        LinearLayout navTile = Ui.tile(this, "\uD83E\uDDED", "\u05E0\u05D9\u05D5\u05D5\u05D8",
                !navColSet ? "\u05D1\u05D7\u05E8 \u05E2\u05DE\u05D5\u05D3\u05EA \u05DB\u05EA\u05D5\u05D1\u05EA" : navAddr.isEmpty() ? "\u05D0\u05D9\u05DF \u05DB\u05EA\u05D5\u05D1\u05EA" : "\u05DC\u05D7\u05D9\u05E6\u05D4 \u05D0\u05E8\u05D5\u05DB\u05D4: \u05E9\u05D9\u05E0\u05D5\u05D9", Ui.MUTED, v -> {
                    if (!navColSet) Nav.chooseColumn(this, this::recreate);
                    else Nav.choose(this, navAddr);
                });
        navTile.setOnLongClickListener(v -> {
            Nav.chooseColumn(this, this::recreate);
            return true;
        });
        acts.add(navTile);
        acts.add(Ui.tile(this, "\uD83D\uDCB0", "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8", "", 0, v -> startActivity(new Intent(this, QuoteActivity.class)
                .putExtra(EXTRA_ROW, clientRow))));
        acts.add(Ui.tile(this, "\u270F\uFE0F", "\u05E2\u05E8\u05D9\u05DB\u05D4", "", 0, v -> {
            startActivity(new Intent(this, ClientEditActivity.class).putExtra(EXTRA_ROW, clientRow));
            finish();
        }));
        final String qrName = s.name(r), qrPhone = phone1;
        acts.add(Ui.tile(this, "\uD83D\uDD33", "QR \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4", "\u05DE\u05D3\u05D1\u05E7\u05D4 \u05DC\u05DC\u05E7\u05D5\u05D7", Ui.MUTED, v -> {
            String url = Sync.clientLink(qrName, qrPhone);
            if (url == null) { Toast.makeText(this, "\u05D3\u05D5\u05E8\u05E9 \u05D7\u05D9\u05D1\u05D5\u05E8 \u05DC\u05E2\u05E0\u05DF (\u2601\uFE0F \u05E2\u05E0\u05DF \u2013 \u05E6\u05D5\u05D5\u05EA)", Toast.LENGTH_LONG).show(); return; }
            open(new Intent(Intent.ACTION_VIEW, Uri.parse(url + "&qr=1")));
        }));
        Ui.grid(this, box, acts, 3);

        // Tickets
        ticketBox = new LinearLayout(this);
        ticketBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(ticketBox);

        equipBox = new LinearLayout(this);
        equipBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(equipBox);

        // Details
        box.addView(Ui.sectionTitle(this, "\u05E4\u05E8\u05D8\u05D9 \u05D4\u05DC\u05E7\u05D5\u05D7"));
        LinearLayout details = Ui.card(this);
        boolean first = true;
        List<String> hidden = Data.hiddenColumns(this);
        for (int c = 0; c < s.headers.size(); c++) {
            String value = row.get(c);
            if (c == s.nameCol || value.trim().isEmpty() || value.trim().matches("0+([.,]0+)?")
                    || hidden.contains(s.headers.get(c).trim())) continue;
            if (!first) {
                View line = new View(this);
                line.setBackgroundColor(Theme.HAIRLINE);
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
                llp.setMargins(0, dp(8), 0, dp(8));
                details.addView(line, llp);
            }
            first = false;
            details.addView(Ui.text(this, s.headers.get(c), 12, Ui.MUTED, false));
            TextView val = Ui.text(this, value, 17, Ui.TEXT, false);
            val.setTextIsSelectable(true);
            details.addView(val);
            if (c == Nav.column(this, s)) {
                final String addr = Nav.address(this, s, r);
                LinearLayout navLine = new LinearLayout(this);
                Button wz = new Button(this);
                wz.setAllCaps(false);
                wz.setText("\uD83D\uDE97 Waze");
                wz.setOnClickListener(v -> Nav.waze(this, addr));
                Button gm = new Button(this);
                gm.setAllCaps(false);
                gm.setText("\uD83D\uDDFA\uFE0F \u05DE\u05E4\u05D5\u05EA");
                gm.setOnClickListener(v -> Nav.maps(this, addr));
                navLine.addView(wz, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                navLine.addView(gm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                details.addView(navLine);
            }
            if (s.phoneCols.contains(c)) {
                for (final String number : ClientStore.phoneParts(value)) {
                    LinearLayout line = new LinearLayout(this);
                    Button call = new Button(this);
                    call.setAllCaps(false);
                    call.setText("\uD83D\uDCDE " + number);
                    call.setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(number)))));
                    Button wa = new Button(this);
                    wa.setAllCaps(false);
                    wa.setText("\uD83D\uDCAC \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4");
                    wa.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/" + whatsAppNumber(number)))));
                    line.addView(call, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f));
                    line.addView(wa, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
                    details.addView(line);
                }
            }
        }
        box.addView(details, Ui.cardParams(this));

        sentBox = new LinearLayout(this);
        sentBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(sentBox);

        calBox = new LinearLayout(this);
        calBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(calBox);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
    }

    private static View dim(View v, boolean disabled) {
        if (disabled) {
            v.setAlpha(0.4f);
            v.setEnabled(false);
            v.setClickable(false);
        }
        return v;
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

    private LinearLayout ticketBox, calBox, sentBox;
    private int row = -1;
    private LinearLayout equipBox;

    @Override
    protected void onResume() {
        super.onResume();
        refreshTickets();
        refreshSent();
        refreshCalendar();
        if (equipBox != null && row >= 0) {
            ClientStore st = Data.get(this);
            String ph = "";
            for (int pc : st.phoneCols) {
                java.util.List<String> rr = st.rows.get(row);
                String v = pc < rr.size() ? rr.get(pc) : "";
                if (v != null && !v.trim().isEmpty()) { ph = v.trim(); break; }
            }
            Equipment.show(this, equipBox, st.name(row), ph);
        }
    }

    /** This client's calendar events (call log, follow-ups); tap one to delete it. */
    private void refreshCalendar() {
        if (calBox == null || row < 0) return;
        calBox.removeAllViews();
        ClientStore s = Data.get(this);
        if (row >= s.rows.size()) return;
        List<Followups.CalEvent> events = Followups.eventsFor(this, s.name(row));
        if (events.isEmpty()) return;
        calBox.addView(Ui.sectionTitle(this, "\uD83D\uDCC5 \u05D0\u05D9\u05E8\u05D5\u05E2\u05D9\u05DD \u05D1\u05D9\u05D5\u05DE\u05DF (" + events.size() + ") - \u05DC\u05D7\u05E5 \u05DC\u05DE\u05D7\u05D9\u05E7\u05D4"));
        for (final Followups.CalEvent e : events) {
            LinearLayout card = Ui.rowCard(this, e.title, TicketDb.fmt(e.start), "", 0, v -> new AlertDialog.Builder(this)
                    .setTitle(e.title)
                    .setMessage(TicketDb.fmt(e.start) + (e.description.isEmpty() ? "" : "\n\n" + e.description))
                    .setPositiveButton("\u05DE\u05D7\u05E7 \u05DE\u05D4\u05D9\u05D5\u05DE\u05DF", (d, w) -> Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05D0\u05D9\u05E8\u05D5\u05E2 \u05DE\u05D4\u05D9\u05D5\u05DE\u05DF", () -> {
                        if (Followups.deleteEvent(this, e.id)) {
                            Toast.makeText(this, "\u05D4\u05D0\u05D9\u05E8\u05D5\u05E2 \u05E0\u05DE\u05D7\u05E7 \u05DE\u05D4\u05D9\u05D5\u05DE\u05DF", Toast.LENGTH_SHORT).show();
                        }
                        refreshCalendar();
                    }))
                    .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                    .show());
            calBox.addView(card, Ui.cardParams(this));
        }
    }

    /** Emails sent to service providers about this client: view and send again. */
    private void refreshSent() {
        if (sentBox == null || row < 0) return;
        sentBox.removeAllViews();
        ClientStore s = Data.get(this);
        if (row >= s.rows.size()) return;
        List<TicketDb.Sent> list = TicketDb.get(this).sentForClient(s.name(row), s.keysForRow(row));
        if (list.isEmpty()) return;
        sentBox.addView(Ui.sectionTitle(this, "\uD83D\uDCE8 \u05DE\u05D9\u05D9\u05DC\u05D9\u05DD \u05E9\u05E0\u05E9\u05DC\u05D7\u05D5 (" + list.size() + ")"));
        for (final TicketDb.Sent m : list) {
            String sub = TicketDb.fmt(m.time) + "  \u2022  \u05D0\u05DC: " + m.to + (m.cc.isEmpty() ? "" : "\n\u05D4\u05E2\u05EA\u05E7 \u05DC\u05DC\u05E7\u05D5\u05D7: " + m.cc);
            sentBox.addView(Ui.rowCard(this, m.subject, sub, m.cc.isEmpty() ? "" : "\u05D4\u05E2\u05EA\u05E7 \u05DC\u05DC\u05E7\u05D5\u05D7", Ui.GREEN,
                    v -> showSent(m)), Ui.cardParams(this));
        }
    }

    private void showSent(final TicketDb.Sent m) {
        new AlertDialog.Builder(this)
                .setTitle(m.subject)
                .setMessage("\u05E0\u05E9\u05DC\u05D7: " + TicketDb.fmt(m.time) + "\n\u05D0\u05DC: " + m.to
                        + (m.cc.isEmpty() ? "" : "\n\u05D4\u05E2\u05EA\u05E7: " + m.cc)
                        + (m.ticketId > 0 ? "\n\u05E7\u05E8\u05D9\u05D0\u05D4 #" + m.ticketId : "") + "\n\n" + m.body)
                .setPositiveButton("\u05E9\u05DC\u05D7 \u05E9\u05D5\u05D1 / \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4\u2026", (d, w) -> sentActions(m))
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                .show();
    }

    private void sentActions(final TicketDb.Sent m) {
        TicketDb db = TicketDb.get(this);
        TicketDb.Ticket t = m.ticketId > 0 ? db.load(m.ticketId) : null;
        TicketDb.Provider pr = t == null || t.provider.isEmpty() ? null : db.providerByName(t.provider);
        final String provName = t == null ? "" : t.provider;
        final String provPhone = pr == null ? "" : pr.phone.trim();
        final String clientPhone = m.clientPhone.trim();
        final String text = m.subject + "\n\n" + m.body;
        final String[] opts = {
                "\uD83D\uDCE7 \u05E9\u05DC\u05D7 \u05E9\u05D5\u05D1 \u05D1\u05DE\u05D9\u05D9\u05DC",
                "\uD83D\uDCAC \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05E0\u05D5\u05EA\u05DF \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA" + (provName.isEmpty() ? "" : " (" + provName + ")"),
                "\uD83D\uDCAC \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05DC\u05E7\u05D5\u05D7" + (m.clientName.isEmpty() ? "" : " (" + m.clientName + ")"),
                "\uD83D\uDDD1\uFE0F \u05DE\u05D7\u05E7 \u05DE\u05D4\u05E8\u05E9\u05D9\u05DE\u05D4"};
        new AlertDialog.Builder(this)
                .setTitle("\u05DE\u05D4 \u05DC\u05E2\u05E9\u05D5\u05EA \u05E2\u05DD \u05D4\u05DE\u05D9\u05D9\u05DC?")
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        TicketDb.Sent again = new TicketDb.Sent();
                        again.ticketId = m.ticketId;
                        again.clientName = m.clientName;
                        again.clientPhone = m.clientPhone;
                        again.to = m.to;
                        again.cc = m.cc;
                        again.subject = m.subject;
                        again.body = m.body;
                        db.addSent(again);
                        if (m.ticketId > 0) db.addNote(m.ticketId, "\u05DE\u05D9\u05D9\u05DC \u05E0\u05E9\u05DC\u05D7 \u05E9\u05D5\u05D1 \u05D0\u05DC " + m.to
                                + (m.cc.isEmpty() ? "" : " (\u05D4\u05E2\u05EA\u05E7: " + m.cc + ")"));
                        TicketActivity.openEmail(this, m.to, m.cc, m.subject, m.body);
                    } else if (w == 1 || w == 2) {
                        String phone = w == 1 ? provPhone : clientPhone;
                        if (phone.isEmpty()) {
                            Toast.makeText(this, w == 1 ? "\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05E0\u05D5\u05EA\u05DF \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA - \u05D4\u05D5\u05E1\u05E3 \u05D0\u05D5\u05EA\u05D5 \u05D1'\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA'"
                                    : "\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05DC\u05E7\u05D5\u05D7", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (m.ticketId > 0) db.addNote(m.ticketId, "\u05EA\u05D5\u05DB\u05DF \u05D4\u05DE\u05D9\u05D9\u05DC \u05E0\u05E9\u05DC\u05D7 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC"
                                + (w == 1 ? provName : (m.clientName.isEmpty() ? "\u05DC\u05E7\u05D5\u05D7" : m.clientName)));
                        open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"
                                + whatsAppNumber(phone) + "?text=" + Uri.encode(text))));
                    } else {
                        Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05DE\u05D9\u05D9\u05DC \u05DE\u05D4\u05E8\u05E9\u05D9\u05DE\u05D4", () -> {
                            db.deleteSent(m.id);
                            refreshSent();
                        });
                    }
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void refreshTickets() {
        if (ticketBox == null || row < 0) return;
        ticketBox.removeAllViews();
        ClientStore s = Data.get(this);
        if (row >= s.rows.size()) return;
        List<TicketDb.Ticket> list = TicketDb.get(this).forClient(s.name(row), s.keysForRow(row));
        if (list.isEmpty()) return;
        ticketBox.addView(Ui.sectionTitle(this, "\uD83C\uDFAB \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA (" + list.size() + ")"));
        for (final TicketDb.Ticket t : list) {
            String badge;
            int color;
            if (t.isOverdue()) { badge = "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8"; color = Ui.RED; }
            else if (!t.isOpen()) { badge = "\u05E1\u05D2\u05D5\u05E8\u05D4"; color = Ui.MUTED; }
            else { badge = He.status(t.status); color = Ui.PRIMARY; }
            String sub = "#" + t.id + "  \u2022  " + TicketDb.fmt(t.created)
                    + (t.followupAt > 0 && t.isOpen() ? "\n\u05DE\u05E2\u05E7\u05D1: " + TicketDb.fmt(t.followupAt) : "");
            ticketBox.addView(Ui.rowCard(this, t.title, sub, badge, color, v -> startActivity(
                    new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id))), Ui.cardParams(this));
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
