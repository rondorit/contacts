package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/** Create or edit one service ticket. */
public class TicketActivity extends Activity {
    static final String EXTRA_TICKET = "ticket";

    private TicketDb db;
    private TicketDb.Ticket t;
    private String oldStatus;
    private EditText eClient, ePhone, eTitle, eDesc, eEquip, eOutsource, eNote;
    private Spinner sStatus, sPriority, sCategory;
    private TextView tFollow, history;
    private Spinner sProvider;
    private final java.util.List<String> providerNames = new java.util.ArrayList<>();
    private long followupAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = TicketDb.get(this);
        long id = getIntent().getLongExtra(EXTRA_TICKET, 0);
        t = id > 0 ? db.load(id) : null;
        if (t == null) {
            t = new TicketDb.Ticket();
            int row = getIntent().getIntExtra(DetailActivity.EXTRA_ROW, -1);
            ClientStore s = Data.get(this);
            if (row >= 0 && row < s.rows.size()) {
                t.clientName = s.name(row);
                t.clientPhone = s.firstPhone(row);
            }
        }
        oldStatus = t.status;
        followupAt = t.followupAt;
        setTitle(t.id > 0 ? "\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id : "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = new TextView(this);
        head.setTextSize(20);
        head.setTypeface(null, Typeface.BOLD);
        head.setText(t.id > 0 ? "\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + "  \u00B7  \u05E0\u05E4\u05EA\u05D7\u05D4 " + TicketDb.fmt(t.created) : "\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4");
        box.addView(head);

        eClient = field(box, "\u05DC\u05E7\u05D5\u05D7", t.clientName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        ePhone = field(box, "\u05D8\u05DC\u05E4\u05D5\u05DF \u05D4\u05DC\u05E7\u05D5\u05D7", t.clientPhone, InputType.TYPE_CLASS_PHONE);
        eTitle = field(box, "\u05E0\u05D5\u05E9\u05D0 (\u05E7\u05E6\u05E8)", t.title, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eDesc = field(box, "\u05EA\u05D9\u05D0\u05D5\u05E8 \u05D4\u05EA\u05E7\u05DC\u05D4", t.description,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eDesc.setMinLines(3);
        sStatus = spinner(box, "\u05E1\u05D8\u05D8\u05D5\u05E1", TicketDb.STATUSES, He.STATUS, t.status);
        sPriority = spinner(box, "\u05D3\u05D7\u05D9\u05E4\u05D5\u05EA", TicketDb.PRIORITIES, He.PRIORITY, t.priority);
        sCategory = spinner(box, "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4", TicketDb.CATEGORIES, He.CATEGORY, t.category);
        eEquip = field(box, "\u05E6\u05D9\u05D5\u05D3 (\u05E9\u05DD \u05DE\u05D7\u05E9\u05D1, \u05D3\u05D2\u05DD, \u05DE\u05E1\u05E4\u05E8 \u05E1\u05D9\u05D3\u05D5\u05E8\u05D9)", t.equipment,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        label(box, "\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA (\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5)");
        LinearLayout pRow = row(box);
        sProvider = new Spinner(this);
        pRow.addView(sProvider, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        rowButton(pRow, "\u05E0\u05D9\u05D4\u05D5\u05DC", v -> startActivity(new Intent(this, ProvidersActivity.class)));
        loadProviders(t.provider);
        eOutsource = field(box, "\u05E4\u05E8\u05D8\u05D9\u05DD \u05DC\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5 (\u05DE\u05E1\u05E4\u05E8 \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D0\u05E6\u05DC\u05DD, \u05D0\u05D9\u05E9 \u05E7\u05E9\u05E8)", t.outsource,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        LinearLayout sendRow = row(box);
        rowButton(sendRow, "\u05E9\u05DC\u05D7 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", v -> sendToProvider(true));
        rowButton(sendRow, "\u05E9\u05DC\u05D7 \u05D1\u05DE\u05D9\u05D9\u05DC", v -> sendToProvider(false));

        label(box, "\u05DE\u05E2\u05E7\u05D1");
        tFollow = new TextView(this);
        tFollow.setTextSize(17);
        box.addView(tFollow);
        LinearLayout fRow = row(box);
        rowButton(fRow, "\u05D1\u05D7\u05E8 \u05EA\u05D0\u05E8\u05D9\u05DA \u05D5\u05E9\u05E2\u05D4", v -> pickFollowup());
        rowButton(fRow, "\u05DE\u05D7\u05E8 9:00", v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            setFollowup(c.getTimeInMillis());
        });
        LinearLayout fRow2 = row(box);
        rowButton(fRow2, "\u05D1\u05E2\u05D5\u05D3 \u05E9\u05D1\u05D5\u05E2", v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 7);
            c.set(Calendar.SECOND, 0);
            setFollowup(c.getTimeInMillis());
        });
        rowButton(fRow2, "\u05DC\u05DC\u05D0 \u05DE\u05E2\u05E7\u05D1", v -> setFollowup(0));
        showFollowup();

        label(box, "\u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D9\u05EA \u05D4\u05E2\u05E8\u05D5\u05EA");
        history = new TextView(this);
        history.setTextIsSelectable(true);
        showHistory();
        box.addView(history);
        eNote = field(box, "\u05D4\u05D5\u05E1\u05E3 \u05D4\u05E2\u05E8\u05D4 (\u05DE\u05D4 \u05D1\u05D5\u05E6\u05E2, \u05DE\u05D4 \u05D4\u05DC\u05D0\u05D4)", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eNote.setMinLines(2);

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setText("\u05E9\u05DE\u05D5\u05E8 \u05E7\u05E8\u05D9\u05D0\u05D4");
        save.setTextSize(18);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        box.addView(save, lp);

        LinearLayout aRow = row(box);
        rowButton(aRow, "\u05D4\u05EA\u05E7\u05E9\u05E8 \u05DC\u05DC\u05E7\u05D5\u05D7", v -> {
            String ph = ePhone.getText().toString().trim();
            if (ph.isEmpty()) toast("\u05D0\u05D9\u05DF \u05DE\u05E1\u05E4\u05E8 \u05D8\u05DC\u05E4\u05D5\u05DF");
            else open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(ph))));
        });
        rowButton(aRow, "\u05E9\u05EA\u05E3 / \u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5", v -> share());
        if (t.id > 0) {
            LinearLayout dRow = row(box);
            rowButton(dRow, "\u05DE\u05D7\u05E7 \u05E7\u05E8\u05D9\u05D0\u05D4", v -> new AlertDialog.Builder(this)
                    .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + "?")
                    .setPositiveButton("\u05DE\u05D7\u05E7", (d, w) -> {
                        Followups.cancel(this, t.id);
                        Followups.removeEvent(this, t);
                        db.delete(t.id);
                        finish();
                    })
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show());
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
    }

    private void readForm() {
        t.clientName = eClient.getText().toString().trim();
        t.clientPhone = ePhone.getText().toString().trim();
        t.title = eTitle.getText().toString().trim();
        t.description = eDesc.getText().toString().trim();
        t.status = TicketDb.STATUSES[sStatus.getSelectedItemPosition()];
        t.priority = TicketDb.PRIORITIES[sPriority.getSelectedItemPosition()];
        t.category = TicketDb.CATEGORIES[sCategory.getSelectedItemPosition()];
        t.equipment = eEquip.getText().toString().trim();
        t.outsource = eOutsource.getText().toString().trim();
        int pi = sProvider.getSelectedItemPosition();
        t.provider = pi <= 0 || pi > providerNames.size() ? "" : providerNames.get(pi - 1);
        t.followupAt = followupAt;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sProvider != null) {
            int pi = sProvider.getSelectedItemPosition();
            loadProviders(pi <= 0 || pi > providerNames.size() ? "" : providerNames.get(pi - 1));
        }
    }

    private void loadProviders(String selected) {
        providerNames.clear();
        for (TicketDb.Provider p : db.providers()) providerNames.add(p.name);
        if (!selected.isEmpty() && !providerNames.contains(selected)) providerNames.add(selected);
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("\u05DC\u05DC\u05D0");
        items.addAll(providerNames);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sProvider.setAdapter(a);
        sProvider.setSelection(selected.isEmpty() ? 0 : providerNames.indexOf(selected) + 1);
    }

    private void showHistory() {
        StringBuilder sb = new StringBuilder();
        if (t.id > 0) {
            for (TicketDb.Note n : db.notes(t.id)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(TicketDb.fmt(n.time)).append('\n').append(n.text);
            }
        }
        history.setText(sb.length() == 0 ? "\u05D0\u05D9\u05DF \u05D4\u05E2\u05E8\u05D5\u05EA \u05E2\u05D3\u05D9\u05D9\u05DF." : sb.toString());
    }

    /** Sends the ticket to the chosen outsourcing provider by WhatsApp or email. */
    private void sendToProvider(boolean whatsapp) {
        readForm();
        if (t.provider.isEmpty()) {
            toast("\u05D1\u05D7\u05E8 \u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA");
            return;
        }
        TicketDb.Provider p = db.providerByName(t.provider);
        String phone = p == null ? "" : p.phone.trim();
        String email = p == null ? "" : p.email.trim();
        if (whatsapp && phone.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05E0\u05D5\u05EA\u05DF \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA - \u05D4\u05D5\u05E1\u05E3 \u05D0\u05D5\u05EA\u05D5 \u05D1'\u05E0\u05D9\u05D4\u05D5\u05DC'");
            return;
        }
        if (!whatsapp && email.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05DE\u05D9\u05D9\u05DC \u05DC\u05E0\u05D5\u05EA\u05DF \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA - \u05D4\u05D5\u05E1\u05E3 \u05D0\u05D5\u05EA\u05D5 \u05D1'\u05E0\u05D9\u05D4\u05D5\u05DC'");
            return;
        }
        if (!"Closed".equals(t.status) && !"Waiting for outsourcing".equals(t.status)) {
            t.status = "Waiting for outsourcing";
            sStatus.setSelection(java.util.Arrays.asList(TicketDb.STATUSES).indexOf(t.status));
        }
        if (!persist()) return;
        db.addNote(t.id, "\u05E0\u05E9\u05DC\u05D7 \u05DC" + t.provider + (whatsapp ? " \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : " \u05D1\u05DE\u05D9\u05D9\u05DC"));
        showHistory();
        String text = db.summary(t);
        if (whatsapp) {
            open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"
                    + DetailActivity.whatsAppNumber(phone) + "?text=" + Uri.encode(text))));
        } else {
            Intent send = new Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                    .putExtra(Intent.EXTRA_SUBJECT, "\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA #" + t.id + " - " + t.title)
                    .putExtra(Intent.EXTRA_TEXT, text);
            send.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
            open(Intent.createChooser(send, "\u05E9\u05DC\u05D9\u05D7\u05EA \u05DE\u05D9\u05D9\u05DC"));
        }
    }

