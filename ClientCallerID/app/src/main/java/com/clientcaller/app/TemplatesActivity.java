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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Create and edit message templates for outsourcing providers. */
public class TemplatesActivity extends Activity {
    private ArrayAdapter<String> adapter;
    private List<Templates.Tpl> items = new ArrayList<>();
    private EditText lastFocused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05EA\u05D1\u05E0\u05D9\u05D5\u05EA \u05D4\u05D5\u05D3\u05E2\u05D4 \u05DC\u05E0\u05D5\u05EA\u05E0\u05D9 \u05E9\u05D9\u05E8\u05D5\u05EA");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);

        TextView hint = new TextView(this);
        hint.setText("\u05D1\u05EA\u05D1\u05E0\u05D9\u05EA \u05D0\u05E4\u05E9\u05E8 \u05DC\u05E9\u05D9\u05DD \u05E9\u05D3\u05D5\u05EA \u05DB\u05DE\u05D5 {\u05DC\u05E7\u05D5\u05D7} \u05D0\u05D5 {MAC}, \u05D5\u05D2\u05DD \u05DB\u05DC \u05E2\u05DE\u05D5\u05D3\u05D4 \u05DE\u05D4\u05D0\u05E7\u05E1\u05DC, \u05DC\u05DE\u05E9\u05DC {\u05DB\u05EA\u05D5\u05D1\u05EA}. "
                + "\u05D1\u05E9\u05DC\u05D9\u05D7\u05D4 \u05D4\u05DD \u05D9\u05D5\u05D7\u05DC\u05E4\u05D5 \u05D1\u05E0\u05EA\u05D5\u05E0\u05D9 \u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D5\u05D4\u05DC\u05E7\u05D5\u05D7.");
        root.addView(hint);

        Button add = new Button(this);
        add.setAllCaps(false);
        add.setText("+ \u05EA\u05D1\u05E0\u05D9\u05EA \u05D7\u05D3\u05E9\u05D4");
        add.setOnClickListener(v -> edit(-1));
        root.addView(add);

        ListView lv = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                v.setPadding(dp(14), dp(14), dp(14), dp(14));
                v.setBackground(Ui.rounded(0xFFFFFFFF, 12, getContext()));
                return v;
            }
        };
        lv.setDivider(new android.graphics.drawable.ColorDrawable(0x00000000));
        lv.setDividerHeight(dp(8));
        lv.setPadding(0, dp(8), 0, dp(8));
        lv.setClipToPadding(false);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, pos, id) -> edit(pos));
        root.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        He.rtl(this);
        refresh();
    }

    private void refresh() {
        items = Templates.list(this);
        List<String> labels = new ArrayList<>();
        for (Templates.Tpl t : items) labels.add(t.name + (t.subject.isEmpty() ? "" : "\n" + t.subject));
        adapter.clear();
        adapter.addAll(labels);
    }

    private void edit(final int pos) {
        final Templates.Tpl t = pos >= 0 ? items.get(pos) : new Templates.Tpl();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, 0);
        box.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        final EditText name = field(box, "\u05E9\u05DD \u05D4\u05EA\u05D1\u05E0\u05D9\u05EA (\u05DC\u05DE\u05E9\u05DC: \u05E4\u05EA\u05D9\u05D7\u05EA \u05EA\u05E7\u05DC\u05D4 \u05D1\u05D4\u05D5\u05D8)", t.name, false);
        final EditText subject = field(box, "\u05E0\u05D5\u05E9\u05D0 \u05D4\u05DE\u05D9\u05D9\u05DC", t.subject, false);
        final EditText body = field(box, "\u05EA\u05D5\u05DB\u05DF \u05D4\u05D4\u05D5\u05D3\u05E2\u05D4", t.body, true);
        lastFocused = body;
        subject.setOnFocusChangeListener((v, has) -> { if (has) lastFocused = subject; });
        body.setOnFocusChangeListener((v, has) -> { if (has) lastFocused = body; });

        Button insert = new Button(this);
        insert.setAllCaps(false);
        insert.setText("\u05D4\u05D5\u05E1\u05E3 \u05E9\u05D3\u05D4 \u05DE\u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 / \u05DE\u05D4\u05D0\u05E7\u05E1\u05DC");
        insert.setOnClickListener(v -> {
            final List<String> fields = Templates.allFields(this);
            new AlertDialog.Builder(this)
                    .setTitle("\u05D1\u05D7\u05E8 \u05E9\u05D3\u05D4")
                    .setItems(fields.toArray(new String[0]), (d, w) -> {
                        EditText e = lastFocused == null ? body : lastFocused;
                        int at = Math.max(e.getSelectionStart(), 0);
                        e.getText().insert(at, fields.get(w));
                    })
                    .show();
        });
        box.addView(insert);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(pos < 0 ? "\u05EA\u05D1\u05E0\u05D9\u05EA \u05D7\u05D3\u05E9\u05D4" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05EA\u05D1\u05E0\u05D9\u05EA")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        Toast.makeText(this, "\u05D7\u05E1\u05E8 \u05E9\u05DD \u05DC\u05EA\u05D1\u05E0\u05D9\u05EA", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    t.name = n;
                    t.subject = subject.getText().toString();
                    t.body = body.getText().toString();
                    if (pos < 0) items.add(t);
                    Templates.save(this, items);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (pos >= 0) {
            b.setNeutralButton("\u05DE\u05D7\u05E7", (d, w) -> new AlertDialog.Builder(this)
                    .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA \u05D4\u05EA\u05D1\u05E0\u05D9\u05EA " + t.name + "?")
                    .setPositiveButton("\u05DE\u05D7\u05E7", (d2, w2) -> {
                        items.remove(pos);
                        Templates.save(this, items);
                        refresh();
                    })
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show());
        }
        b.show();
    }

    private EditText field(LinearLayout box, String label, String value, boolean multi) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(13);
        l.setTextColor(0xFF777777);
        box.addView(l);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (multi ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        if (multi) e.setMinLines(6);
        e.setText(value);
        box.addView(e);
        return e;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
