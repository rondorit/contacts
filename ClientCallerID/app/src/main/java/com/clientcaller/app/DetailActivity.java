package com.clientcaller.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Full client card with Call and WhatsApp buttons. */
public class DetailActivity extends Activity {
    static final String EXTRA_ROW = "row";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ClientStore s = Data.get(this);
        int r = getIntent().getIntExtra(EXTRA_ROW, -1);
        if (r < 0 || r >= s.rows.size()) {
            finish();
            return;
        }
        List<String> row = s.rows.get(r);
        setTitle(s.name(r));

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView name = new TextView(this);
        name.setText(s.name(r));
        name.setTextSize(24);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextIsSelectable(true);
        box.addView(name);

        for (int c = 0; c < s.headers.size(); c++) {
            String value = row.get(c);
            if (c == s.nameCol || value.isEmpty()) continue;

            TextView label = new TextView(this);
            label.setText(s.headers.get(c));
            label.setTextSize(13);
            label.setTextColor(0xFF777777);
            label.setPadding(0, dp(12), 0, 0);
            box.addView(label);

            TextView val = new TextView(this);
            val.setText(value);
            val.setTextSize(17);
            val.setTextIsSelectable(true);
            box.addView(val);

            if (s.phoneCols.contains(c)) {
                for (final String number : ClientStore.phoneParts(value)) {
                    LinearLayout line = new LinearLayout(this);
                    line.setOrientation(LinearLayout.HORIZONTAL);
                    Button call = new Button(this);
                    call.setAllCaps(false);
                    call.setText("Call " + number);
                    call.setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + Uri.encode(number)))));
                    Button wa = new Button(this);
                    wa.setAllCaps(false);
                    wa.setText("WhatsApp");
                    wa.setOnClickListener(v -> open(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://wa.me/" + whatsAppNumber(number)))));
                    line.addView(call, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
                    line.addView(wa, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    box.addView(line);
                }
            }
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    /** Israeli local numbers get the 972 country code; others are used as written. */
    private static String whatsAppNumber(String number) {
        String k = ClientStore.key(number);
        String plain = number.replaceAll("[^0-9]", "");
        if (number.trim().startsWith("+") || plain.startsWith("00")) {
            return plain.startsWith("00") ? plain.substring(2) : plain;
        }
        return (k.length() == 8 || k.length() == 9) ? "972" + k : plain;
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "No app found for this action", Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
