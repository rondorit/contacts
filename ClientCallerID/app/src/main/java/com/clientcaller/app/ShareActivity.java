package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Receives text shared from another app (a call transcript from SpeakApp, a WhatsApp message,
 * an email...) and attaches it to a service ticket.
 */
public class ShareActivity extends Activity {
    private String text;
    private EditText search;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent in = getIntent();
        CharSequence body = in.getCharSequenceExtra(Intent.EXTRA_TEXT);
        String subject = in.getStringExtra(Intent.EXTRA_SUBJECT);
        if (body == null || body.toString().trim().isEmpty()) {
            Toast.makeText(this, "\u05DC\u05D0 \u05D4\u05EA\u05E7\u05D1\u05DC \u05D8\u05E7\u05E1\u05D8", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        text = (subject == null || subject.trim().isEmpty() ? "" : subject.trim() + "\n") + body.toString().trim();
        setTitle("\u05E6\u05D9\u05E8\u05D5\u05E3 \u05DC\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 40));

        box.addView(Ui.sectionTitle(this, "\u05D4\u05D8\u05E7\u05E1\u05D8 \u05E9\u05D4\u05EA\u05E7\u05D1\u05DC"));
        LinearLayout preview = Ui.card(this);
        TextView pv = Ui.text(this, text.length() > 600 ? text.substring(0, 600) + "\u2026" : text, 14, Ui.TEXT, false);
        preview.addView(pv);
        box.addView(preview, Ui.cardParams(this));

        box.addView(Ui.rowCard(this, "\uD83D\uDCDD  \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4 \u05E2\u05DD \u05D4\u05D8\u05E7\u05E1\u05D8", "\u05D4\u05D8\u05E7\u05E1\u05D8 \u05D9\u05D9\u05DB\u05E0\u05E1 \u05DC\u05EA\u05D9\u05D0\u05D5\u05E8 \u05D4\u05EA\u05E7\u05DC\u05D4", "", 0, v -> {
            startActivity(new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_DESC, text));
            finish();
        }), Ui.cardParams(this));

        box.addView(Ui.sectionTitle(this, "\u05D0\u05D5 \u05E6\u05D9\u05E8\u05D5\u05E3 \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4 \u05E7\u05D9\u05D9\u05DE\u05EA"));
        search = new EditText(this);
        search.setHint("\uD83D\uDD0D  \u05D7\u05D9\u05E4\u05D5\u05E9 \u05DC\u05E7\u05D5\u05D7 \u05D0\u05D5 \u05E0\u05D5\u05E9\u05D0\u2026");
        search.setSingleLine(true);
        search.setBackground(Ui.rounded(0xFFFFFFFF, 12, this));
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refresh(); }
        });
        box.addView(search, Ui.cardParams(this));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        box.addView(list);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<TicketDb.Ticket> open = new ArrayList<>(), closed = new ArrayList<>();
        for (TicketDb.Ticket t : TicketDb.get(this).all()) {
            String hay = (t.clientName + " " + t.clientPhone + " " + t.title + " #" + t.id).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !hay.contains(q)) continue;
            (t.isOpen() ? open : closed).add(t);
        }
        List<TicketDb.Ticket> all = new ArrayList<>(open);
        all.addAll(closed);
        int n = 0;
        for (final TicketDb.Ticket t : all) {
            if (n++ >= 50) break;
            String sub = "#" + t.id + "  \u2022  " + (t.clientName.isEmpty() ? "\u05DC\u05DC\u05D0 \u05DC\u05E7\u05D5\u05D7" : t.clientName)
                    + "  \u2022  " + TicketDb.fmt(t.updated).substring(0, 10);
            list.addView(Ui.rowCard(this, t.title, sub, t.isOpen() ? He.status(t.status) : "\u05E1\u05D2\u05D5\u05E8\u05D4",
                    t.isOpen() ? Ui.PRIMARY : Ui.MUTED, v -> attach(t)), Ui.cardParams(this));
        }
        if (all.isEmpty()) list.addView(Ui.text(this, "   \u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05DE\u05EA\u05D0\u05D9\u05DE\u05D5\u05EA", 14, Ui.MUTED, false));
    }

    private void attach(final TicketDb.Ticket t) {
        new AlertDialog.Builder(this)
                .setMessage("\u05DC\u05E6\u05E8\u05E3 \u05D0\u05EA \u05D4\u05D8\u05E7\u05E1\u05D8 \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " - " + t.title + "?")
                .setPositiveButton("\u05E6\u05E8\u05E3", (d, w) -> {
                    TicketDb.get(this).addNote(t.id, "\uD83D\uDCC4 \u05D8\u05E7\u05E1\u05D8 \u05DE\u05E9\u05D5\u05EA\u05E3:\n" + text);
                    Toast.makeText(this, "\u05E6\u05D5\u05E8\u05E3 \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id));
                    finish();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }
}
