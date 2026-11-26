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
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;
    private static final int REQ_ROLE = 2;
    private static final int REQ_PERMS = 3;
    private static final int REQ_BACKUP = 4;
    private static final int REQ_RESTORE = 5;
    private static final int REQ_EXPORT = 6;

    private TextView status;
    private Button bAsk, bContacts, bBackup, bRestore, bImport, bNameCol, bCols, bPerms, bOverlay, bRole, bCalendar, bUnknown, bTest, bTickets, bOverdue;
    private EditText search;
    private LinearLayout settingsBox, listBox;
    private final List<Integer> shown = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("\u05D6\u05D9\u05D4\u05D5\u05D9 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05E9\u05D9\u05E8\u05D5\u05EA");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(4), 0, dp(8));
        root.addView(status);

        // Service tickets
        LinearLayout ticketRow = new LinearLayout(this);
        bTickets = new Button(this);
        bTickets.setAllCaps(false);
        bTickets.setTextSize(17);
        bTickets.setOnClickListener(v -> startActivity(new Intent(this, TicketsActivity.class)));
        bOverdue = new Button(this);
        bOverdue.setAllCaps(false);
        bOverdue.setTextSize(17);
        bOverdue.setOnClickListener(v -> startActivity(new Intent(this, TicketsActivity.class)
                .putExtra(TicketsActivity.EXTRA_FILTER, TicketsActivity.OVERDUE)));
        ticketRow.addView(bTickets, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 3f));
        ticketRow.addView(bOverdue, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        root.addView(ticketRow);

        // Templates and lists
        LinearLayout toolsRow = new LinearLayout(this);
        String[] toolNames = {"\u05D3\u05D5\u05D7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA", "\u05EA\u05D1\u05E0\u05D9\u05D5\u05EA \u05D4\u05D5\u05D3\u05E2\u05D4", "\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA", "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D5\u05EA"};
        Class<?>[] toolScreens = {ReportActivity.class, TemplatesActivity.class, ProvidersActivity.class, CategoriesActivity.class};
        for (int i = 0; i < toolNames.length; i++) {
            final Class<?> screen = toolScreens[i];
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setText(toolNames[i]);
            b.setOnClickListener(v -> startActivity(new Intent(this, screen)));
            toolsRow.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(toolsRow);

        // Client data: add, import, export
        section(root, "\u05E0\u05D9\u05D4\u05D5\u05DC \u05E0\u05EA\u05D5\u05E0\u05D9 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA");
        LinearLayout clientRow = new LinearLayout(this);
        Button bAdd = new Button(this);
        bAdd.setAllCaps(false);
        bAdd.setText("+ \u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9");
        bAdd.setOnClickListener(v -> startActivity(new Intent(this, ClientEditActivity.class)));
        bImport = new Button(this);
        bImport.setAllCaps(false);
        bImport.setOnClickListener(v -> pickFile());
        Button bExport = new Button(this);
        bExport.setAllCaps(false);
        bExport.setText("\u05D9\u05D9\u05E6\u05D5\u05D0 \u05DC\u05D0\u05E7\u05E1\u05DC");
        bExport.setOnClickListener(v -> startExport());
        clientRow.addView(bAdd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        clientRow.addView(bImport, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        clientRow.addView(bExport, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(clientRow);
        TextView editHint = new TextView(this);
        editHint.setText("\u05DC\u05E2\u05E8\u05D9\u05DB\u05EA \u05DC\u05E7\u05D5\u05D7: \u05DC\u05D7\u05E5 \u05E2\u05DC\u05D9\u05D5 \u05D1\u05E8\u05E9\u05D9\u05DE\u05D4 \u05D5\u05D0\u05D6 \"\u05E2\u05E8\u05D9\u05DB\u05EA \u05E4\u05E8\u05D8\u05D9 \u05D4\u05DC\u05E7\u05D5\u05D7\", \u05D0\u05D5 \u05DC\u05D7\u05D9\u05E6\u05D4 \u05D0\u05E8\u05D5\u05DB\u05D4 \u05E2\u05DC\u05D9\u05D5.");
        editHint.setTextSize(12);
        editHint.setTextColor(0xFF777777);
        root.addView(editHint);

        // Settings (collapsible)
        final Button bSettings = new Button(this);
        bSettings.setAllCaps(false);
        root.addView(bSettings);
        settingsBox = new LinearLayout(this);
        settingsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(settingsBox);
        bSettings.setOnClickListener(v -> {
            boolean show = settingsBox.getVisibility() != android.view.View.VISIBLE;
            settingsBox.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
            bSettings.setText(show ? "\u25B2 \u05D4\u05E1\u05EA\u05E8 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05D5\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA" : "\u2699 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA, \u05D4\u05E8\u05E9\u05D0\u05D5\u05EA \u05D5\u05D2\u05D9\u05D1\u05D5\u05D9");
        });
        bNameCol = addButton(settingsBox, v -> chooseNameColumn());
        bCols = addButton(settingsBox, v -> choosePhoneColumns());
        bPerms = addButton(settingsBox, v -> askPermissions());
        bOverlay = addButton(settingsBox, v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        bRole = addButton(settingsBox, v -> askCallerIdRole());
        bCalendar = addButton(settingsBox, v -> chooseCalendar());
        bAsk = addButton(settingsBox, v -> {
            SharedPreferences sp = CallLogger.prefs(this);
            sp.edit().putBoolean(CallLogger.KEY_ASK, !sp.getBoolean(CallLogger.KEY_ASK, true)).apply();
            refresh();
        });
        bUnknown = addButton(settingsBox, v -> {
            SharedPreferences sp = CallLogger.prefs(this);
            sp.edit().putBoolean(CallLogger.KEY_UNKNOWN, !sp.getBoolean(CallLogger.KEY_UNKNOWN, false)).apply();
            refresh();
        });
        bContacts = addButton(settingsBox, v -> {
            SharedPreferences sp = CallLogger.prefs(this);
            sp.edit().putBoolean(PhoneContacts.KEY_ENABLED, !sp.getBoolean(PhoneContacts.KEY_ENABLED, true)).apply();
            refresh();
        });
        bTest = addButton(settingsBox, v -> testPopup());
        LinearLayout bkRow = new LinearLayout(this);
        bBackup = new Button(this);
        bBackup.setAllCaps(false);
        bBackup.setOnClickListener(v -> startBackup());
        bRestore = new Button(this);
        bRestore.setAllCaps(false);
        bRestore.setText("\u05E9\u05D7\u05D6\u05D5\u05E8 \u05DE\u05D2\u05D9\u05D1\u05D5\u05D9");
        bRestore.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("\u05E9\u05D7\u05D6\u05D5\u05E8 \u05DE\u05D2\u05D9\u05D1\u05D5\u05D9")
                .setMessage("\u05D4\u05E9\u05D7\u05D6\u05D5\u05E8 \u05D9\u05D7\u05DC\u05D9\u05E3 \u05D0\u05EA \u05DB\u05DC \u05D4\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA, \u05E0\u05D5\u05EA\u05E0\u05D9 \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA, \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05D5\u05D4\u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05E9\u05D1\u05D8\u05DC\u05E4\u05D5\u05DF \u05D1\u05EA\u05D5\u05DB\u05DF \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D2\u05D9\u05D1\u05D5\u05D9. \u05DC\u05D4\u05DE\u05E9\u05D9\u05DA?")
                .setPositiveButton("\u05D1\u05D7\u05E8 \u05E7\u05D5\u05D1\u05E5 \u05D2\u05D9\u05D1\u05D5\u05D9", (d, w) -> {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(i, REQ_RESTORE);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show());
        bkRow.addView(bBackup, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bkRow.addView(bRestore, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        settingsBox.addView(bkRow);
        boolean setupDone = !Data.get(this).rows.isEmpty() && hasPermissions() && Settings.canDrawOverlays(this);
        settingsBox.setVisibility(setupDone ? android.view.View.GONE : android.view.View.VISIBLE);
        bSettings.setText(setupDone ? "\u2699 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA, \u05D4\u05E8\u05E9\u05D0\u05D5\u05EA \u05D5\u05D2\u05D9\u05D1\u05D5\u05D9" : "\u25B2 \u05D4\u05E1\u05EA\u05E8 \u05D4\u05D2\u05D3\u05E8\u05D5\u05EA \u05D5\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA");

        // Clients list
        section(root, "\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA");
        search = new EditText(this);
        search.setHint("\u05D7\u05D9\u05E4\u05D5\u05E9 \u05DC\u05E4\u05D9 \u05E9\u05DD \u05D0\u05D5 \u05D8\u05DC\u05E4\u05D5\u05DF");
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refreshList(); }
        });
        root.addView(search);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        android.widget.ScrollView sv = new android.widget.ScrollView(this);
        sv.addView(root);
        setContentView(sv);
        He.rtl(this);
        Backup.scheduleReminder(this);
        if (getIntent().getBooleanExtra(Backup.EXTRA_BACKUP, false)) startBackup();
    }

    private void section(LinearLayout root, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(0xFF1E88E5);
        t.setPadding(0, dp(14), 0, dp(2));
        root.addView(t);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        int openT = 0, lateT = 0;
        for (TicketDb.Ticket t : TicketDb.get(this).all()) {
            if (t.isOpen()) openT++;
            if (t.isOverdue()) lateT++;
        }
        bTickets.setText("\uD83C\uDFAB \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA (" + openT + " \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA)");
        bOverdue.setText("\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8: " + lateT);
        bOverdue.setTextColor(lateT > 0 ? 0xFFD32F2F : 0xFF444444);
        ClientStore s = Data.get(this);
        int n = s.rows.size();
        if (n == 0) {
            status.setText("\u05E2\u05D3\u05D9\u05D9\u05DF \u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA. \u05D4\u05EA\u05D7\u05DC \u05D1\u05E9\u05DC\u05D1 1.");
        } else {
            StringBuilder cols = new StringBuilder();
            for (int c : s.phoneCols) {
                if (cols.length() > 0) cols.append(", ");
                cols.append(s.headers.get(c));
            }
            status.setText(n + " \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E0\u05D8\u05E2\u05E0\u05D5.\n\u05E2\u05DE\u05D5\u05D3\u05EA \u05E9\u05DD: " + s.headers.get(s.nameCol)
                    + "\n\u05E2\u05DE\u05D5\u05D3\u05D5\u05EA \u05D8\u05DC\u05E4\u05D5\u05DF: " + (cols.length() == 0 ? "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5 - \u05D1\u05D3\u05D5\u05E7 \u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5" : cols));
        }
        bImport.setText("\u05D9\u05D9\u05D1\u05D5\u05D0 \u05D0\u05E7\u05E1\u05DC / Access");
        bNameCol.setText("\u05D1\u05D7\u05D9\u05E8\u05EA \u05E2\u05DE\u05D5\u05D3\u05EA \u05E9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7");
        bCols.setText("\u05D1\u05D7\u05D9\u05E8\u05EA \u05E2\u05DE\u05D5\u05D3\u05D5\u05EA \u05D8\u05DC\u05E4\u05D5\u05DF (\u05D0\u05DD \u05DE\u05E1\u05E4\u05E8\u05D9\u05DD \u05DC\u05D0 \u05DE\u05D6\u05D5\u05D4\u05D9\u05DD)");
        bPerms.setText((hasPermissions() ? "\u2713 " : "2. ") + "\u05D4\u05E8\u05E9\u05D0\u05D5\u05EA \u05D8\u05DC\u05E4\u05D5\u05DF, \u05D9\u05D5\u05DE\u05DF, \u05D0\u05E0\u05E9\u05D9 \u05E7\u05E9\u05E8 \u05D5\u05D4\u05EA\u05E8\u05D0\u05D5\u05EA");
        bOverlay.setText((Settings.canDrawOverlays(this) ? "\u2713 " : "3. ") + "\u05D4\u05E6\u05D2\u05D4 \u05DE\u05E2\u05DC \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D5\u05EA \u05D0\u05D7\u05E8\u05D5\u05EA");
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            boolean held = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
            bRole.setText((held ? "\u2713 " : "4. ") + "\u05D4\u05D2\u05D3\u05E8\u05D4 \u05DB\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D9\u05EA \u05D6\u05D9\u05D4\u05D5\u05D9 \u05DE\u05EA\u05E7\u05E9\u05E8");
        } else {
            bRole.setVisibility(View.GONE);
        }
        SharedPreferences sp = CallLogger.prefs(this);
        long calId = sp.getLong(CallLogger.KEY_CAL_ID, 0);
        String calText = calId < 0 ? "\u05DB\u05D1\u05D5\u05D9"
                : calId == 0 ? "\u05D9\u05D5\u05DE\u05DF \u05E8\u05D0\u05E9\u05D9" : sp.getString(CallLogger.KEY_CAL_NAME, "\u05D9\u05D5\u05DE\u05DF \u05E9\u05E0\u05D1\u05D7\u05E8");
        bCalendar.setText("5. \u05E8\u05D9\u05E9\u05D5\u05DD \u05E9\u05D9\u05D7\u05D5\u05EA \u05D1\u05D9\u05D5\u05DE\u05DF: " + calText);
        bAsk.setText("\u05E9\u05DE\u05D9\u05E8\u05EA \u05E9\u05D9\u05D7\u05D4 \u05D1\u05D9\u05D5\u05DE\u05DF: " + (CallLogger.prefs(this).getBoolean(CallLogger.KEY_ASK, true)
                ? "\u05DC\u05E9\u05D0\u05D5\u05DC \u05D0\u05D7\u05E8\u05D9 \u05DB\u05DC \u05E9\u05D9\u05D7\u05D4" : "\u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9\u05EA, \u05D1\u05DC\u05D9 \u05DC\u05E9\u05D0\u05D5\u05DC"));
        bUnknown.setText("\u05E8\u05D9\u05E9\u05D5\u05DD \u05D2\u05DD \u05E9\u05DC \u05DE\u05E1\u05E4\u05E8\u05D9\u05DD \u05DC\u05D0 \u05DE\u05D5\u05DB\u05E8\u05D9\u05DD: "
                + (sp.getBoolean(CallLogger.KEY_UNKNOWN, false) ? "\u05E4\u05E2\u05D9\u05DC" : "\u05DB\u05D1\u05D5\u05D9"));
        bContacts.setText("\u05D6\u05D9\u05D4\u05D5\u05D9 \u05D2\u05DD \u05DE\u05D0\u05E0\u05E9\u05D9 \u05D4\u05E7\u05E9\u05E8 \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF: "
                + (CallLogger.prefs(this).getBoolean(PhoneContacts.KEY_ENABLED, true) ? "\u05E4\u05E2\u05D9\u05DC" : "\u05DB\u05D1\u05D5\u05D9"));
        long lastBk = CallLogger.prefs(this).getLong(Backup.KEY_LAST, 0);
        bBackup.setText("\u05D2\u05D9\u05D1\u05D5\u05D9 \u05E0\u05EA\u05D5\u05E0\u05D9\u05DD" + (lastBk == 0 ? " (\u05DC\u05D0 \u05D1\u05D5\u05E6\u05E2)" : "\n(\u05D0\u05D7\u05E8\u05D5\u05DF: " + TicketDb.fmt(lastBk).substring(0, 10) + ")"));
        bTest.setText("\u05D1\u05D3\u05D9\u05E7\u05D4: \u05D4\u05E6\u05D2 \u05D7\u05DC\u05D5\u05E0\u05D9\u05EA \u05DC\u05DC\u05E7\u05D5\u05D7 \u05D4\u05E8\u05D0\u05E9\u05D5\u05DF");
        refreshList();
    }

    private void refreshList() {
        ClientStore s = Data.get(this);
        List<Integer> rows = s.search(search.getText().toString());
        listBox.removeAllViews();
        int count = 0;
        for (final int r : rows) {
            if (count >= 300) {
                TextView more = new TextView(this);
                more.setText("\u05DE\u05D5\u05E6\u05D2\u05D9\u05DD 300 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E8\u05D0\u05E9\u05D5\u05E0\u05D9\u05DD - \u05D7\u05E4\u05E9 \u05DB\u05D3\u05D9 \u05DC\u05E6\u05DE\u05E6\u05DD (" + rows.size() + " \u05D1\u05E1\u05DA \u05D4\u05DB\u05D5\u05DC)");
                more.setPadding(0, dp(10), 0, dp(10));
                listBox.addView(more);
                break;
            }
            count++;
            TextView tv = new TextView(this);
            String phone = s.firstPhone(r);
            tv.setText(s.name(r) + (phone.isEmpty() ? "" : "\n" + phone));
            tv.setTextSize(17);
            tv.setPadding(dp(4), dp(12), dp(4), dp(12));
            tv.setOnClickListener(v -> openCard(r));
            tv.setOnLongClickListener(v -> {
                startActivity(new Intent(this, ClientEditActivity.class).putExtra(DetailActivity.EXTRA_ROW, r));
                return true;
            });
            listBox.addView(tv);
            android.view.View line = new android.view.View(this);
            line.setBackgroundColor(0xFFDDDDDD);
            listBox.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1))));
        }
        if (rows.isEmpty()) {
            TextView none = new TextView(this);
            none.setText(s.rows.isEmpty() ? "\u05D0\u05D9\u05DF \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA - \u05DC\u05D7\u05E5 \"\u05D9\u05D9\u05D1\u05D5\u05D0\" \u05D0\u05D5 \"+ \u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9\"" : "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D5 \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA");
            none.setPadding(0, dp(10), 0, dp(10));
            listBox.addView(none);
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
                    CallLogger.prefs(this).edit().putString("access_table", name).apply();
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
                        final File f = new File(getCacheDir(), "import.accdb");
                        try (java.io.FileOutputStream o = new java.io.FileOutputStream(f)) {
                            o.write(bytes);
                        }
                        final List<String> tables = AccessImport.tables(f);
                        if (tables.isEmpty()) throw new IOException("\u05D0\u05D9\u05DF \u05D8\u05D1\u05DC\u05D0\u05D5\u05EA \u05D1\u05E7\u05D5\u05D1\u05E5 \u05D4-Access");
                        if (tables.size() == 1) finishImport(AccessImport.read(f, tables.get(0)));
                        else runOnUiThread(() -> chooseAccessTable(f, tables));
                    } else {
                        finishImport(ClientStore.readTable(bytes));
                    }
                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, "\u05E9\u05D2\u05D9\u05D0\u05D4 \u05D1\u05E7\u05E8\u05D9\u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5: " + msg, Toast.LENGTH_LONG).show());
                }
            }).start();
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

    private void startBackup() {
        String date = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_TITLE, "backup-service-" + date + ".zip");
        Toast.makeText(this, "\u05D1\u05D7\u05E8 \u05D4\u05D9\u05DB\u05DF \u05DC\u05E9\u05DE\u05D5\u05E8 - \u05DE\u05D5\u05DE\u05DC\u05E5 Google Drive", Toast.LENGTH_LONG).show();
        startActivityForResult(i, REQ_BACKUP);
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
