package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Add a new client or edit an existing one; one field per Excel column. */
public class ClientEditActivity extends Activity {
    private final List<EditText> fields = new ArrayList<>();
    private int row;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ClientStore s = Data.get(this);
        if (s.headers.isEmpty()) {
            Toast.makeText(this, "\u05D9\u05D9\u05D1\u05D0 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05E7\u05D5\u05D1\u05E5 \u05D4\u05D0\u05E7\u05E1\u05DC", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        row = getIntent().getIntExtra(DetailActivity.EXTRA_ROW, -1);
        if (row >= s.rows.size()) row = -1;
        setTitle(row < 0 ? "\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05DC\u05E7\u05D5\u05D7");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = new TextView(this);
        head.setText(row < 0 ? "\u05DC\u05E7\u05D5\u05D7 \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA " + s.name(row));
        head.setTextSize(22);
        head.setTypeface(null, Typeface.BOLD);
        box.addView(head);

        for (int c = 0; c < s.headers.size(); c++) {
            TextView l = new TextView(this);
            l.setText(s.headers.get(c));
            l.setTextSize(13);
            l.setTextColor(0xFF777777);
            l.setPadding(0, dp(12), 0, 0);
            box.addView(l);
            EditText e = new EditText(this);
            boolean phone = s.phoneCols.contains(c);
            e.setInputType(phone ? InputType.TYPE_CLASS_PHONE
                    : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            if (row >= 0) e.setText(s.rows.get(row).get(c));
            box.addView(e);
            fields.add(e);
        }

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setTextSize(18);
        save.setText("\u05E9\u05DE\u05D5\u05E8 \u05DC\u05E7\u05D5\u05D7");
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        box.addView(save, lp);

        if (row >= 0) {
            Button del = new Button(this);
            del.setAllCaps(false);
            del.setText("\u05DE\u05D7\u05E7 \u05DC\u05E7\u05D5\u05D7");
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA " + s.name(row) + " \u05DE\u05E8\u05E9\u05D9\u05DE\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA?")
                    .setPositiveButton("\u05DE\u05D7\u05E7", (d, w) -> Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA " + s.name(row), () -> {
                        Data.recordDelete(this, Data.identity(s, row));
                        s.removeRow(row);
                        if (persist()) {
                            Toast.makeText(this, "\u05D4\u05DC\u05E7\u05D5\u05D7 \u05E0\u05DE\u05D7\u05E7", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }))
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show());
            box.addView(del);
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(Ui.wrapCard(this, box));
        setContentView(sv);
        He.rtl(this);
    }

    private void save() {
        ClientStore s = Data.get(this);
        List<String> values = new ArrayList<>();
        boolean any = false;
        for (EditText e : fields) {
            String v = e.getText().toString().trim();
            if (!v.isEmpty()) any = true;
            values.add(v);
        }
        if (!any) {
            Toast.makeText(this, "\u05DB\u05DC \u05D4\u05E9\u05D3\u05D5\u05EA \u05E8\u05D9\u05E7\u05D9\u05DD", Toast.LENGTH_SHORT).show();
            return;
        }
        int r = s.setRow(row, values);
        Data.recordEdit(this, Data.identity(s, r));
        if (!persist()) return;
        Toast.makeText(this, "\u05D4\u05DC\u05E7\u05D5\u05D7 \u05E0\u05E9\u05DE\u05E8", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_ROW, r));
        finish();
    }

    private boolean persist() {
        try {
            Data.saveEdits(this);
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "\u05E9\u05D2\u05D9\u05D0\u05D4 \u05D1\u05E9\u05DE\u05D9\u05E8\u05D4: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
