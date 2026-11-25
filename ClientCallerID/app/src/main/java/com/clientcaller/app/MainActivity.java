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

    private TextView status;
    private Button bImport, bPerms, bOverlay, bRole, bCalendar, bUnknown, bTest;
    private EditText search;
    private ArrayAdapter<String> adapter;
    private final List<Integer> shown = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Client Caller ID");
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        status = new TextView(this);
        status.setPadding(0, dp(4), 0, dp(8));
        root.addView(status);

        bImport = addButton(root, v -> pickFile());
        bPerms = addButton(root, v -> askPermissions());
        bOverlay = addButton(root, v -> startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        bRole = addButton(root, v -> askCallerIdRole());
        bCalendar = addButton(root, v -> chooseCalendar());
        bUnknown = addButton(root, v -> {
            SharedPreferences sp = CallLogger.prefs(this);
            sp.edit().putBoolean(CallLogger.KEY_UNKNOWN, !sp.getBoolean(CallLogger.KEY_UNKNOWN, false)).apply();
            refresh();
        });
        bTest = addButton(root, v -> testPopup());

        search = new EditText(this);
        search.setHint("Search name or phone");
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refreshList(); }
        });
        root.addView(search);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, pos, id) -> openCard(shown.get(pos)));
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        ClientStore s = Data.get(this);
        int n = s.rows.size();
        if (n == 0) {
            status.setText("No clients yet. Start with step 1.");
        } else {
            StringBuilder cols = new StringBuilder();
            for (int c : s.phoneCols) {
                if (cols.length() > 0) cols.append(", ");
                cols.append(s.headers.get(c));
            }
            status.setText(n + " clients loaded.\nName column: " + s.headers.get(s.nameCol)
                    + "\nPhone column(s): " + (cols.length() == 0 ? "NONE FOUND - check the file" : cols));
        }
        bImport.setText((n > 0 ? "? " : "1. ") + "Import Excel file" + (n > 0 ? " (tap to update)" : ""));
        bPerms.setText((hasPermissions() ? "? " : "2. ") + "Allow phone, calendar & notification access");
        bOverlay.setText((Settings.canDrawOverlays(this) ? "? " : "3. ") + "Allow showing over other apps");
        if (Build.VERSION.SDK_INT >= 29) {
            RoleManager rm = getSystemService(RoleManager.class);
            boolean held = rm != null && rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
            bRole.setText((held ? "? " : "4. ") + "Set as Caller ID app");
        } else {
            bRole.setVisibility(View.GONE);
        }
        SharedPreferences sp = CallLogger.prefs(this);
        long calId = sp.getLong(CallLogger.KEY_CAL_ID, 0);
        String calText = calId < 0 ? "OFF"
                : calId == 0 ? "main calendar" : sp.getString(CallLogger.KEY_CAL_NAME, "chosen calendar");
        bCalendar.setText("5. Log calls in calendar: " + calText);
        bUnknown.setText("Also log calls from unknown numbers: "
                + (sp.getBoolean(CallLogger.KEY_UNKNOWN, false) ? "ON" : "OFF"));
        bTest.setText("Test: show popup for first client");
        refreshList();
    }

    private void refreshList() {
        ClientStore s = Data.get(this);
        List<Integer> rows = s.search(search.getText().toString());
        shown.clear();
        List<String> items = new ArrayList<>();
        for (int r : rows) {
            if (shown.size() >= 500) break;
            shown.add(r);
            String phone = s.firstPhone(r);
            items.add(s.name(r) + (phone.isEmpty() ? "" : "    " + phone));
        }
        adapter.clear();
        adapter.addAll(items);
    }

    private void openCard(int row) {
        startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_ROW, row));
    }

    // ------------------------------------------------------------------ import

    private void pickFile() {
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
            Toast.makeText(this, "Reading file?", Toast.LENGTH_SHORT).show();
            new Thread(() -> {
                try {
                    byte[] bytes;
                    try (InputStream in = getContentResolver().openInputStream(uri)) {
                        if (in == null) throw new IOException("Cannot open the file");
                        bytes = ClientStore.readAll(in);
                    }
                    ClientStore s = new ClientStore();
                    s.load(ClientStore.readTable(bytes));
                    if (s.rows.isEmpty()) throw new IOException("No client rows found in the file");
                    s.saveFile(new File(getFilesDir(), Data.FILE));
                    Data.set(s);
                    final int n = s.rows.size();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Imported " + n + " clients", Toast.LENGTH_LONG).show();
                        refresh();
                    });
                } catch (Exception e) {
                    final String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                    runOnUiThread(() -> Toast.makeText(this, "Could not read file: " + msg, Toast.LENGTH_LONG).show());
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
                    Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
        }
        return new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
    }

    private boolean hasPermissions() {
        for (String perm : neededPermissions()) {
            if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    private void askPermissions() {
        if (hasPermissions()) {
            Toast.makeText(this, "Already allowed", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Open Permissions and allow Phone, Call logs and Notifications",
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
            Toast.makeText(this, "Not available on this phone - steps 2 and 3 are enough", Toast.LENGTH_LONG).show();
            return;
        }
        if (rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "Already set", Toast.LENGTH_SHORT).show();
            return;
        }
        startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING), REQ_ROLE);
    }

    private void chooseCalendar() {
        if (!CallLogger.hasPermissions(this)) {
            Toast.makeText(this, "Do step 2 first (allow calendar access)", Toast.LENGTH_LONG).show();
            return;
        }
        final List<Long> ids = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        ids.add(-1L);
        labels.add("Off - don't log calls");
        ids.add(0L);
        labels.add("Automatic - main calendar");
        CallLogger.listCalendars(this, ids, labels);
        new AlertDialog.Builder(this)
                .setTitle("Calendar for call log")
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    CallLogger.prefs(this).edit()
                            .putLong(CallLogger.KEY_CAL_ID, ids.get(which))
                            .putString(CallLogger.KEY_CAL_NAME, labels.get(which))
                            .apply();
                    refresh();
                })
                .show();
    }

    private void testPopup() {
        ClientStore s = Data.get(this);
        if (s.rows.isEmpty()) {
            Toast.makeText(this, "Import the Excel file first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Do step 3 first to see the popup over other apps", Toast.LENGTH_LONG).show();
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
