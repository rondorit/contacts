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
    private TextView tFollow;
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
        setTitle(t.id > 0 ? "Ticket #" + t.id : "New ticket");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = new TextView(this);
        head.setTextSize(20);
        head.setTypeface(null, Typeface.BOLD);
        head.setText(t.id > 0 ? "Ticket #" + t.id + "  \u00B7  opened " + TicketDb.fmt(t.created) : "New ticket");
        box.addView(head);

        eClient = field(box, "Client", t.clientName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        ePhone = field(box, "Client phone", t.clientPhone, InputType.TYPE_CLASS_PHONE);
        eTitle = field(box, "Subject (short)", t.title, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eDesc = field(box, "Problem description", t.description,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eDesc.setMinLines(3);
        sStatus = spinner(box, "Status", TicketDb.STATUSES, t.status);
        sPriority = spinner(box, "Priority", TicketDb.PRIORITIES, t.priority);
        sCategory = spinner(box, "Category", TicketDb.CATEGORIES, t.category);
        eEquip = field(box, "Equipment (computer name, model, serial no.)", t.equipment,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        eOutsource = field(box, "Outsourcing (company / contact / their ticket no.)", t.outsource,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        label(box, "Follow-up");
        tFollow = new TextView(this);
        tFollow.setTextSize(17);
        box.addView(tFollow);
        LinearLayout fRow = row(box);
        rowButton(fRow, "Set date & time", v -> pickFollowup());
        rowButton(fRow, "Tomorrow 9:00", v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 1);
            c.set(Calendar.HOUR_OF_DAY, 9);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            setFollowup(c.getTimeInMillis());
        });
        LinearLayout fRow2 = row(box);
        rowButton(fRow2, "In 1 week", v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_MONTH, 7);
            c.set(Calendar.SECOND, 0);
            setFollowup(c.getTimeInMillis());
        });
        rowButton(fRow2, "No follow-up", v -> setFollowup(0));
        showFollowup();

        label(box, "Notes history");
        TextView history = new TextView(this);
        history.setTextIsSelectable(true);
        StringBuilder sb = new StringBuilder();
        if (t.id > 0) {
            for (TicketDb.Note n : db.notes(t.id)) {
                if (sb.length() > 0) sb.append("\n\n");
                sb.append(TicketDb.fmt(n.time)).append('\n').append(n.text);
            }
        }
        history.setText(sb.length() == 0 ? "No notes yet." : sb.toString());
        box.addView(history);
        eNote = field(box, "Add a note (what was done, what's next)", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eNote.setMinLines(2);

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setText("Save ticket");
        save.setTextSize(18);
        save.setOnClickListener(v -> save());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        box.addView(save, lp);

        LinearLayout aRow = row(box);
        rowButton(aRow, "Call client", v -> {
            String ph = ePhone.getText().toString().trim();
            if (ph.isEmpty()) toast("No phone number");
            else open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(ph))));
        });
        rowButton(aRow, "Share / outsourcing", v -> share());
        if (t.id > 0) {
            LinearLayout dRow = row(box);
            rowButton(dRow, "Delete ticket", v -> new AlertDialog.Builder(this)
                    .setMessage("Delete ticket #" + t.id + "?")
                    .setPositiveButton("Delete", (d, w) -> {
                        Followups.cancel(this, t.id);
                        Followups.removeEvent(this, t);
                        db.delete(t.id);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        setContentView(sv);
    }

    private void readForm() {
        t.clientName = eClient.getText().toString().trim();
        t.clientPhone = ePhone.getText().toString().trim();
        t.title = eTitle.getText().toString().trim();
        t.description = eDesc.getText().toString().trim();
        t.status = (String) sStatus.getSelectedItem();
        t.priority = (String) sPriority.getSelectedItem();
        t.category = (String) sCategory.getSelectedItem();
        t.equipment = eEquip.getText().toString().trim();
        t.outsource = eOutsource.getText().toString().trim();
        t.followupAt = followupAt;
    }

    private void save() {
        readForm();
        if (t.title.isEmpty()) {
            if (t.description.isEmpty()) {
                toast("Write a subject for the ticket");
                return;
            }
            t.title = t.description.length() > 40 ? t.description.substring(0, 40) + "\u2026" : t.description;
        }
        boolean isNew = t.id == 0;
        db.save(t);
        if (isNew) db.addNote(t.id, "Ticket opened (" + t.status + ")");
        else if (!t.status.equals(oldStatus)) db.addNote(t.id, "Status: " + oldStatus + " \u2192 " + t.status);
        String note = eNote.getText().toString().trim();
        if (!note.isEmpty()) db.addNote(t.id, note);
        Followups.schedule(this, t);
        Followups.syncCalendar(this, t);
        db.save(t);
        toast("Ticket #" + t.id + " saved");
        finish();
    }

    private void share() {
        readForm();
        if (t.id == 0) {
            toast("Save the ticket first");
            return;
        }
        Intent i = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Service ticket #" + t.id + " - " + t.title)
                .putExtra(Intent.EXTRA_TEXT, db.summary(t));
        open(Intent.createChooser(i, "Share ticket"));
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
            tFollow.setText("None");
        } else {
            boolean late = followupAt < System.currentTimeMillis();
            tFollow.setText(TicketDb.fmt(followupAt) + (late ? "   (OVERDUE)" : ""));
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

    private Spinner spinner(LinearLayout box, String label, String[] items, String value) {
        label(box, label);
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        List<String> list = Arrays.asList(items);
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
            toast("No app found for this action");
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
