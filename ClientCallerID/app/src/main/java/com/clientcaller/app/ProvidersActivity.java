package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Outsourcing service providers: name, phone (WhatsApp) and email. */
public class ProvidersActivity extends Activity {
    private ArrayAdapter<String> adapter;
    private List<TicketDb.Provider> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA - \u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);

        TextView hint = new TextView(this);
        hint.setText("\u05DC\u05D7\u05E5 \u05E2\u05DC \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA \u05DB\u05D3\u05D9 \u05DC\u05E2\u05D3\u05DB\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF (\u05DC\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4) \u05D5\u05DE\u05D9\u05D9\u05DC.");
        root.addView(hint);

        Button add = new Button(this);
        add.setAllCaps(false);
        add.setText("+ \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA \u05D7\u05D3\u05E9");
        add.setOnClickListener(v -> edit(new TicketDb.Provider()));
        root.addView(add);

        ListView lv = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                v.setPadding(dp(14), dp(14), dp(14), dp(14));
                v.setBackground(Ui.rounded(0xFFFFFFFF, 12, getContext()));
                v.setTextAlignment(android.view.View.TEXT_ALIGNMENT_VIEW_START);
                return v;
            }
        };
        lv.setDivider(new android.graphics.drawable.ColorDrawable(0x00000000));
        lv.setDividerHeight(dp(8));
        lv.setPadding(0, dp(8), 0, dp(8));
        lv.setClipToPadding(false);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, pos, id) -> edit(list.get(pos)));
        root.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        He.rtl(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        list = TicketDb.get(this).providers();
        List<String> items = new ArrayList<>();
        for (TicketDb.Provider pr : list) {
            StringBuilder sb = new StringBuilder(pr.name);
            sb.append("\n").append(pr.phone.isEmpty() ? "\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF" : "\u05D8\u05DC\u05E4\u05D5\u05DF: " + pr.phone);
            sb.append("\n").append(pr.email.isEmpty() ? "\u05D0\u05D9\u05DF \u05DE\u05D9\u05D9\u05DC" : "\u05DE\u05D9\u05D9\u05DC: " + pr.email);
            items.add(sb.toString());
        }
        adapter.clear();
        adapter.addAll(items);
    }

    private void edit(final TicketDb.Provider pr) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, 0);
        final EditText name = field(box, "\u05E9\u05DD", pr.name, InputType.TYPE_CLASS_TEXT);
        final EditText phone = field(box, "\u05D8\u05DC\u05E4\u05D5\u05DF (\u05DC\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4)", pr.phone, InputType.TYPE_CLASS_PHONE);
        final EditText email = field(box, "\u05DE\u05D9\u05D9\u05DC", pr.email,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(pr.id == 0 ? "\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA")
                .setView(box)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        Toast.makeText(this, "\u05D7\u05E1\u05E8 \u05E9\u05DD", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pr.name = n;
                    pr.phone = phone.getText().toString().trim();
                    pr.email = email.getText().toString().trim();
                    TicketDb.get(this).saveProvider(pr);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (pr.id > 0) {
            b.setNeutralButton("\u05DE\u05D7\u05E7", (d, w) -> new AlertDialog.Builder(this)
                    .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA " + pr.name + "?")
                    .setPositiveButton("\u05DE\u05D7\u05E7", (d2, w2) -> Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA " + pr.name, () -> {
                        TicketDb.get(this).deleteProvider(pr.id);
                        refresh();
                    }))
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show());
        }
        b.show();
    }

    private EditText field(LinearLayout box, String label, String value, int type) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13);
        l.setTextColor(0xFF777777);
        box.addView(l);
        EditText e = new EditText(this);
        e.setInputType(type);
        e.setText(value);
        box.addView(e);
        return e;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
