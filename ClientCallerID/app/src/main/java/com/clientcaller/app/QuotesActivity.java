package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** List of price quotes with filters; access to the price list and issuer details. */
public class QuotesActivity extends Activity {
    private static final String[] FILTERS = {"\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", "\u05D0\u05D5\u05E9\u05E8\u05D5", "\u05E0\u05D3\u05D7\u05D5", "\u05D4\u05DB\u05DC"};
    private int filter = 0;
    private LinearLayout page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8");
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 60));
        page.addView(Theme.header(this, "\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8"));
        ScrollView sv = new ScrollView(this);
        sv.addView(page);
        setContentView(sv);
        He.rtl(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        page.removeAllViews();
        List<QuoteDb.Quote> all = QuoteDb.get(this).all();
        int open = 0, approved = 0, rejected = 0;
        double approvedSum = 0, openSum = 0;
        for (QuoteDb.Quote q : all) {
            if (q.isOpen()) { open++; openSum += q.total(); }
            if ("Approved".equals(q.status)) { approved++; approvedSum += q.total(); }
            if ("Rejected".equals(q.status)) rejected++;
        }
        List<View> tools = new ArrayList<>();
        tools.add(Ui.tile(this, "\uD83D\uDCDD", "\u05D4\u05E6\u05E2\u05D4 \u05D7\u05D3\u05E9\u05D4", "", 0, v -> startActivity(new Intent(this, QuoteActivity.class))));
        tools.add(Ui.tile(this, "\uD83C\uDFF7\uFE0F", "\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", QuoteDb.get(this).catalog().size() + " \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD", Ui.MUTED,
                v -> startActivity(new Intent(this, CatalogActivity.class))));
        tools.add(Ui.tile(this, "\uD83E\uDDFE", "\u05E4\u05E8\u05D8\u05D9 \u05D4\u05DE\u05E6\u05D9\u05E2", Issuer.get(this, 0), Ui.MUTED, v -> editIssuer()));
        Ui.grid(this, page, tools, 3);

        List<View> stats = new ArrayList<>();
        stats.add(Ui.stat(this, "\uD83D\uDCC2", String.valueOf(open), "\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA \u2022 " + QuoteDb.money(openSum), Ui.PRIMARY, v -> setFilter(0)));
        stats.add(Ui.stat(this, "\u2705", String.valueOf(approved), "\u05D0\u05D5\u05E9\u05E8\u05D5 \u2022 " + QuoteDb.money(approvedSum), Ui.GREEN, v -> setFilter(1)));
        Ui.grid(this, page, stats, 2);

        LinearLayout tabs = new LinearLayout(this);
        for (int i = 0; i < FILTERS.length; i++) {
            final int f = i;
            int count = i == 0 ? open : i == 1 ? approved : i == 2 ? rejected : all.size();
            TextView t = Theme.text(this, FILTERS[i] + " (" + count + ")", 14,
                    i == filter ? Theme.MINT : Theme.TEXT_SECONDARY, i == filter ? 1 : 0);
            t.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            t.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            t.setBackground(Theme.pillBg(this, i == filter ? Theme.MINT_CONTAINER : Theme.SURFACE_RAISED));
            t.setOnClickListener(v -> setFilter(f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMargins(Ui.dp(this, 3), Ui.dp(this, 10), Ui.dp(this, 3), Ui.dp(this, 6));
            tabs.addView(t, lp);
        }
        page.addView(tabs);

        int shown = 0;
        for (final QuoteDb.Quote q : all) {
            boolean ok = filter == 3 || (filter == 0 && q.isOpen()) || (filter == 1 && "Approved".equals(q.status))
                    || (filter == 2 && "Rejected".equals(q.status));
            if (!ok) continue;
            shown++;
            int color = "Approved".equals(q.status) ? Ui.GREEN : "Rejected".equals(q.status) ? Ui.RED
                    : "Sent".equals(q.status) ? Ui.PRIMARY : Ui.MUTED;
            boolean due = q.isOpen() && q.followupAt > 0 && q.followupAt < System.currentTimeMillis();
            String sub = "\u05DE\u05E1' " + q.id + "  \u2022  " + TicketDb.fmt(q.created).substring(0, 10) + "  \u2022  " + QuoteDb.money(q.total())
                    + (q.title.isEmpty() ? "" : "\n" + q.title) + (due ? "\n\u23F0 \u05DC\u05D1\u05D3\u05D5\u05E7 \u05D0\u05D9\u05E9\u05D5\u05E8" : "");
            page.addView(Ui.rowCard(this, q.clientName.isEmpty() ? "(\u05DC\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7)" : q.clientName, sub,
                    QuoteDb.statusHe(q.status), color, v -> startActivity(new Intent(this, QuoteActivity.class)
                            .putExtra(QuoteActivity.EXTRA_QUOTE, q.id))), Ui.cardParams(this));
        }
        if (shown == 0) page.addView(Ui.text(this, "   \u05D0\u05D9\u05DF \u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8 \u05DB\u05D0\u05DF", 14, Ui.MUTED, false));
    }

    private void setFilter(int f) {
        filter = f;
        refresh();
    }

    private void editIssuer() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final EditText[] fields = new EditText[Issuer.KEYS.length];
        for (int i = 0; i < Issuer.KEYS.length; i++) {
            lay.addView(Ui.text(this, Issuer.LABELS[i], 12, Ui.MUTED, false));
            EditText e = new EditText(this);
            e.setInputType(i == 3 ? InputType.TYPE_CLASS_PHONE : i == 4
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS : InputType.TYPE_CLASS_TEXT);
            e.setText(Issuer.get(this, i));
            lay.addView(e);
            fields[i] = e;
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(lay);
        new AlertDialog.Builder(this)
                .setTitle("\u05E4\u05E8\u05D8\u05D9 \u05D4\u05DE\u05E6\u05D9\u05E2 (\u05DE\u05D5\u05E4\u05D9\u05E2\u05D9\u05DD \u05D1\u05D4\u05E6\u05E2\u05D4)")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    for (int i = 0; i < fields.length; i++) Issuer.set(this, i, fields[i].getText().toString().trim());
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }
}
