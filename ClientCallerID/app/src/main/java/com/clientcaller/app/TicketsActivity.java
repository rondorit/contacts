package com.clientcaller.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** List of service tickets: Open / Overdue / Closed / All. */
public class TicketsActivity extends Activity {
    static final String EXTRA_FILTER = "filter";
    static final int OPEN = 0, OVERDUE = 1, CLOSED = 2, ALL = 3;
    private static final String[] NAMES = {"\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8", "\u05E1\u05D2\u05D5\u05E8\u05D5\u05EA", "\u05D4\u05DB\u05DC"};

    private int filter = OPEN;
    private final Button[] tabs = new Button[4];
    private EditText search;
    private TextView empty;
    private ArrayAdapter<String> adapter;
    private final List<Long> shown = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filter = getIntent().getIntExtra(EXTRA_FILTER, OPEN);
        setTitle("\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);

        LinearLayout tabRow = new LinearLayout(this);
        for (int i = 0; i < 4; i++) {
            final int f = i;
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                filter = f;
                refresh();
            });
            tabs[i] = b;
            tabRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(tabRow);

        Button add = new Button(this);
        add.setAllCaps(false);
        add.setText("+ \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4");
        add.setOnClickListener(v -> startActivity(new Intent(this, TicketActivity.class)));
        LinearLayout addRow = new LinearLayout(this);
        addRow.addView(add, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button prov = new Button(this);
        prov.setAllCaps(false);
        prov.setText("\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA");
        prov.setOnClickListener(v -> startActivity(new Intent(this, ProvidersActivity.class)));
        addRow.addView(prov, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(addRow);

        search = new EditText(this);
        search.setHint("\u05D7\u05D9\u05E4\u05D5\u05E9 \u05DC\u05E7\u05D5\u05D7, \u05E0\u05D5\u05E9\u05D0, \u05E6\u05D9\u05D5\u05D3\u2026");
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refresh(); }
        });
        root.addView(search);

        empty = new TextView(this);
        empty.setPadding(0, dp(12), 0, 0);
        root.addView(empty);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> startActivity(
                new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, shown.get(pos))));
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        He.rtl(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<TicketDb.Ticket> all = TicketDb.get(this).all();
        int[] counts = new int[4];
        for (TicketDb.Ticket t : all) {
            if (t.isOpen()) counts[OPEN]++; else counts[CLOSED]++;
            if (t.isOverdue()) counts[OVERDUE]++;
        }
        counts[ALL] = all.size();
        for (int i = 0; i < 4; i++) {
            tabs[i].setText(NAMES[i] + " (" + counts[i] + ")");
            tabs[i].setTypeface(null, i == filter ? Typeface.BOLD : Typeface.NORMAL);
            tabs[i].setTextColor(i == filter ? 0xFF1E88E5 : (i == OVERDUE && counts[i] > 0 ? 0xFFD32F2F : 0xFF444444));
        }

        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<TicketDb.Ticket> sel = new ArrayList<>();
        for (TicketDb.Ticket t : all) {
            boolean ok = filter == ALL
                    || (filter == OPEN && t.isOpen())
                    || (filter == OVERDUE && t.isOverdue())
                    || (filter == CLOSED && !t.isOpen());
            if (!ok) continue;
            if (!q.isEmpty()) {
                String hay = (t.clientName + " " + t.clientPhone + " " + t.title + " " + t.description + " "
                        + t.equipment + " " + t.outsource + " " + t.provider + " #" + t.id).toLowerCase(Locale.ROOT);
                if (!hay.contains(q)) continue;
            }
            sel.add(t);
        }
        // Open lists: soonest follow-up first (no follow-up last); others: newest first.
        if (filter == OPEN || filter == OVERDUE) {
            Collections.sort(sel, (a, b) -> {
                long fa = a.followupAt == 0 ? Long.MAX_VALUE : a.followupAt;
                long fb = b.followupAt == 0 ? Long.MAX_VALUE : b.followupAt;
                if (fa != fb) return fa < fb ? -1 : 1;
                return Long.compare(b.updated, a.updated);
            });
        }

        shown.clear();
        List<String> items = new ArrayList<>();
        for (TicketDb.Ticket t : sel) {
            shown.add(t.id);
            StringBuilder sb = new StringBuilder();
            if (t.isOverdue()) sb.append("\u26A0 \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8  ");
            if ("Urgent".equals(t.priority)) sb.append("\uD83D\uDD34 \u05D3\u05D7\u05D5\u05E3  ");
            sb.append('#').append(t.id).append("  ").append(He.status(t.status)).append('\n');
            sb.append(t.clientName.isEmpty() ? "(\u05DC\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7)" : t.clientName).append(" \u2014 ").append(t.title);
            if (t.followupAt > 0 && t.isOpen()) sb.append("\n\u05DE\u05E2\u05E7\u05D1: ").append(TicketDb.fmt(t.followupAt));
            if (!t.provider.isEmpty() || !t.outsource.isEmpty())
                sb.append("\n\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5: ").append(t.provider).append(t.provider.isEmpty() || t.outsource.isEmpty() ? "" : " - ").append(t.outsource);
            items.add(sb.toString());
        }
        adapter.clear();
        adapter.addAll(items);
        empty.setText(items.isEmpty() ? "\u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05DB\u05D0\u05DF." : "");
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