    /** Saves the form; returns false if the ticket has no subject yet. */
    private boolean persist() {
        if (t.title.isEmpty()) {
            if (t.description.isEmpty()) {
                toast("\u05DB\u05EA\u05D5\u05D1 \u05E0\u05D5\u05E9\u05D0 \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4");
                return false;
            }
            t.title = t.description.length() > 40 ? t.description.substring(0, 40) + "\u2026" : t.description;
        }
        boolean isNew = t.id == 0;
        db.save(t);
        if (isNew) db.addNote(t.id, "\u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05E0\u05E4\u05EA\u05D7\u05D4 (" + He.status(t.status) + ")");
        else if (!t.status.equals(oldStatus)) db.addNote(t.id, "\u05E1\u05D8\u05D8\u05D5\u05E1: " + He.status(oldStatus) + " \u2190 " + He.status(t.status));
        oldStatus = t.status;
        String note = eNote.getText().toString().trim();
        if (!note.isEmpty()) db.addNote(t.id, note);
        eNote.setText("");
        Followups.schedule(this, t);
        Followups.syncCalendar(this, t);
        db.save(t);
        return true;
    }

    private void save() {
        readForm();
        if (!persist()) return;
        toast("\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " \u05E0\u05E9\u05DE\u05E8\u05D4");
        finish();
    }

    private void share() {
        readForm();
        if (t.id == 0) {
            toast("\u05E9\u05DE\u05D5\u05E8 \u05E7\u05D5\u05D3\u05DD \u05D0\u05EA \u05D4\u05E7\u05E8\u05D9\u05D0\u05D4");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA #" + t.id + " - " + t.title)
                .putExtra(Intent.EXTRA_TEXT, db.summary(t));
        open(Intent.createChooser(i, "\u05E9\u05D9\u05EA\u05D5\u05E3 \u05E7\u05E8\u05D9\u05D0\u05D4"));
    }

    private void pickFollowup() {
        final Calendar c = Calendar.getInstance();
        if (followupAt > 0) c.setTimeInMillis(followupAt);
        new DatePickerDialog(this, (view, y, m, d) -> new TimePickerDialog(this, (tp, h, min) -> {
            Calendar r = Calendar.getInstance();
            r.set(y, m, d, h, min, 0);
            setFollowup(r.getTimeInMillis());
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void setFollowup(long when) {
        followupAt = when;
        showFollowup();
    }

    private void showFollowup() {
        if (followupAt == 0) {
            tFollow.setText("\u05D0\u05D9\u05DF");
        } else {
            boolean late = followupAt < System.currentTimeMillis();
            tFollow.setText(TicketDb.fmt(followupAt) + (late ? "   (\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8)" : ""));
        }
    }

    // ------------------------------------------------------------------ small UI helpers

    private void label(LinearLayout box, String text) {
        TextView l = new TextView(this);
        l.setText(text);
        l.setTextSize(13);
        l.setTextColor(0xFF777777);
        l.setPadding(0, dp(14), 0, 0);
        box.addView(l);
    }

    private EditText field(LinearLayout box, String label, String value, int inputType) {
        label(box, label);
        EditText e = new EditText(this);
        e.setInputType(inputType);
        e.setText(value);
        box.addView(e);
        return e;
    }

    private Spinner spinner(LinearLayout box, String label, String[] keys, String[] items, String value) {
        label(box, label);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        List<String> list = Arrays.asList(keys);
        int i = list.indexOf(value);
        s.setSelection(i < 0 ? 0 : i);
        box.addView(s);
        return s;
    }

    private LinearLayout row(LinearLayout box) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        box.addView(r);
        return r;
    }

    private void rowButton(LinearLayout r, String text, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setOnClickListener(l);
        r.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05DC\u05E4\u05E2\u05D5\u05DC\u05D4 \u05D6\u05D5");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
