package com.clientcaller.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;
    private static final int REQ_ROLE = 2;
    private static final int REQ_PERMS = 3;
    private static final int REQ_BACKUP = 4;
    private static final int REQ_RESTORE = 5;
    private static final int REQ_EXPORT = 6;
    private static final int REQ_ACCESS_SAVE = 7;
    private static final int REQ_ACCESS_SOURCE = 8;

    private static final String[] TAB_ICONS = {"\uD83C\uDFE0", "\uD83D\uDC65", "\uD83C\uDFAB", "\u2699\uFE0F"};
    private static final String[] TAB_NAMES = {"\u05E8\u05D0\u05E9\u05D9", "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", "\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA", "\u05E0\u05D9\u05D4\u05D5\u05DC"};

    private int tab = 0;
    private final LinearLayout[] pages = new LinearLayout[4];
    private final android.widget.ScrollView[] scrolls = new android.widget.ScrollView[4];
    private final LinearLayout[] tabButtons = new LinearLayout[4];
    private TextView headerSub;
    private EditText search;
    private LinearLayout listBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.PRIMARY_DARK);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        android.graphics.drawable.GradientDrawable hg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{Ui.PRIMARY_DARK, Ui.PRIMARY});
        header.setBackground(hg);
        header.setPadding(dp(20), dp(16), dp(20), dp(16));
        header.addView(Ui.text(this, "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05E9\u05D9\u05E8\u05D5\u05EA", 22, 0xFFFFFFFF, true));
        headerSub = Ui.text(this, "", 13, 0xCCFFFFFF, false);
        header.addView(headerSub);
        root.addView(header);

        // Pages
        android.widget.FrameLayout content = new android.widget.FrameLayout(this);
        for (int i = 0; i < 4; i++) {
            pages[i] = new LinearLayout(this);
            pages[i].setOrientation(LinearLayout.VERTICAL);
            pages[i].setPadding(dp(10), dp(6), dp(10), dp(90));
            scrolls[i] = new android.widget.ScrollView(this);
            scrolls[i].setFillViewport(true);
            scrolls[i].addView(pages[i]);
            content.addView(scrolls[i]);
        }
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Clients page: search box stays, list below it
        search = new EditText(this);
        search.setHint("\uD83D\uDD0D  \u05D7\u05D9\u05E4\u05D5\u05E9 \u05DC\u05E4\u05D9 \u05E9\u05DD, \u05D8\u05DC\u05E4\u05D5\u05DF, \u05D7\u05DC\u05E7\u05D4\u2026");
        search.setSingleLine(true);
        search.setBackground(Ui.rounded(0xFFFFFFFF, 12, this));
        search.setPadding(dp(14), dp(12), dp(14), dp(12));
        search.setElevation(dp(1));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refreshList(); }
        });
        LinearLayout.LayoutParams slp = Ui.cardParams(this);
        slp.topMargin = dp(10);
        pages[1].addView(search, slp);
        List<View> clientActions = new ArrayList<>();
        clientActions.add(Ui.tile(this, "\u2795", "\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9", "", 0, v -> startActivity(new Intent(this, ClientEditActivity.class))));
        clientActions.add(Ui.tile(this, "\uD83D\uDCE5", "\u05D9\u05D9\u05D1\u05D5\u05D0", "\u05D0\u05E7\u05E1\u05DC / Access", Ui.MUTED, v -> pickFile()));
        clientActions.add(Ui.tile(this, "\uD83D\uDCE4", "\u05D9\u05D9\u05E6\u05D5\u05D0", "\u05DC\u05D0\u05E7\u05E1\u05DC", Ui.MUTED, v -> startExport()));
        Ui.grid(this, pages[1], clientActions, 3);
        pages[1].addView(Ui.text(this, "  \u05DC\u05D7\u05D9\u05E6\u05D4 \u05E2\u05DC \u05DC\u05E7\u05D5\u05D7 \u05E4\u05D5\u05EA\u05D7\u05EA \u05D0\u05EA \u05D4\u05DB\u05E8\u05D8\u05D9\u05E1, \u05DC\u05D7\u05D9\u05E6\u05D4 \u05D0\u05E8\u05D5\u05DB\u05D4 - \u05E2\u05E8\u05D9\u05DB\u05D4", 12, Ui.MUTED, false));
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        pages[1].addView(listBox);

        // Bottom tabs
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundColor(0xFFFFFFFF);
        tabs.setElevation(dp(8));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout tb = new LinearLayout(this);
            tb.setOrientation(LinearLayout.VERTICAL);
            tb.setGravity(android.view.Gravity.CENTER);
            tb.setPadding(0, dp(6), 0, dp(6));
            TextView ic = Ui.text(this, TAB_ICONS[i], 22, Ui.TEXT, false);
            ic.setGravity(android.view.Gravity.CENTER);
            ic.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            TextView nm = Ui.text(this, TAB_NAMES[i], 12, Ui.MUTED, false);
            nm.setGravity(android.view.Gravity.CENTER);
            nm.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tb.addView(ic);
            tb.addView(nm);
            tb.setOnClickListener(v -> showTab(idx));
            tabButtons[i] = tb;
            tabs.addView(tb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(tabs);

        setContentView(root);
        He.rtl(this);
        installCrashCatcher();
        String lastCrash = CallLogger.prefs(this).getString("last_crash", "");
        if (!lastCrash.isEmpty()) {
            CallLogger.prefs(this).edit().remove("last_crash").commit();
            new AlertDialog.Builder(this).setTitle("\u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05E0\u05E1\u05D2\u05E8\u05D4 \u05D1\u05D2\u05DC\u05DC \u05E9\u05D2\u05D9\u05D0\u05D4")
                    .setMessage("\u05E6\u05DC\u05DD \u05D0\u05EA \u05D4\u05DE\u05E1\u05DA \u05D5\u05E9\u05DC\u05D7 \u05DC\u05E7\u05DC\u05D5\u05D3:\n\n" + lastCrash)
                    .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", null).show();
        }
        Backup.scheduleReminder(this);
        showTab(0);
        if (getIntent().getBooleanExtra(Backup.EXTRA_BACKUP, false)) startBackup();
    }

    private void showTab(int i) {
        tab = i;
        for (int k = 0; k < 4; k++) {
            scrolls[k].setVisibility(k == i ? View.VISIBLE : View.GONE);
            tabButtons[k].setBackground(k == i ? Ui.rounded(0xFFE3EDFB, 12, this) : null);
            ((TextView) tabButtons[k].getChildAt(1)).setTextColor(k == i ? Ui.PRIMARY : Ui.MUTED);
            ((TextView) tabButtons[k].getChildAt(1)).setTypeface(null, k == i ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private boolean setupDone() {
        boolean role = true;
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            role = rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)
                    || rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
        }
        return !Data.get(this).rows.isEmpty() && hasPermissions() && Settings.canDrawOverlays(this) && role;
    }

    private void refresh() {
        ClientStore s = Data.get(this);
        List<TicketDb.Ticket> all = TicketDb.get(this).all();
        int open = 0, late = 0, today = 0;
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        List<TicketDb.Ticket> upcoming = new ArrayList<>();
        for (TicketDb.Ticket t : all) {
            if (!t.isOpen()) continue;
            open++;
            if (t.isOverdue()) late++;
            if (t.followupAt > 0 && t.followupAt <= endOfDay.getTimeInMillis()) today++;
            upcoming.add(t);
        }
        java.util.Collections.sort(upcoming, (a, b) -> {
            long fa = a.followupAt == 0 ? Long.MAX_VALUE : a.followupAt;
            long fb = b.followupAt == 0 ? Long.MAX_VALUE : b.followupAt;
            return Long.compare(fa, fb);
        });
        headerSub.setText(s.rows.size() + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA  \u2022  " + open + " \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA" + (late > 0 ? "  \u2022  " + late + " \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8" : ""));

        buildHome(s, open, late, today, upcoming);
        buildTickets(all, open, late, upcoming);
        buildManage(s);
        refreshList();
    }

    // ------------------------------------------------------------------ Home

    private void buildHome(ClientStore s, int open, int late, int today, List<TicketDb.Ticket> upcoming) {
        LinearLayout p = pages[0];
        p.removeAllViews();
        if (!setupDone()) {
            LinearLayout warn = Ui.rowCard(this, "\u26A0\uFE0F  \u05D4\u05D4\u05D2\u05D3\u05E8\u05D4 \u05DC\u05D0 \u05D4\u05D5\u05E9\u05DC\u05DE\u05D4",
                    "\u05DC\u05D7\u05E5 \u05DB\u05D0\u05DF \u05DB\u05D3\u05D9 \u05DC\u05D4\u05E9\u05DC\u05D9\u05DD \u05D9\u05D9\u05D1\u05D5\u05D0 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA \u05D1\u05DC\u05E9\u05D5\u05E0\u05D9\u05EA \u05E0\u05D9\u05D4\u05D5\u05DC", "", 0, v -> showTab(3));
            warn.setBackground(Ui.rounded(0xFFFFF4E5, 14, this));
            p.addView(warn, Ui.cardParams(this));
        }
        p.addView(Ui.sectionTitle(this, "\u05DE\u05D1\u05D8 \u05DE\u05D4\u05D9\u05E8"));
        List<View> stats = new ArrayList<>();
        stats.add(Ui.stat(this, "\uD83C\uDFAB", String.valueOf(open), "\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", Ui.PRIMARY, v -> openTickets(TicketsActivity.OPEN)));
        stats.add(Ui.stat(this, "\u23F0", String.valueOf(late), "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8", late > 0 ? Ui.RED : Ui.GREEN, v -> openTickets(TicketsActivity.OVERDUE)));
        stats.add(Ui.stat(this, "\uD83D\uDCC5", String.valueOf(today), "\u05DE\u05E2\u05E7\u05D1\u05D9\u05DD \u05DC\u05D4\u05D9\u05D5\u05DD", Ui.ORANGE, v -> openTickets(TicketsActivity.OPEN)));
        stats.add(Ui.stat(this, "\uD83D\uDC65", String.valueOf(s.rows.size()), "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", Ui.PRIMARY_DARK, v -> showTab(1)));
        Ui.grid(this, p, stats, 2);

        p.addView(Ui.sectionTitle(this, "\u05E4\u05E2\u05D5\u05DC\u05D5\u05EA \u05DE\u05D4\u05D9\u05E8\u05D5\u05EA"));
        List<View> quick = new ArrayList<>();
        quick.add(Ui.tile(this, "\uD83D\uDCDD", "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4", "", 0, v -> startActivity(new Intent(this, TicketActivity.class))));
        quick.add(Ui.tile(this, "\uD83D\uDC64", "\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9", "", 0, v -> startActivity(new Intent(this, ClientEditActivity.class))));
        quick.add(Ui.tile(this, "\uD83D\uDCB0", "\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8", "", 0, v -> startActivity(new Intent(this, QuotesActivity.class))));
        quick.add(Ui.tile(this, "\uD83D\uDCCA", "\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA", "", 0, v -> startActivity(new Intent(this, ReportActivity.class))));
        long lastBk = CallLogger.prefs(this).getLong(Backup.KEY_LAST, 0);
        boolean oldBk = System.currentTimeMillis() - lastBk > 7L * 24 * 3600 * 1000;
        quick.add(Ui.tile(this, "\uD83D\uDCBE", "\u05D2\u05D9\u05D1\u05D5\u05D9", lastBk == 0 ? "\u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2" : TicketDb.fmt(lastBk).substring(0, 10),
                oldBk ? Ui.RED : Ui.GREEN, v -> startBackup()));
        quick.add(Ui.tile(this, "\u2699\uFE0F", "\u05E0\u05D9\u05D4\u05D5\u05DC \u05D5\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA", "", 0, v -> showTab(3)));
        Ui.grid(this, p, quick, 3);

        p.addView(Ui.sectionTitle(this, "\u05DE\u05E2\u05E7\u05D1\u05D9\u05DD \u05E7\u05E8\u05D5\u05D1\u05D9\u05DD"));
        int shownCount = 0;
        for (TicketDb.Ticket t : upcoming) {
            if (t.followupAt == 0 || shownCount >= 5) break;
            p.addView(ticketCard(t), Ui.cardParams(this));
            shownCount++;
        }
        if (shownCount == 0) p.addView(Ui.text(this, "   \u05D0\u05D9\u05DF \u05DE\u05E2\u05E7\u05D1\u05D9\u05DD \u05E4\u05EA\u05D5\u05D7\u05D9\u05DD \uD83D\uDC4D", 14, Ui.MUTED, false));
    }

    private void openTickets(int filter) {
        startActivity(new Intent(this, TicketsActivity.class).putExtra(TicketsActivity.EXTRA_FILTER, filter));
    }

    private LinearLayout ticketCard(final TicketDb.Ticket t) {
        String badge;
        int color;
        if (t.isOverdue()) { badge = "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8"; color = Ui.RED; }
        else if ("Urgent".equals(t.priority)) { badge = "\u05D3\u05D7\u05D5\u05E3"; color = Ui.ORANGE; }
        else if (!t.isOpen()) { badge = "\u05E1\u05D2\u05D5\u05E8\u05D4"; color = Ui.MUTED; }
        else { badge = He.status(t.status); color = Ui.PRIMARY; }
        String sub = "#" + t.id + "  \u2022  " + (t.clientName.isEmpty() ? "\u05DC\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7" : t.clientName)
                + (t.followupAt > 0 && t.isOpen() ? "\n\u05DE\u05E2\u05E7\u05D1: " + TicketDb.fmt(t.followupAt) : "")
                + (t.provider.isEmpty() ? "" : "\n\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA: " + t.provider);
        return Ui.rowCard(this, t.title, sub, badge, color, v -> startActivity(
                new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id)));
    }

    // ------------------------------------------------------------------ Tickets

    private void buildTickets(List<TicketDb.Ticket> all, int open, int late, List<TicketDb.Ticket> openSorted) {
        LinearLayout p = pages[2];
        p.removeAllViews();
        int closed = 0;
        for (TicketDb.Ticket t : all) if (!t.isOpen()) closed++;
        p.addView(Ui.sectionTitle(this, "\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA"));
        List<View> tiles = new ArrayList<>();
        tiles.add(Ui.tile(this, "\uD83D\uDCDD", "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4", "", 0, v -> startActivity(new Intent(this, TicketActivity.class))));
        tiles.add(Ui.tile(this, "\uD83D\uDCC2", "\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", String.valueOf(open), Ui.PRIMARY, v -> openTickets(TicketsActivity.OPEN)));
        tiles.add(Ui.tile(this, "\u23F0", "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8", String.valueOf(late), late > 0 ? Ui.RED : Ui.MUTED, v -> openTickets(TicketsActivity.OVERDUE)));
        tiles.add(Ui.tile(this, "\u2705", "\u05E1\u05D2\u05D5\u05E8\u05D5\u05EA", String.valueOf(closed), Ui.MUTED, v -> openTickets(TicketsActivity.CLOSED)));
        tiles.add(Ui.tile(this, "\uD83D\uDDC2\uFE0F", "\u05D4\u05DB\u05DC", String.valueOf(all.size()), Ui.MUTED, v -> openTickets(TicketsActivity.ALL)));
        tiles.add(Ui.tile(this, "\uD83D\uDCCA", "\u05D3\u05D5\u05D7", "", 0, v -> startActivity(new Intent(this, ReportActivity.class))));
        Ui.grid(this, p, tiles, 3);
        p.addView(Ui.sectionTitle(this, "\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA"));
        int n = 0;
        for (TicketDb.Ticket t : openSorted) {
            if (n++ >= 30) break;
            p.addView(ticketCard(t), Ui.cardParams(this));
        }
        if (openSorted.isEmpty()) p.addView(Ui.text(this, "   \u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA \uD83D\uDC4D", 14, Ui.MUTED, false));
    }

    // ------------------------------------------------------------------ Manage

    private void buildManage(ClientStore s) {
        LinearLayout p = pages[3];
        p.removeAllViews();
        SharedPreferences sp = CallLogger.prefs(this);
        final int ok = Ui.GREEN, need = Ui.RED, info = Ui.MUTED;

        StringBuilder cols = new StringBuilder();
        for (int c : s.phoneCols) {
            if (cols.length() > 0) cols.append(", ");
            cols.append(s.headers.get(c));
        }
        LinearLayout status = Ui.card(this);
        status.addView(Ui.text(this, s.rows.isEmpty() ? "\u05E2\u05D3\u05D9\u05D9\u05DF \u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA - \u05D4\u05EA\u05D7\u05DC \u05D1\u05D9\u05D9\u05D1\u05D5\u05D0" : s.rows.size() + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", 16, Ui.TEXT, true));
        if (!s.rows.isEmpty()) {
            status.addView(Ui.text(this, "\u05E2\u05DE\u05D5\u05D3\u05EA \u05E9\u05DD: " + s.headers.get(s.nameCol)
                    + "   \u2022   \u05E2\u05DE\u05D5\u05D3\u05D5\u05EA \u05D8\u05DC\u05E4\u05D5\u05DF: " + (cols.length() == 0 ? "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5" : cols), 13, Ui.MUTED, false));
        }
        p.addView(status, Ui.cardParams(this));

        p.addView(Ui.sectionTitle(this, "\u05D2\u05D9\u05D1\u05D5\u05D9"));
        long lastBk = sp.getLong(Backup.KEY_LAST, 0);
        boolean oldBk = System.currentTimeMillis() - lastBk > 7L * 24 * 3600 * 1000;
        List<View> bk = new ArrayList<>();
        bk.add(Ui.tile(this, "\uD83D\uDCBE", "\u05D2\u05D9\u05D1\u05D5\u05D9 \u05E0\u05EA\u05D5\u05E0\u05D9\u05DD", lastBk == 0 ? "\u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2" : "\u05D0\u05D7\u05E8\u05D5\u05DF: " + TicketDb.fmt(lastBk).substring(0, 10),
                oldBk ? need : ok, v -> startBackup()));
        bk.add(Ui.tile(this, "\u267B\uFE0F", "\u05E9\u05D7\u05D6\u05D5\u05E8 \u05DE\u05D2\u05D9\u05D1\u05D5\u05D9", "", 0, v -> new AlertDialog.Builder(this)
                .setTitle("\u05E9\u05D7\u05D6\u05D5\u05E8 \u05DE\u05D2\u05D9\u05D1\u05D5\u05D9")
                .setMessage("\u05D4\u05E9\u05D7\u05D6\u05D5\u05E8 \u05D9\u05D7\u05DC\u05D9\u05E3 \u05D0\u05EA \u05DB\u05DC \u05D4\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA, \u05E0\u05D5\u05EA\u05E0\u05D9 \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA, \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05D4\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05E9\u05D1\u05D8\u05DC\u05E4\u05D5\u05DF \u05D1\u05EA\u05D5\u05DB\u05DF \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D2\u05D9\u05D1\u05D5\u05D9. \u05DC\u05D4\u05DE\u05E9\u05D9\u05DA?")
                .setPositiveButton("\u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05D2\u05D9\u05D1\u05D5\u05D9", (d, w) -> Auth.confirm(this, "\u05E9\u05D7\u05D6\u05D5\u05E8 \u05D9\u05D7\u05DC\u05D9\u05E3 \u05D0\u05EA \u05DB\u05DC \u05D4\u05E0\u05EA\u05D5\u05E0\u05D9\u05DD", () -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, REQ_RESTORE);
                }))
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show()));
        Ui.grid(this, p, bk, 3);

        p.addView(Ui.sectionTitle(this, "\u05E0\u05EA\u05D5\u05E0\u05D9 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA"));
        List<View> data = new ArrayList<>();
        data.add(Ui.tile(this, "\uD83D\uDCE5", "\u05D9\u05D9\u05D1\u05D5\u05D0", "\u05D0\u05E7\u05E1\u05DC / Access", info, v -> pickFile()));
        data.add(Ui.tile(this, "\uD83D\uDCE4", "\u05D9\u05D9\u05E6\u05D5\u05D0 \u05DC\u05D0\u05E7\u05E1\u05DC", "", 0, v -> startExport()));
        if (Data.fromAccess(this)) {
            int changes = Data.list(this, Data.KEY_ACC_EDITS).size() + Data.list(this, Data.KEY_ACC_DELETES).size();
            data.add(Ui.tile(this, "\uD83D\uDD04", "\u05E2\u05D3\u05DB\u05D5\u05DF \u05E7\u05D5\u05D1\u05E5 Access", changes == 0 ? "\u05D0\u05D9\u05DF \u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD" : changes + " \u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD",
                    changes == 0 ? info : Ui.ORANGE, v -> startAccessUpdate()));
        } else {
            data.add(Ui.tile(this, "\uD83D\uDD04", "\u05E2\u05D3\u05DB\u05D5\u05DF \u05E7\u05D5\u05D1\u05E5 Access", "\u05DC\u05D0 \u05DE\u05D7\u05D5\u05D1\u05E8", need, v -> new AlertDialog.Builder(this)
                    .setTitle("\u05D7\u05D9\u05D1\u05D5\u05E8 \u05DC\u05E7\u05D5\u05D1\u05E5 Access")
                    .setMessage("\u05DB\u05D3\u05D9 \u05DC\u05E2\u05D3\u05DB\u05DF \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4-Access, \u05D9\u05D9\u05D1\u05D0 \u05D0\u05D5\u05EA\u05D5 \u05E4\u05E2\u05DD \u05D0\u05D7\u05EA \u05D3\u05E8\u05DA \"\u05D9\u05D9\u05D1\u05D5\u05D0\" - \u05D9\u05E9\u05D9\u05E8\u05D5\u05EA \u05DE-Google Drive "
                            + "(\u05D1\u05D7\u05DC\u05D5\u05DF \u05D1\u05D7\u05D9\u05E8\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5: \u05EA\u05E4\u05E8\u05D9\u05D8 \u2630 \u2190 Drive). \u05DE\u05D0\u05D5\u05EA\u05D5 \u05E8\u05D2\u05E2 \u05DB\u05DC \u05DC\u05E7\u05D5\u05D7 \u05E9\u05EA\u05D5\u05E1\u05D9\u05E3, \u05EA\u05E2\u05E8\u05D5\u05DA \u05D0\u05D5 \u05EA\u05DE\u05D7\u05E7 "
                            + "\u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05D9\u05E2\u05D5\u05D3\u05DB\u05DF \u05D1\u05E7\u05D5\u05D1\u05E5.\n\n\u05E9\u05D9\u05DD \u05DC\u05D1: \u05D9\u05D9\u05D1\u05D5\u05D0 \u05DE\u05D7\u05D3\u05E9 \u05DE\u05D7\u05DC\u05D9\u05E3 \u05D0\u05EA \u05E8\u05E9\u05D9\u05DE\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 "
                            + "\u05D1\u05EA\u05D5\u05DB\u05DF \u05D4\u05E7\u05D5\u05D1\u05E5.")
                    .setPositiveButton("\u05D9\u05D9\u05D1\u05D5\u05D0 \u05E2\u05DB\u05E9\u05D9\u05D5", (d, w) -> pickFile())
                    .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null)
                    .show()));
        }
        data.add(Ui.tile(this, "\uD83D\uDC64", "\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9", "", 0, v -> startActivity(new Intent(this, ClientEditActivity.class))));
        data.add(Ui.tile(this, "\uD83D\uDD24", "\u05E2\u05DE\u05D5\u05D3\u05EA \u05E9\u05DD", s.headers.isEmpty() ? "" : s.headers.get(s.nameCol), info, v -> chooseNameColumn()));
        data.add(Ui.tile(this, "\uD83D\uDCDE", "\u05E2\u05DE\u05D5\u05D3\u05D5\u05EA \u05D8\u05DC\u05E4\u05D5\u05DF", cols.toString(), info, v -> choosePhoneColumns()));
        int navCol = Nav.column(this, s);
        String navSuffix = sp.getString(Nav.KEY_SUFFIX, "");
        data.add(Ui.tile(this, "\uD83E\uDDED", "\u05DB\u05EA\u05D5\u05D1\u05EA \u05DC\u05E0\u05D9\u05D5\u05D5\u05D8", navCol < 0 ? "\u05DC\u05D0 \u05E0\u05D1\u05D7\u05E8\u05D4" : s.headers.get(navCol)
                + (navSuffix.isEmpty() ? "" : " + " + navSuffix), navCol < 0 ? need : info, v -> chooseNavColumn()));
        List<String> hiddenCols = Data.hiddenColumns(this);
        data.add(Ui.tile(this, "\uD83D\uDE48", "\u05E9\u05D3\u05D5\u05EA \u05DE\u05D5\u05E1\u05EA\u05E8\u05D9\u05DD", hiddenCols.isEmpty() ? "\u05D0\u05D9\u05DF" : String.join(", ", hiddenCols),
                info, v -> chooseHiddenColumns()));
        Ui.grid(this, p, data, 3);

        p.addView(Ui.sectionTitle(this, "\u05E9\u05D9\u05E8\u05D5\u05EA \u05D5\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5"));
        List<View> svc = new ArrayList<>();
        svc.add(Ui.tile(this, "\uD83D\uDCCA", "\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA", "", 0, v -> startActivity(new Intent(this, ReportActivity.class))));
        svc.add(Ui.tile(this, "\uD83D\uDCB0", "\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8", "", 0, v -> startActivity(new Intent(this, QuotesActivity.class))));
        svc.add(Ui.tile(this, "\uD83C\uDFF7\uFE0F", "\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", "", 0, v -> startActivity(new Intent(this, CatalogActivity.class))));
        svc.add(Ui.tile(this, "\u2709\uFE0F", "\u05EA\u05D1\u05E0\u05D9\u05D5\u05EA \u05D4\u05D5\u05D3\u05E2\u05D4", "", 0, v -> startActivity(new Intent(this, TemplatesActivity.class))));
        svc.add(Ui.tile(this, "\uD83C\uDFE2", "\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA", "", 0, v -> startActivity(new Intent(this, ProvidersActivity.class))));
        int teamSize = Team.list(this).size();
        svc.add(Ui.tile(this, "\uD83D\uDC77", "\u05D0\u05E0\u05E9\u05D9 \u05E6\u05D5\u05D5\u05EA", teamSize == 0 ? "" : teamSize + " \u05D0\u05E0\u05E9\u05D9\u05DD", 0, v -> Team.manage(this)));
        svc.add(Ui.tile(this, "\uD83C\uDFF7\uFE0F", "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D5\u05EA", "", 0, v -> startActivity(new Intent(this, CategoriesActivity.class))));
        Ui.grid(this, p, svc, 3);

        p.addView(Ui.sectionTitle(this, "\u05E9\u05D9\u05D7\u05D5\u05EA \u05D5\u05D9\u05D5\u05DE\u05DF"));
        long calId = sp.getLong(CallLogger.KEY_CAL_ID, 0);
        String calText = calId < 0 ? "\u05DB\u05D1\u05D5\u05D9" : calId == 0 ? "\u05D9\u05D5\u05DE\u05DF \u05E8\u05D0\u05E9\u05D9" : sp.getString(CallLogger.KEY_CAL_NAME, "\u05D9\u05D5\u05DE\u05DF \u05E9\u05E0\u05D1\u05D7\u05E8");
        boolean ask = sp.getBoolean(CallLogger.KEY_ASK, true);
        boolean unknown = sp.getBoolean(CallLogger.KEY_UNKNOWN, false);
        boolean contacts = sp.getBoolean(PhoneContacts.KEY_ENABLED, true);
        List<View> calls = new ArrayList<>();
        calls.add(Ui.tile(this, "\uD83D\uDCC5", "\u05D9\u05D5\u05DE\u05DF \u05DC\u05E9\u05D9\u05D7\u05D5\u05EA", calText, calId < 0 ? info : ok, v -> chooseCalendar()));
        calls.add(Ui.tile(this, "\uD83D\uDCAC", "\u05E9\u05DE\u05D9\u05E8\u05EA \u05E9\u05D9\u05D7\u05D4", ask ? "\u05DC\u05E9\u05D0\u05D5\u05DC \u05DB\u05DC \u05E4\u05E2\u05DD" : "\u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9\u05EA", info, v -> {
            sp.edit().putBoolean(CallLogger.KEY_ASK, !ask).apply();
            refresh();
        }));
        calls.add(Ui.tile(this, "\u2753", "\u05DE\u05E1\u05E4\u05E8\u05D9\u05DD \u05DC\u05D0 \u05DE\u05D5\u05DB\u05E8\u05D9\u05DD", unknown ? "\u05E0\u05E8\u05E9\u05DE\u05D9\u05DD" : "\u05DC\u05D0 \u05E0\u05E8\u05E9\u05DE\u05D9\u05DD", info, v -> {
            sp.edit().putBoolean(CallLogger.KEY_UNKNOWN, !unknown).apply();
            refresh();
        }));
        calls.add(Ui.tile(this, "\uD83D\uDCC7", "\u05D6\u05D9\u05D4\u05D5\u05D9 \u05DE\u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8", contacts ? "\u05E4\u05E2\u05D9\u05DC" : "\u05DB\u05D1\u05D5\u05D9", contacts ? ok : info, v -> {
            sp.edit().putBoolean(PhoneContacts.KEY_ENABLED, !contacts).apply();
            refresh();
        }));
        calls.add(Ui.tile(this, "\uD83E\uDDEA", "\u05D1\u05D3\u05D9\u05E7\u05EA \u05D7\u05DC\u05D5\u05E0\u05D9\u05EA", "\u05DC\u05DC\u05E7\u05D5\u05D7 \u05D4\u05E8\u05D0\u05E9\u05D5\u05DF", info, v -> testPopup()));
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        final boolean waOn = enabledListeners != null && enabledListeners.contains(getPackageName());
        final int waMode = WaMessages.mode(this);
        String[] waModeText = {"\u05DB\u05D1\u05D5\u05D9", "\u05E8\u05E7 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05DE\u05D4\u05D0\u05E7\u05E1\u05DC", "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA + \u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8"};
        calls.add(Ui.tile(this, "\uD83D\uDCDD", "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05DE\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", waModeText[waMode], waMode > 0 ? ok : info, v ->
                new AlertDialog.Builder(this)
                        .setTitle("\u05D4\u05E6\u05E2\u05EA \u05E7\u05E8\u05D9\u05D0\u05D4 \u05DE\u05D4\u05D5\u05D3\u05E2\u05D5\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4")
                        .setSingleChoiceItems(waModeText, waMode, (d, which) -> {
                            sp.edit().putInt(WaMessages.KEY_MODE, which).apply();
                            d.dismiss();
                            refresh();
                        })
                        .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                        .show()));
        calls.add(Ui.tile(this, "\uD83D\uDCAC", "\u05D6\u05D9\u05D4\u05D5\u05D9 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", waOn ? "\u2713 \u05E4\u05E2\u05D9\u05DC" : "\u05DC\u05D0 \u05DE\u05D5\u05E4\u05E2\u05DC", waOn ? ok : info, v -> whatsAppSetup(waOn)));
        Ui.grid(this, p, calls, 3);

        p.addView(Ui.sectionTitle(this, "\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA"));
        List<View> perms = new ArrayList<>();
        boolean hp = hasPermissions();
        perms.add(Ui.tile(this, "\uD83D\uDD10", "\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA", hp ? "\u2713 \u05D0\u05D5\u05E9\u05E8\u05D5" : "\u05E0\u05D3\u05E8\u05E9 \u05D0\u05D9\u05E9\u05D5\u05E8", hp ? ok : need, v -> askPermissions()));
        boolean ov = Settings.canDrawOverlays(this);
        perms.add(Ui.tile(this, "\uD83E\uDE9F", "\u05D4\u05E6\u05D2\u05D4 \u05DE\u05E2\u05DC \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D5\u05EA", ov ? "\u2713 \u05DE\u05D0\u05D5\u05E9\u05E8" : "\u05E0\u05D3\u05E8\u05E9 \u05D0\u05D9\u05E9\u05D5\u05E8", ov ? ok : need,
                v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())))));
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            boolean held = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
            perms.add(Ui.tile(this, "\uD83C\uDD94", "\u05D6\u05D9\u05D4\u05D5\u05D9 \u05DE\u05EA\u05E7\u05E9\u05E8", held ? "\u2713 \u05DE\u05D5\u05D2\u05D3\u05E8" : "\u05E0\u05D3\u05E8\u05E9 \u05D0\u05D9\u05E9\u05D5\u05E8", held ? ok : need, v -> askCallerIdRole()));
        }
        final boolean authOn = Auth.enabled(this);
        perms.add(Ui.tile(this, "\uD83D\uDD0F", "\u05D0\u05D9\u05E9\u05D5\u05E8 \u05DE\u05D7\u05D9\u05E7\u05D4", authOn ? "\u05D8\u05D1\u05D9\u05E2\u05EA \u05D0\u05E6\u05D1\u05E2: \u05E4\u05E2\u05D9\u05DC" : "\u05DB\u05D1\u05D5\u05D9", authOn ? ok : info,
                v -> Auth.confirm(this, authOn ? "\u05DB\u05D9\u05D1\u05D5\u05D9 \u05D0\u05D9\u05E9\u05D5\u05E8 \u05DE\u05D7\u05D9\u05E7\u05D4" : "\u05D4\u05E4\u05E2\u05DC\u05EA \u05D0\u05D9\u05E9\u05D5\u05E8 \u05DE\u05D7\u05D9\u05E7\u05D4", () -> {
                    sp.edit().putBoolean(Auth.KEY_ENABLED, !authOn).apply();
                    refresh();
                })));
        Ui.grid(this, p, perms, 3);

    }

    // ------------------------------------------------------------------ Clients list

    private void refreshList() {
        ClientStore s = Data.get(this);
        List<Integer> rows = s.search(search.getText().toString());
        listBox.removeAllViews();
        int count = 0;
        for (final int r : rows) {
            if (count >= 300) {
                listBox.addView(Ui.text(this, "  \u05DE\u05D5\u05E6\u05D2\u05D9\u05DD 300 \u05E8\u05D0\u05E9\u05D5\u05E0\u05D9\u05DD \u05DE\u05EA\u05D5\u05DA " + rows.size() + " - \u05D7\u05E4\u05E9 \u05DB\u05D3\u05D9 \u05DC\u05E6\u05DE\u05E6\u05DD", 13, Ui.MUTED, false));
                break;
            }
            count++;
            String phone = s.firstPhone(r);
            LinearLayout card = Ui.rowCard(this, s.name(r), phone, "", 0, v -> openCard(r));
            card.setOnLongClickListener(v -> {
                startActivity(new Intent(this, ClientEditActivity.class).putExtra(DetailActivity.EXTRA_ROW, r));
                return true;
            });
            listBox.addView(card, Ui.cardParams(this));
        }
        if (rows.isEmpty()) {
            listBox.addView(Ui.text(this, s.rows.isEmpty() ? "  \u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA - \u05DC\u05D7\u05E5 \"\u05D9\u05D9\u05D1\u05D5\u05D0\" \u05D0\u05D5 \"\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9\"" : "  \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA",
                    14, Ui.MUTED, false));
        }
    }

    private void openCard(int row) {
        startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_ROW, row));
    }

    // ------------------------------------------------------------------ import

    private void pickFile() {
        if (CallLogger.prefs(this).getBoolean(Data.KEY_EDITED, false)) {
            new AlertDialog.Builder(this)
                    .setTitle("\u05D9\u05E9 \u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD \u05E9\u05DC\u05D0 \u05D9\u05D5\u05E6\u05D0\u05D5")
                    .setMessage("\u05D4\u05D5\u05E1\u05E4\u05EA \u05D0\u05D5 \u05E2\u05E8\u05DB\u05EA \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05D5\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0 \u05D9\u05D9\u05E6\u05D0\u05EA \u05D0\u05D5\u05EA\u05DD \u05DC\u05D0\u05E7\u05E1\u05DC. "
                            + "\u05D9\u05D9\u05D1\u05D5\u05D0 \u05E7\u05D5\u05D1\u05E5 \u05D7\u05D3\u05E9 \u05D9\u05DE\u05D7\u05E7 \u05D0\u05EA \u05D4\u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD \u05D4\u05D0\u05DC\u05D4. \u05DE\u05D4 \u05DC\u05E2\u05E9\u05D5\u05EA?")
                    .setPositiveButton("\u05D9\u05D9\u05E6\u05D0 \u05E7\u05D5\u05D3\u05DD", (d, w) -> startExport())
                    .setNeutralButton("\u05D9\u05D9\u05D1\u05D0 \u05D1\u05DB\u05DC \u05D6\u05D0\u05EA", (d, w) -> pickFileNow())
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show();
            return;
        }
        pickFileNow();
    }

    private void startExport() {
        if (Data.get(this).headers.isEmpty()) {
            Toast.makeText(this, "\u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05DC\u05D9\u05D9\u05E6\u05D5\u05D0", Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        i.putExtra(Intent.EXTRA_TITLE, "clients-" + date + ".xlsx");
        startActivityForResult(i, REQ_EXPORT);
    }

    /** Runs on a background thread: loads the rows as the client list. */
    private void finishImport(List<List<String>> table) throws Exception {
        ClientStore s = new ClientStore();
        s.load(table);
        if (s.rows.isEmpty()) throw new IOException("\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D1\u05E7\u05D5\u05D1\u05E5");
        s.saveFile(new File(getFilesDir(), Data.FILE));
        Data.set(this, s);
        CallLogger.prefs(this).edit().putBoolean(Data.KEY_EDITED, false).apply();
        Data.clearAccessChanges(this);
        final int n = s.rows.size();
        runOnUiThread(() -> {
            Toast.makeText(this, "\u05D9\u05D5\u05D1\u05D0\u05D5 " + n + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", Toast.LENGTH_LONG).show();
            refresh();
        });
    }

    /** An Access file with several tables: ask which one holds the clients. */
    private void chooseAccessTable(final File f, final List<String> tables) {
        final String last = CallLogger.prefs(this).getString("access_table", "");
        if (tables.remove(last)) tables.add(0, last);
        new AlertDialog.Builder(this)
                .setTitle("\u05D1\u05D0\u05D9\u05D6\u05D5 \u05D8\u05D1\u05DC\u05D4 \u05E0\u05DE\u05E6\u05D0\u05D9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA?")
                .setItems(tables.toArray(new String[0]), (d, w) -> {
                    final String name = tables.get(w);
                    CallLogger.prefs(this).edit().putString(Data.KEY_ACC_TABLE, name).apply();
                    new Thread(() -> {
                        try {
                            finishImport(AccessImport.read(f, name));
                        } catch (Exception e) {
                            final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                            runOnUiThread(() -> Toast.makeText(this, "\u05E9\u05D2\u05D9\u05D0\u05D4 \u05D1\u05E7\u05E8\u05D9\u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5: " + msg,
                                    Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void pickFileNow() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, REQ_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            Toast.makeText(this, "\u05E7\u05D5\u05E8\u05D0 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5\u2026", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    byte[] bytes;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                        bytes = ClientStore.readAll(in);
                    }
                    if (AccessImport.isAccess(bytes)) {
                        final File f = new File(getFilesDir(), Data.ACCESS_COPY);
                        try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) {
                            o.write(bytes);
                        }
                        try {
                            getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception e1) {
                            try {
                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) { }
                        }
                        CallLogger.prefs(this).edit().putString(Data.KEY_ACC_URI, uri.toString()).apply();
                        final List<String> tables = AccessImport.tables(f);
                        if (tables.isEmpty()) throw new IOException("\u05D0\u05D9\u05DF \u05D8\u05D1\u05DC\u05D0\u05D5\u05EA \u05D1\u05E7\u05D5\u05D1\u05E5 \u05D4-Access");
                        if (tables.size() == 1) {
                            CallLogger.prefs(this).edit().putString(Data.KEY_ACC_TABLE, tables.get(0)).apply();
                            finishImport(AccessImport.read(f, tables.get(0)));
                        }
                        else runOnUiThread(() -> chooseAccessTable(f, tables));
                    } else {
                        CallLogger.prefs(this).edit().remove(Data.KEY_ACC_TABLE).remove(Data.KEY_ACC_URI).apply();
                        finishImport(ClientStore.readTable(bytes));
                    }
                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, "\u05E9\u05D2\u05D9\u05D0\u05D4 \u05D1\u05E7\u05E8\u05D9\u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5: " + msg, Toast.LENGTH_LONG).show());
                }
            }).start();
        } else if (requestCode == REQ_ACCESS_SOURCE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            accessSourcePicked(data.getData());
        } else if (requestCode == REQ_ACCESS_SAVE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            runAccessUpdate(data.getData());
        } else if (requestCode == REQ_EXPORT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            new Thread(() -> {
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05DB\u05EA\u05D5\u05D1 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                    Data.get(this).writeXlsx(out);
                    CallLogger.prefs(this).edit().putBoolean(Data.KEY_EDITED, false).apply();
                    runOnUiThread(() -> Toast.makeText(this, "\u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC \u05E0\u05E9\u05DE\u05E8", Toast.LENGTH_LONG).show());
                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, "\u05D4\u05D9\u05D9\u05E6\u05D5\u05D0 \u05E0\u05DB\u05E9\u05DC: " + msg, Toast.LENGTH_LONG).show());
                }
            }).start();
        } else if ((requestCode == REQ_BACKUP || requestCode == REQ_RESTORE)
                && resultCode == RESULT_OK && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            final boolean backup = requestCode == REQ_BACKUP;
            new Thread(() -> {
                try {
                    if (backup) {
                        try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                            if (out == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05DB\u05EA\u05D5\u05D1 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                            Backup.write(this, out);
                        }
                    } else {
                        try (InputStream in = getContentResolver().openInputStream(uri)) {
                            if (in == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                            Backup.restore(this, in);
                        }
                    }
                    runOnUiThread(() -> {
                        Toast.makeText(this, backup ? "\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05E0\u05E9\u05DE\u05E8" : "\u05D4\u05E9\u05D7\u05D6\u05D5\u05E8 \u05D4\u05D5\u05E9\u05DC\u05DD", Toast.LENGTH_LONG).show();
                        refresh();
                    });
                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, (backup ? "\u05D4\u05D2\u05D9\u05D1\u05D5\u05D9 \u05E0\u05DB\u05E9\u05DC: " : "\u05D4\u05E9\u05D7\u05D6\u05D5\u05E8 \u05E0\u05DB\u05E9\u05DC: ") + msg,
                            Toast.LENGTH_LONG).show());
                }
            }).start();
        } else if (requestCode == REQ_ROLE) {
            refresh();
        }
    }

    // ------------------------------------------------------------------ permissions

    private String[] neededPermissions() {
        if (Build.VERSION.SDK_INT >= 33) {
            return new String[]{Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG, Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CONTACTS};
        }
        return new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.READ_CONTACTS};
    }

    private boolean hasPermissions() {
        for (String perm : neededPermissions()) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void askPermissions() {
        if (hasPermissions()) {
            Toast.makeText(this, "\u05DB\u05D1\u05E8 \u05D0\u05D5\u05E9\u05E8", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(neededPermissions(), REQ_PERMS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != REQ_PERMS) return;
        for (int i = 0; i < permissions.length; i++) {
            if (results[i] != PackageManager.PERMISSION_GRANTED
                    && !shouldShowRequestPermissionRationale(permissions[i])) {
                // Permanently denied: send the user to the app's settings page.
                Toast.makeText(this, "\u05E4\u05EA\u05D7 \u05D4\u05E8\u05E9\u05D0\u05D5\u05EA \u05D5\u05D0\u05E9\u05E8 \u05D8\u05DC\u05E4\u05D5\u05DF, \u05D9\u05D5\u05DE\u05DF \u05E9\u05D9\u05D7\u05D5\u05EA, \u05D9\u05D5\u05DE\u05DF \u05D5\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA",
                        Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
                break;
            }
        }
        refresh();
    }

    private void askCallerIdRole() {
        if (Build.VERSION.SDK_INT < 29) return;
        RoleManager rm = getSystemService(RoleManager.class);
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "\u05DC\u05D0 \u05D6\u05DE\u05D9\u05DF \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF \u05D6\u05D4 - \u05E9\u05DC\u05D1\u05D9\u05DD 2 \u05D5-3 \u05DE\u05E1\u05E4\u05D9\u05E7\u05D9\u05DD", Toast.LENGTH_LONG).show();
            return;
        }
        if (rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "\u05DB\u05D1\u05E8 \u05DE\u05D5\u05D2\u05D3\u05E8", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE);
    }

    private void chooseCalendar() {
        if (!CallLogger.hasPermissions(this)) {
            Toast.makeText(this, "\u05D1\u05E6\u05E2 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E9\u05DC\u05D1 2 (\u05D4\u05E8\u05E9\u05D0\u05EA \u05D9\u05D5\u05DE\u05DF)", Toast.LENGTH_LONG).show();
            return;
        }
        final List<Long> ids = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        ids.add(-1L);
        labels.add("\u05DB\u05D1\u05D5\u05D9 - \u05DC\u05D0 \u05DC\u05E8\u05E9\u05D5\u05DD \u05E9\u05D9\u05D7\u05D5\u05EA");
        ids.add(0L);
        labels.add("\u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9 - \u05D9\u05D5\u05DE\u05DF \u05E8\u05D0\u05E9\u05D9");
        CallLogger.listCalendars(this, ids, labels);
        new AlertDialog.Builder(this)
                .setTitle("\u05D9\u05D5\u05DE\u05DF \u05DC\u05E8\u05D9\u05E9\u05D5\u05DD \u05E9\u05D9\u05D7\u05D5\u05EA \u05D5\u05DE\u05E2\u05E7\u05D1\u05D9\u05DD")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    CallLogger.prefs(this).edit()
                            .putLong(CallLogger.KEY_CAL_ID, ids.get(which))
                            .putString(CallLogger.KEY_CAL_NAME, labels.get(which))
                            .apply();
                    refresh();
                })
                .show();
    }

    /** Guide for turning on notification access (needed to see WhatsApp calls). */
    private void whatsAppSetup(boolean on) {
        new AlertDialog.Builder(this)
                .setTitle("\u05D6\u05D9\u05D4\u05D5\u05D9 \u05DE\u05EA\u05E7\u05E9\u05E8\u05D9\u05DD \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4")
                .setMessage((on ? "\u05D4\u05D6\u05D9\u05D4\u05D5\u05D9 \u05E4\u05E2\u05D9\u05DC.\n\n" : "")
                        + "\u05DB\u05D3\u05D9 \u05DC\u05D6\u05D4\u05D5\u05EA \u05E9\u05D9\u05D7\u05D5\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4, \u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05E7\u05D5\u05E8\u05D0\u05EA \u05D0\u05EA \u05D4\u05D4\u05EA\u05E8\u05D0\u05D4 \"\u05E9\u05D9\u05D7\u05D4 \u05E0\u05DB\u05E0\u05E1\u05EA\" \u05E9\u05DC \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4.\n\n"
                        + "\u05D4\u05E4\u05E2\u05DC\u05D4 (\u05E4\u05E2\u05DD \u05D0\u05D7\u05EA):\n"
                        + "1. \u05DC\u05D7\u05E5 \"\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4\" \u2190 \u05EA\u05E4\u05E8\u05D9\u05D8 \u22EE \u05DC\u05DE\u05E2\u05DC\u05D4 \u2190 \"\u05D0\u05E4\u05E9\u05E8 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05DE\u05D5\u05D2\u05D1\u05DC\u05D5\u05EA\" (\u05D0\u05DD \u05DE\u05D5\u05E4\u05D9\u05E2).\n"
                        + "2. \u05D7\u05D6\u05D5\u05E8 \u05D5\u05DC\u05D7\u05E5 \"\u05D2\u05D9\u05E9\u05D4 \u05DC\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA\" \u05D5\u05D4\u05E4\u05E2\u05DC \u05D0\u05EA \"\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05E9\u05D9\u05E8\u05D5\u05EA\".\n\n"
                        + "\u05D4\u05D6\u05D9\u05D4\u05D5\u05D9 \u05E2\u05D5\u05D1\u05D3 \u05DB\u05E9\u05D4\u05E9\u05DD \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05D6\u05D4\u05D4 \u05DC\u05E9\u05DD \u05D1\u05D0\u05E7\u05E1\u05DC, \u05DB\u05E9\u05D4\u05DE\u05E1\u05E4\u05E8 \u05DC\u05D0 \u05E9\u05DE\u05D5\u05E8 (\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DE\u05E6\u05D9\u05D2 \u05DE\u05E1\u05E4\u05E8), "
                        + "\u05D0\u05D5 \u05D3\u05E8\u05DA \u05D4\u05DE\u05E1\u05E4\u05E8 \u05D4\u05E9\u05DE\u05D5\u05E8 \u05D1\u05D0\u05E0\u05E9\u05D9 \u05D4\u05E7\u05E9\u05E8.")
                .setPositiveButton("\u05D2\u05D9\u05E9\u05D4 \u05DC\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA", (d, w) -> {
                    try {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    } catch (Exception e) {
                        Toast.makeText(this, "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0 \u05DE\u05E1\u05DA \u05D4\u05D4\u05D2\u05D3\u05E8\u05D4", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("\u05DE\u05D4 \u05D6\u05D5\u05D4\u05D4 \u05DC\u05D0\u05D7\u05E8\u05D5\u05E0\u05D4", (d, w) -> showWaLog())
                .setNeutralButton("\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4", (d, w) -> startActivity(new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                .show();
    }

    private void showWaLog() {
        SharedPreferences sp = CallLogger.prefs(this);
        long conn = sp.getLong(WaCallListener.KEY_CONNECTED, 0);
        String log = sp.getString(WaCallListener.KEY_LOG, "");
        new AlertDialog.Builder(this)
                .setTitle("\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA \u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05D0\u05D7\u05E8\u05D5\u05E0\u05D5\u05EA")
                .setMessage("\u05D7\u05D9\u05D1\u05D5\u05E8 \u05DC\u05E9\u05D9\u05E8\u05D5\u05EA \u05D4\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA: " + (conn == 0 ? "\u05E2\u05D3\u05D9\u05D9\u05DF \u05DC\u05D0 \u05D4\u05EA\u05D7\u05D1\u05E8" : TicketDb.fmt(conn))
                        + "\n\n" + (log.isEmpty() ? "\u05DC\u05D0 \u05D4\u05EA\u05E7\u05D1\u05DC\u05D5 \u05D4\u05EA\u05E8\u05D0\u05D5\u05EA \u05DE\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05E2\u05D3\u05D9\u05D9\u05DF." : log))
                .setPositiveButton("\u05E1\u05D2\u05D5\u05E8", null)
                .show();
    }

    // ------------------------------------------------------------------ Access write-back

    private void startAccessUpdate() {
        final List<String> edits = Data.list(this, Data.KEY_ACC_EDITS);
        final List<String> dels = Data.list(this, Data.KEY_ACC_DELETES);
        if (edits.isEmpty() && dels.isEmpty()) {
            Toast.makeText(this, "\u05D0\u05D9\u05DF \u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD \u05DC\u05E2\u05D3\u05DB\u05D5\u05DF \u05D1-Access", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("\u05E2\u05D3\u05DB\u05D5\u05DF \u05E7\u05D5\u05D1\u05E5 Access")
                .setMessage(edits.size() + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05DC\u05E2\u05D3\u05DB\u05D5\u05DF/\u05D4\u05D5\u05E1\u05E4\u05D4, " + dels.size() + " \u05DC\u05DE\u05D7\u05D9\u05E7\u05D4.\n\n"
                        + "\u05D1\u05D7\u05E8 \u05E2\u05DB\u05E9\u05D9\u05D5 \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4-Access \u05D4\u05E2\u05D3\u05DB\u05E0\u05D9 (\u05DC\u05DE\u05E9\u05DC \u05DE-Google Drive \u2190 Metzer). "
                        + "\u05D4\u05E9\u05D9\u05E0\u05D5\u05D9\u05D9\u05DD \u05DE\u05D4\u05D8\u05DC\u05E4\u05D5\u05DF \u05D9\u05D9\u05DB\u05EA\u05D1\u05D5 \u05DC\u05EA\u05D5\u05DB\u05D5, \u05D5\u05E9\u05D0\u05E8 \u05D4\u05E8\u05E9\u05D5\u05DE\u05D5\u05EA \u05DC\u05D0 \u05D9\u05E9\u05EA\u05E0\u05D5.\n\n\u05D5\u05D3\u05D0 \u05E9\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E1\u05D2\u05D5\u05E8 \u05D1-Access \u05D1\u05DE\u05D7\u05E9\u05D1.")
                .setPositiveButton("\u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 Access", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    startActivityForResult(i, REQ_ACCESS_SOURCE);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    /** The user picked the current Access file: keep a fresh copy, then ask where to save the result. */
    private void accessSourcePicked(final Uri uri) {
        Toast.makeText(this, "\u05E7\u05D5\u05E8\u05D0 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5\u2026", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                byte[] bytes;
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5");
                    bytes = ClientStore.readAll(in);
                }
                if (bytes.length == 0) throw new IOException("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E9\u05E0\u05D1\u05D7\u05E8 \u05E8\u05D9\u05E7 (0 \u05D1\u05EA\u05D9\u05DD). \u05D1\u05D7\u05E8 \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4-Access \u05D4\u05DE\u05E7\u05D5\u05E8\u05D9.");
                if (!AccessImport.isAccess(bytes)) throw new IOException("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E9\u05E0\u05D1\u05D7\u05E8 \u05D0\u05D9\u05E0\u05D5 \u05E7\u05D5\u05D1\u05E5 Access");
                final File copy = new File(getFilesDir(), Data.ACCESS_COPY);
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(copy)) {
                    o.write(bytes);
                }
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                } catch (Exception ignored) { }
                CallLogger.prefs(this).edit().putString(Data.KEY_ACC_URI, uri.toString()).apply();
                final List<String> tables = AccessImport.tables(copy);
                String table = CallLogger.prefs(this).getString(Data.KEY_ACC_TABLE, "");
                if (!tables.contains(table)) {
                    if (tables.size() == 1) {
                        CallLogger.prefs(this).edit().putString(Data.KEY_ACC_TABLE, tables.get(0)).apply();
                    } else {
                        runOnUiThread(() -> new AlertDialog.Builder(this)
                                .setTitle("\u05D1\u05D0\u05D9\u05D6\u05D5 \u05D8\u05D1\u05DC\u05D4 \u05E0\u05DE\u05E6\u05D0\u05D9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA?")
                                .setItems(tables.toArray(new String[0]), (d, w) -> {
                                    CallLogger.prefs(this).edit().putString(Data.KEY_ACC_TABLE, tables.get(w)).apply();
                                    showSaveOptions(uri, bytes.length);
                                })
                                .show());
                        return;
                    }
                }
                final int size = bytes.length;
                runOnUiThread(() -> showSaveOptions(uri, size));
            } catch (Throwable e) {
                final String msg = errorText(e);
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E7\u05E8\u05D5\u05D0 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5")
                        .setMessage(msg).setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", null).show());
            }
        }).start();
    }

    private void showSaveOptions(final Uri source, int size) {
        final String[] opts = {
                "\uD83D\uDCE4 \u05E9\u05EA\u05E3 / \u05E9\u05DE\u05D5\u05E8 \u05DC-Drive",
                "\uD83D\uDCC1 \u05E9\u05DE\u05D5\u05E8 \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF (\u05EA\u05D9\u05E7\u05D9\u05D9\u05EA \u05D4\u05D5\u05E8\u05D3\u05D5\u05EA)",
                "\uD83D\uDCBE \u05E9\u05DE\u05D5\u05E8 \u05DB\u05E7\u05D5\u05D1\u05E5 \u05D7\u05D3\u05E9",
                "\u270F\uFE0F \u05E2\u05D3\u05DB\u05DF \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5 \u05E9\u05E0\u05D1\u05D7\u05E8: " + describe(source),
                "\uD83D\uDCE7 \u05E9\u05DC\u05D7 \u05D1\u05DE\u05D9\u05D9\u05DC (Gmail)"};
        new AlertDialog.Builder(this)
                .setTitle("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E0\u05E7\u05E8\u05D0 (" + (size / 1024) + " KB). \u05D0\u05D9\u05E4\u05D4 \u05DC\u05E9\u05DE\u05D5\u05E8 \u05D0\u05EA \u05D4\u05DE\u05E2\u05D5\u05D3\u05DB\u05DF?")
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        runAccessUpdate(null);
                    } else if (w == 1) {
                        runAccessUpdate(Uri.parse("downloads:"));
                    } else if (w == 2) {
                        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("application/octet-stream");
                        String ext = AccessImport.extension(new File(getFilesDir(), Data.ACCESS_COPY));
                        i.putExtra(Intent.EXTRA_TITLE, "clients-" + date + "." + ext);
                        startActivityForResult(i, REQ_ACCESS_SAVE);
                    } else if (w == 3) {
                        runAccessUpdate(source);
                    } else {
                        runAccessUpdate(Uri.parse("gmail:"));
                    }
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    /** Writes bytes to a document; checks the result is not empty. Drive needs mode "w" (not "wt"). */
    private String writeDocument(Uri target, byte[] out) throws IOException {
        String auth = target.getAuthority() == null ? "" : target.getAuthority();
        boolean drive = auth.contains("com.google.android.apps.docs");
        String[] modes = drive ? new String[]{"w", "rwt", "wt"} : new String[]{"wt", "w", "rwt"};
        Exception lastErr = null;
        for (String mode : modes) {
            try (android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(target, mode)) {
                if (pfd == null) continue;
                try (java.io.FileOutputStream os = new java.io.FileOutputStream(pfd.getFileDescriptor())) {
                    os.write(out);
                    os.flush();
                    try { os.getFD().sync(); } catch (Exception ignored) { }
                }
            } catch (Exception e) {
                lastErr = e;
                continue;
            }
            long len = readLength(target);
            if (len <= 0) {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) { }
                len = readLength(target);
            }
            if (len == out.length) return "\u2713 \u05D0\u05D5\u05DE\u05EA (" + (out.length / 1024) + " KB)";
            if (len > 0 && !drive) return "\u05E0\u05DB\u05EA\u05D1\u05D5 " + len + " \u05D1\u05EA\u05D9\u05DD";
        }
        throw new IOException("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E0\u05E9\u05DE\u05E8 \u05E8\u05D9\u05E7" + (lastErr == null ? "" : " (" + lastErr.getMessage() + ")")
                + ". \u05D4\u05E9\u05EA\u05DE\u05E9 \u05D1\"\u05E9\u05EA\u05E3 / \u05E9\u05DE\u05D5\u05E8 \u05DC-Drive\".");
    }

    private long readLength(Uri u) {
        try (InputStream in = getContentResolver().openInputStream(u)) {
            return in == null ? -1 : ClientStore.readAll(in).length;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Reads the newest Access file, writes the app's changes into it, and saves it to target. */
    private void runAccessUpdate(final Uri targetIn) {
        final boolean viaGmail = targetIn != null && "gmail".equals(targetIn.getScheme());
        final Uri target = viaGmail ? null : targetIn;
        Toast.makeText(this, "\u05DE\u05E2\u05D3\u05DB\u05DF \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4-Access\u2026", Toast.LENGTH_SHORT).show();
        final String uriStr = CallLogger.prefs(this).getString(Data.KEY_ACC_URI, "");
        new Thread(() -> {
            try {
                // 1. Start from the copy of the Access file the user just picked.
                File work = new File(getCacheDir(), "work.accdb");
                byte[] src;
                File copy = new File(getFilesDir(), Data.ACCESS_COPY);
                if (!copy.exists()) throw new IOException("\u05DC\u05D0 \u05E0\u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 Access. \u05DC\u05D7\u05E5 \u05E9\u05D5\u05D1 \u05E2\u05DC \"\u05E2\u05D3\u05DB\u05D5\u05DF \u05E7\u05D5\u05D1\u05E5 Access\" \u05D5\u05D1\u05D7\u05E8 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5.");
                try (InputStream in = new java.io.FileInputStream(copy)) {
                    src = ClientStore.readAll(in);
                }
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(work)) {
                    o.write(src);
                }

                // 2. Apply the app's changes.
                ClientStore s = Data.get(this);
                List<String> edits = Data.list(this, Data.KEY_ACC_EDITS);
                List<List<String>> changed = new ArrayList<>();
                List<Integer> changedRows = new ArrayList<>();
                for (int r = 0; r < s.rows.size(); r++) {
                    if (edits.contains(Data.identity(s, r))) {
                        changed.add(s.rows.get(r));
                        changedRows.add(r);
                    }
                }
                List<String> dels = new ArrayList<>();
                for (String d : Data.list(this, Data.KEY_ACC_DELETES)) dels.add(d.substring(2));
                String table = CallLogger.prefs(this).getString(Data.KEY_ACC_TABLE, "");
                AccessExport.Result res = AccessExport.apply(work, table, s.headers, changed, dels,
                        Data.ID_COL, s.headers.get(s.nameCol));

                if (src.length == 0) throw new IOException("\u05E7\u05D5\u05D1\u05E5 \u05D4\u05DE\u05E7\u05D5\u05E8 \u05E9\u05E0\u05E7\u05E8\u05D0 \u05DE-Drive \u05E8\u05D9\u05E7 (0 \u05D1\u05EA\u05D9\u05DD). \u05E4\u05EA\u05D7 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5 \u05E4\u05E2\u05DD \u05D0\u05D7\u05EA \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D9\u05EA Drive \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF \u05D5\u05E0\u05E1\u05D4 \u05E9\u05D5\u05D1.");
                final long srcLen = src.length;
                // 2b. Make sure the result opens and reads completely before writing it anywhere.
                int rowsAfter;
                try {
                    rowsAfter = AccessImport.verify(work, table);
                } catch (Exception ve) {
                    throw new IOException("\u05D1\u05D3\u05D9\u05E7\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5 \u05D4\u05DE\u05E2\u05D5\u05D3\u05DB\u05DF \u05E0\u05DB\u05E9\u05DC\u05D4 - \u05D4\u05E7\u05D5\u05D1\u05E5 \u05DC\u05D0 \u05E0\u05E9\u05DE\u05E8, \u05D4\u05DE\u05E7\u05D5\u05E8\u05D9 \u05DC\u05D0 \u05D4\u05E9\u05EA\u05E0\u05D4. (" + ve.getMessage() + ")");
                }
                // Keep the version before this update, so it can be restored.
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(new File(getFilesDir(), "access-before-update.bin"))) {
                    o.write(src);
                }

                // 3. Write the result.
                byte[] out;
                try (InputStream in = new java.io.FileInputStream(work)) {
                    out = ClientStore.readAll(in);
                }
                if (out.length == 0) throw new IOException("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05D4\u05DE\u05E2\u05D5\u05D3\u05DB\u05DF \u05E8\u05D9\u05E7 - \u05DC\u05D0 \u05E0\u05E9\u05DE\u05E8");
                final String where;
                final String verifiedText;
                final boolean drive;
                File shared = null;
                if (target == null) {
                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
                    File dir = new File(getCacheDir(), "share");
                    dir.mkdirs();
                    shared = new File(dir, "clients-" + date + "." + AccessImport.extension(work));
                    try (java.io.FileOutputStream o = new java.io.FileOutputStream(shared)) {
                        o.write(out);
                    }
                    where = shared.getName() + " (\u05DC\u05E9\u05D9\u05EA\u05D5\u05E3)";
                    verifiedText = "\u2713 " + (out.length / 1024) + " KB";
                    drive = false;
                } else if ("downloads".equals(target.getScheme())) {
                    String date = new java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.US).format(new java.util.Date());
                    String name = "clients-" + date + "." + AccessImport.extension(work);
                    android.content.ContentValues cv = new android.content.ContentValues();
                    cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name);
                    cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                    cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/");
                    Uri dl = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (dl == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05D9\u05E6\u05D5\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05D1\u05EA\u05D9\u05E7\u05D9\u05D9\u05EA \u05D4\u05D4\u05D5\u05E8\u05D3\u05D5\u05EA");
                    try (java.io.OutputStream os = getContentResolver().openOutputStream(dl)) {
                        if (os == null) throw new IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05DB\u05EA\u05D5\u05D1");
                        os.write(out);
                    }
                    verifiedText = "\u2713 " + (readLength(dl) / 1024) + " KB";
                    where = name + " (\u05D4\u05E7\u05D1\u05E6\u05D9\u05DD \u05E9\u05DC\u05D9 \u2190 \u05D4\u05D5\u05E8\u05D3\u05D5\u05EA)";
                    drive = false;
                } else {
                    verifiedText = writeDocument(target, out);
                    where = describe(target);
                    drive = target.getAuthority() != null && target.getAuthority().contains("com.google.android.apps.docs");
                }
                final File sharedFile = shared;
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(new File(getFilesDir(), Data.ACCESS_COPY))) {
                    o.write(out);
                }

                // 4. New clients got an ID from Access: keep it in the app too.
                int ic = s.headers.indexOf(Data.ID_COL);
                if (ic >= 0) {
                    for (java.util.Map.Entry<Integer, String> e : res.newKeys.entrySet()) {
                        s.rows.get(changedRows.get(e.getKey())).set(ic, e.getValue());
                    }
                }
                s.saveFile(new File(getFilesDir(), Data.FILE));
                Data.reset();
                Data.clearAccessChanges(this);
                CallLogger.prefs(this).edit().putBoolean(Data.KEY_EDITED, false).apply();

                final String msg = "\u05E0\u05E9\u05DE\u05E8 \u05D0\u05DC: " + where + "  " + verifiedText + "\n"
                        + "\u05D2\u05D5\u05D3\u05DC: \u05DE\u05E7\u05D5\u05E8 " + (srcLen / 1024) + " KB \u2190 \u05DE\u05E2\u05D5\u05D3\u05DB\u05DF " + (out.length / 1024) + " KB\n\n"
                        + (drive ? "Google Drive \u05DE\u05E1\u05E0\u05DB\u05E8\u05DF \u05DC\u05DE\u05D7\u05E9\u05D1 \u05EA\u05D5\u05DA \u05D3\u05E7\u05D4-\u05E9\u05EA\u05D9\u05D9\u05DD. \u05D0\u05DD \u05D4\u05E7\u05D5\u05D1\u05E5 \u05E4\u05EA\u05D5\u05D7 \u05D1-Access \u05D1\u05DE\u05D7\u05E9\u05D1, \u05E1\u05D2\u05D5\u05E8 \u05D0\u05D5\u05EA\u05D5 - "
                        + "\u05D0\u05D7\u05E8\u05EA Drive \u05D9\u05E9\u05DE\u05D5\u05E8 \u05E2\u05D5\u05EA\u05E7 \u05E0\u05D5\u05E1\u05E3 \u05D1\u05E9\u05DD \u05E2\u05DD (1).\n\n" : "")
                        + "\u05D1\u05E7\u05D5\u05D1\u05E5 " + rowsAfter + " \u05E8\u05E9\u05D5\u05DE\u05D5\u05EA.\n"
                        + "\u05E7\u05D5\u05D1\u05E5 \u05D4-Access \u05E2\u05D5\u05D3\u05DB\u05DF: " + res.updated + " \u05E2\u05D5\u05D3\u05DB\u05E0\u05D5, " + res.added + " \u05E0\u05D5\u05E1\u05E4\u05D5, "
                        + res.deleted + " \u05E0\u05DE\u05D7\u05E7\u05D5"
                        + (res.notFound > 0 ? "\n" + res.notFound + " \u05DC\u05DE\u05D7\u05D9\u05E7\u05D4 \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5 \u05D1\u05E7\u05D5\u05D1\u05E5" : "")
                        + (res.warnings.isEmpty() ? "" : "\n\n\u05E9\u05D3\u05D5\u05EA \u05E9\u05DC\u05D0 \u05E0\u05E9\u05DE\u05E8\u05D5:\n" + String.join("\n", res.warnings));
                runOnUiThread(() -> {
                    if (sharedFile != null) {
                        Uri u = FilesProvider.uriFor(sharedFile);
                        Intent si = new Intent(Intent.ACTION_SEND).setType("application/octet-stream")
                                .putExtra(Intent.EXTRA_STREAM, u)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        si.setClipData(android.content.ClipData.newRawUri("", u));
                        if (viaGmail) {
                            si.setPackage("com.google.android.gm");
                            si.putExtra(Intent.EXTRA_SUBJECT, "\u05D1\u05D3\u05D9\u05E7\u05EA Access");
                            si.putExtra(Intent.EXTRA_TEXT, "\u05E7\u05D5\u05D1\u05E5 Access \u05DE\u05E2\u05D5\u05D3\u05DB\u05DF: " + sharedFile.getName()
                                    + " (" + (sharedFile.length() / 1024) + " KB)");
                            try {
                                startActivity(si);
                            } catch (Exception e) {
                                si.setPackage(null);
                                startActivity(Intent.createChooser(si, "\u05E9\u05DC\u05D9\u05D7\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5"));
                            }
                        } else {
                            startActivity(Intent.createChooser(si, "\u05E9\u05DE\u05D5\u05E8 \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4-Access (\u05D1\u05D7\u05E8: \u05E9\u05DE\u05D9\u05E8\u05D4 \u05D1-Drive)"));
                        }
                    }
                    new AlertDialog.Builder(this).setTitle("\u05D4\u05E2\u05D3\u05DB\u05D5\u05DF \u05D4\u05D5\u05E9\u05DC\u05DD").setMessage(msg)
                            .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", null).show();
                    refresh();
                });
            } catch (Throwable e) {
                final String msg = errorText(e);
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("\u05D4\u05E2\u05D3\u05DB\u05D5\u05DF \u05E0\u05DB\u05E9\u05DC")
                        .setMessage(msg + "\n\n\u05D0\u05E4\u05E9\u05E8 \u05DC\u05E0\u05E1\u05D5\u05EA \"\u05E9\u05DE\u05D5\u05E8 \u05DB\u05E7\u05D5\u05D1\u05E5 \u05D7\u05D3\u05E9\".")
                        .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", null).show());
            }
        }).start();
    }

    /** "file name (Google Drive)" for a document Uri. */
    private String describe(Uri u) {
        String name = u.getLastPathSegment();
        try (android.database.Cursor c = getContentResolver().query(u,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && c.getString(0) != null) name = c.getString(0);
        } catch (Exception ignored) { }
        String auth = u.getAuthority() == null ? "" : u.getAuthority();
        String place = auth.contains("com.google.android.apps.docs") ? "Google Drive"
                : auth.contains("externalstorage") || auth.contains("media") || auth.contains("downloads") ? "\u05D6\u05D9\u05DB\u05E8\u05D5\u05DF \u05D4\u05D8\u05DC\u05E4\u05D5\u05DF"
                : auth.contains("onedrive") || auth.contains("skydrive") ? "OneDrive" : auth;
        return name + " (" + place + ")";
    }

    private void installCrashCatcher() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        if (prev instanceof CrashSaver) return;
        Thread.setDefaultUncaughtExceptionHandler(new CrashSaver(getApplicationContext(), prev));
    }

    static final class CrashSaver implements Thread.UncaughtExceptionHandler {
        private final android.content.Context ctx;
        private final Thread.UncaughtExceptionHandler prev;

        CrashSaver(android.content.Context c, Thread.UncaughtExceptionHandler p) {
            ctx = c;
            prev = p;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                CallLogger.prefs(ctx).edit().putString("last_crash", errorText(e)).commit();
            } catch (Throwable ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        }
    }

    /** Full error text for troubleshooting: type, message, cause and where it happened. */
    static String errorText(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable t = e;
        int depth = 0;
        while (t != null && depth < 3) {
            if (depth > 0) sb.append("\n\u2190 ");
            sb.append(t.getClass().getSimpleName());
            if (t.getMessage() != null) sb.append(": ").append(t.getMessage());
            StackTraceElement[] st = t.getStackTrace();
            for (int k = 0; k < Math.min(3, st.length); k++) {
                sb.append("\n   at ").append(st[k].getClassName().replace("com.healthmarketscience.jackcess.", "jk."))
                        .append('.').append(st[k].getMethodName()).append(':').append(st[k].getLineNumber());
            }
            t = t.getCause();
            depth++;
        }
        return sb.toString();
    }

    private void startBackup() {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_TITLE, "backup-service-" + date + ".zip");
        Toast.makeText(this, "\u05D1\u05D7\u05E8 \u05D4\u05D9\u05DB\u05DF \u05DC\u05E9\u05DE\u05D5\u05E8 - \u05DE\u05D5\u05DE\u05DC\u05E5 Google Drive", Toast.LENGTH_LONG).show();
        startActivityForResult(i, REQ_BACKUP);
    }

    private void chooseNavColumn() {
        Nav.chooseColumn(this, this::refresh);
    }

    /** Which Excel columns to hide in the client card. */
    private void chooseHiddenColumns() {
        final ClientStore s = Data.get(this);
        if (s.headers.isEmpty()) {
            Toast.makeText(this, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] names = s.headers.toArray(new String[0]);
        final boolean[] checked = new boolean[names.length];
        List<String> hidden = Data.hiddenColumns(this);
        for (int i = 0; i < names.length; i++) checked[i] = hidden.contains(names[i].trim());
        new AlertDialog.Builder(this)
                .setTitle("\u05E1\u05DE\u05DF \u05E9\u05D3\u05D5\u05EA \u05E9\u05DC\u05D0 \u05D9\u05D5\u05E6\u05D2\u05D5 \u05D1\u05DB\u05E8\u05D8\u05D9\u05E1 \u05D4\u05DC\u05E7\u05D5\u05D7")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", (d, w) -> {
                    List<String> chosen = new ArrayList<>();
                    for (int i = 0; i < names.length; i++) if (checked[i]) chosen.add(names[i].trim());
                    Data.saveHiddenColumns(this, chosen);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void chooseNameColumn() {
        final ClientStore s = Data.get(this);
        if (s.headers.isEmpty()) {
            Toast.makeText(this, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] names = s.headers.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("\u05D1\u05D0\u05D9\u05D6\u05D5 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05E0\u05DE\u05E6\u05D0 \u05E9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7?")
                .setSingleChoiceItems(names, s.nameCol, (d, which) -> {
                    Data.saveNameColumn(this, names[which]);
                    d.dismiss();
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void choosePhoneColumns() {
        final ClientStore s = Data.get(this);
        if (s.headers.isEmpty()) {
            Toast.makeText(this, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] names = s.headers.toArray(new String[0]);
        final boolean[] checked = new boolean[names.length];
        for (int c : s.phoneCols) checked[c] = true;
        new AlertDialog.Builder(this)
                .setTitle("\u05E1\u05DE\u05DF \u05D0\u05EA \u05D4\u05E2\u05DE\u05D5\u05D3\u05D5\u05EA \u05E9\u05D1\u05D4\u05DF \u05DE\u05E1\u05E4\u05E8\u05D9 \u05D4\u05D8\u05DC\u05E4\u05D5\u05DF")
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", (d, w) -> {
                    List<String> chosen = new ArrayList<>();
                    for (int i = 0; i < names.length; i++) if (checked[i]) chosen.add(names[i]);
                    Data.savePhoneColumns(this, chosen);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void testPopup() {
        ClientStore s = Data.get(this);
        if (s.rows.isEmpty()) {
            Toast.makeText(this, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "\u05D1\u05E6\u05E2 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E9\u05DC\u05D1 3 \u05DB\u05D3\u05D9 \u05DC\u05E8\u05D0\u05D5\u05EA \u05D0\u05EA \u05D4\u05D7\u05DC\u05D5\u05E0\u05D9\u05EA", Toast.LENGTH_LONG).show();
        }
        CallPopup.show(this, 0);
    }

    // ------------------------------------------------------------------ helpers

    private Button addButton(LinearLayout root, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
