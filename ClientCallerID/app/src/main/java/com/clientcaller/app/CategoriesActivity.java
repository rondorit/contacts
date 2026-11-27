package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
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

/** Add, rename, reorder and delete fault categories. */
public class CategoriesActivity extends Activity {
    private ArrayAdapter<String> adapter;
    private List<String> items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05E0\u05D9\u05D4\u05D5\u05DC \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D5\u05EA");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);
        root.addView(Theme.header(this, "\u05E0\u05D9\u05D4\u05D5\u05DC \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D5\u05EA"));

        TextView hint = Theme.body(this, "", Theme.TEXT_SECONDARY);
        hint.setText("\u05DC\u05D7\u05E5 \u05E2\u05DC \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 \u05DB\u05D3\u05D9 \u05DC\u05E9\u05E0\u05D5\u05EA \u05E9\u05DD, \u05DC\u05D4\u05D6\u05D9\u05D6 \u05D0\u05D5 \u05DC\u05DE\u05D7\u05D5\u05E7.");
        root.addView(hint);

        Button add = new Button(this);
        add.setAllCaps(false);
        add.setTextSize(18);
        add.setText("+ \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 \u05D7\u05D3\u05E9\u05D4");
        add.setOnClickListener(v -> edit(-1));
        root.addView(add);

        ListView lv = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<>()) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
                android.view.View v = super.getView(position, convertView, parent);
                v.setPadding(dp(16), dp(16), dp(16), dp(16));
                v.setBackground(Theme.cardBg(getContext()));
                v.setTextAlignment(android.view.View.TEXT_ALIGNMENT_VIEW_START);
                TextView rowText = (TextView) v;
                rowText.setTextColor(Theme.TEXT_PRIMARY);
                rowText.setTextSize(Theme.BODY);
                rowText.setTypeface(Theme.typeface(0));
                rowText.setLetterSpacing(0f);
                rowText.setLineSpacing(dp(4), 1f);
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
        items = Categories.list(this);
        adapter.clear();
        adapter.addAll(items);
    }

    private void edit(final int pos) {
        final EditText name = new EditText(this);
        if (pos >= 0) name.setText(items.get(pos));
        name.setHint("\u05E9\u05DD \u05D4\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4");
        LinearLayout box = new LinearLayout(this);
        int p = dp(16);
        box.setPadding(p, p, p, 0);
        box.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(pos < 0 ? "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 \u05D7\u05D3\u05E9\u05D4" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4")
                .setView(box)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    String n = name.getText().toString().trim().replace("\t", " ");
                    if (n.isEmpty()) return;
                    if (items.contains(n) && (pos < 0 || !items.get(pos).equals(n))) {
                        Toast.makeText(this, "\u05D4\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 \u05DB\u05D1\u05E8 \u05E7\u05D9\u05D9\u05DE\u05EA", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pos < 0) items.add(n); else items.set(pos, n);
                    Categories.save(this, items);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (pos >= 0) {
            b.setNeutralButton("\u05E2\u05D5\u05D3\u2026", (d, w) -> more(pos));
        }
        b.show();
    }

    private void more(final int pos) {
        String[] opts = {"\u05D4\u05D6\u05D6 \u05DC\u05DE\u05E2\u05DC\u05D4", "\u05D4\u05D6\u05D6 \u05DC\u05DE\u05D8\u05D4", "\u05DE\u05D7\u05E7"};
        new AlertDialog.Builder(this)
                .setTitle(items.get(pos))
                .setItems(opts, (d, w) -> {
                    if (w == 0 && pos > 0) {
                        items.add(pos - 1, items.remove(pos));
                    } else if (w == 1 && pos < items.size() - 1) {
                        items.add(pos + 1, items.remove(pos));
                    } else if (w == 2) {
                        if (items.size() <= 1) return;
                        Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05D4\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 " + items.get(pos), () -> {
                            items.remove(pos);
                            Categories.save(this, items);
                            refresh();
                        });
                        return;
                    } else {
                        return;
                    }
                    Categories.save(this, items);
                    refresh();
                })
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
