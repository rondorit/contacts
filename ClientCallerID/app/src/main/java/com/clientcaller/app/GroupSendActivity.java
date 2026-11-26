package com.clientcaller.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * WhatsApp message to a group of clients, one chat after another.
 * WhatsApp doesn't allow sending automatically, so each chat opens with the text ready.
 */
public class GroupSendActivity extends Activity {
    static final String EXTRA_ROWS = "rows";
    static final String EXTRA_TITLE = "title";

    private final List<Integer> rows = new ArrayList<>();
    private final List<CheckBox> boxes = new ArrayList<>();
    private final List<Integer> queue = new ArrayList<>();
    private List<Templates.Tpl> tpls;
    private EditText message;
    private TextView progress;
    private Button next;
    private int pos = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClientStore s = Data.get(this);
        int[] arr = getIntent().getIntArrayExtra(EXTRA_ROWS);
        if (arr != null) for (int r : arr) if (r >= 0 && r < s.rows.size()) rows.add(r);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        setTitle(title == null ? "\u05E9\u05DC\u05D9\u05D7\u05D4 \u05E7\u05D1\u05D5\u05E6\u05EA\u05D9\u05EA \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : title);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = new TextView(this);
        head.setText(title == null ? "\u05E9\u05DC\u05D9\u05D7\u05D4 \u05E7\u05D1\u05D5\u05E6\u05EA\u05D9\u05EA \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : title);
        head.setTextSize(20);
        head.setTypeface(null, Typeface.BOLD);
        box.addView(head);

        label(box, "\u05E0\u05DE\u05E2\u05E0\u05D9\u05DD (" + rows.size() + ")");
        for (int r : rows) {
            CheckBox cb = new CheckBox(this);
            String ph = s.firstPhone(r);
            cb.setText(s.name(r) + (ph.isEmpty() ? "  (\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF)" : "   " + ph));
            cb.setChecked(!ph.isEmpty());
            cb.setEnabled(!ph.isEmpty());
            box.addView(cb);
            boxes.add(cb);
        }

        label(box, "\u05EA\u05D1\u05E0\u05D9\u05EA");
        Spinner sp = new Spinner(this);
        tpls = Templates.list(this);
        List<String> names = new ArrayList<>();
        names.add("\u05D4\u05D5\u05D3\u05E2\u05D4 \u05D7\u05D5\u05E4\u05E9\u05D9\u05EA");
        for (Templates.Tpl t : tpls) names.add(t.name);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        box.addView(sp);

        label(box, "\u05D4\u05D4\u05D5\u05D3\u05E2\u05D4 (\u05E9\u05D3\u05D5\u05EA \u05DB\u05DE\u05D5 {\u05DC\u05E7\u05D5\u05D7} \u05D9\u05D5\u05D7\u05DC\u05E4\u05D5 \u05DC\u05DB\u05DC \u05E0\u05DE\u05E2\u05DF)");
        message = new EditText(this);
        message.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        message.setMinLines(5);
        box.addView(message);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) message.setText(tpls.get(position - 1).body);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Button start = new Button(this);
        start.setAllCaps(false);
        start.setTextSize(18);
        start.setText("\u05D4\u05EA\u05D7\u05DC \u05E9\u05DC\u05D9\u05D7\u05D4");
        start.setOnClickListener(v -> startSending());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        box.addView(start, lp);

        progress = new TextView(this);
        progress.setTextSize(16);
        progress.setPadding(0, dp(12), 0, 0);
        box.addView(progress);

        next = new Button(this);
        next.setAllCaps(false);
        next.setTextSize(18);
        next.setVisibility(View.GONE);
        next.setOnClickListener(v -> openNext());
        box.addView(next);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
    }

    private void startSending() {
        if (message.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "\u05DB\u05EA\u05D5\u05D1 \u05D4\u05D5\u05D3\u05E2\u05D4 \u05D0\u05D5 \u05D1\u05D7\u05E8 \u05EA\u05D1\u05E0\u05D9\u05EA", Toast.LENGTH_SHORT).show();
            return;
        }
        queue.clear();
        for (int i = 0; i < rows.size(); i++) if (boxes.get(i).isChecked()) queue.add(rows.get(i));
        if (queue.isEmpty()) {
            Toast.makeText(this, "\u05DC\u05D0 \u05E0\u05D1\u05D7\u05E8\u05D5 \u05E0\u05DE\u05E2\u05E0\u05D9\u05DD", Toast.LENGTH_SHORT).show();
            return;
        }
        pos = 0;
        openNext();
    }

    private void openNext() {
        if (pos < 0 || pos >= queue.size()) return;
        ClientStore s = Data.get(this);
        int r = queue.get(pos);
        String text = Templates.fillClient(this, r, message.getText().toString());
        String num = DetailActivity.whatsAppNumber(s.firstPhone(r));
        pos++;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + num + "?text=" + Uri.encode(text))));
        } catch (Exception e) {
            Toast.makeText(this, "\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0", Toast.LENGTH_SHORT).show();
        }
        showProgress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showProgress();
    }

    private void showProgress() {
        if (progress == null || pos < 0) return;
        ClientStore s = Data.get(this);
        if (pos >= queue.size()) {
            progress.setText("\u2713 \u05E0\u05E4\u05EA\u05D7\u05D5 \u05DB\u05DC " + queue.size() + " \u05D4\u05E6'\u05D0\u05D8\u05D9\u05DD. \u05D4\u05E9\u05DC\u05D9\u05D7\u05D4 \u05D4\u05E1\u05EA\u05D9\u05D9\u05DE\u05D4.");
            next.setVisibility(View.GONE);
        } else {
            progress.setText("\u05E0\u05E4\u05EA\u05D7\u05D5 " + pos + " \u05DE\u05EA\u05D5\u05DA " + queue.size()
                    + ". \u05DC\u05D7\u05E5 \"\u05E9\u05DC\u05D7\" \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4, \u05D7\u05D6\u05D5\u05E8 \u05DC\u05DB\u05D0\u05DF \u05D5\u05D4\u05DE\u05E9\u05DA:");
            next.setText("\u05D4\u05D1\u05D0: " + s.name(queue.get(pos)));
            next.setVisibility(View.VISIBLE);
        }
    }

    private void label(LinearLayout box, String text) {
        TextView l = new TextView(this);
        l.setText(text);
        l.setTextSize(13);
        l.setTextColor(0xFF777777);
        l.setPadding(0, dp(14), 0, 0);
        box.addView(l);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
