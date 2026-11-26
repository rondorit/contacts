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
    static final String EXTRA_NAME = "client_name";
    static final String EXTRA_PHONE = "client_phone";
    static final String EXTRA_DESC = "description";

    private TicketDb db;
    private TicketDb.Ticket t;
    private String oldStatus;
    private EditText eClient, ePhone, eTitle, eDesc, eEquip, eMac, eOutsource, eNote;
    private Spinner sStatus, sPriority, sCategory;
    private TextView tFollow;
    private LinearLayout history;
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
            } else {
                String n = getIntent().getStringExtra(EXTRA_NAME);
                String ph = getIntent().getStringExtra(EXTRA_PHONE);
                if (n != null) t.clientName = n;
                if (ph != null) t.clientPhone = ph;
            }
        }
        if (t.id == 0 && getIntent().getStringExtra(EXTRA_DESC) != null) {
            t.description = getIntent().getStringExtra(EXTRA_DESC);
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
        label(box, "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4");
        LinearLayout cRow = row(box);
        sCategory = new Spinner(this);
        cRow.addView(sCategory, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        rowButton(cRow, "\u05E0\u05D9\u05D4\u05D5\u05DC", v -> startActivity(new Intent(this, CategoriesActivity.class)));
        loadCategories(He.category(t.category));
        eEquip = field(box, "\u05E6\u05D9\u05D5\u05D3 (\u05E9\u05DD \u05DE\u05D7\u05E9\u05D1, \u05D3\u05D2\u05DD, \u05DE\u05E1\u05E4\u05E8 \u05E1\u05D9\u05D3\u05D5\u05E8\u05D9)", t.equipment,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        label(box, "\u05DB\u05EA\u05D5\u05D1\u05EA MAC \u05E9\u05DC \u05D4\u05E6\u05D9\u05D5\u05D3");
        LinearLayout mRow = row(box);
        eMac = new EditText(this);
        eMac.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        eMac.setHint("AA:BB:CC:DD:EE:FF");
        eMac.setText(t.mac);
        eMac.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);
        eMac.setOnFocusChangeListener((v, has) -> {
            if (!has) eMac.setText(TicketDb.formatMac(eMac.getText().toString()));
        });
        mRow.addView(eMac, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f));
        rowButton(mRow, "\u05D1\u05D7\u05E8", v -> chooseMac());
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
        history = new LinearLayout(this);
        history.setOrientation(LinearLayout.VERTICAL);
        showHistory();
        box.addView(history);
        eNote = field(box, "\u05D4\u05D5\u05E1\u05E3 \u05D4\u05E2\u05E8\u05D4 (\u05DE\u05D4 \u05D1\u05D5\u05E6\u05E2, \u05DE\u05D4 \u05D4\u05DC\u05D0\u05D4)", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eNote.setMinLines(2);
        LinearLayout pasteRow = row(box);
        rowButton(pasteRow, "\uD83D\uDCCB \u05D4\u05D3\u05D1\u05E7 \u05D8\u05E7\u05E1\u05D8 \u05E9\u05D4\u05D5\u05E2\u05EA\u05E7 (\u05EA\u05DE\u05DC\u05D5\u05DC / \u05D4\u05D5\u05D3\u05E2\u05D4)", v -> {
            android.content.ClipboardManager cm = getSystemService(android.content.ClipboardManager.class);
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) {
                toast("\u05D0\u05D9\u05DF \u05D8\u05E7\u05E1\u05D8 \u05DE\u05D5\u05E2\u05EA\u05E7");
                return;
            }
            CharSequence clip = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (clip == null || clip.toString().trim().isEmpty()) {
                toast("\u05D0\u05D9\u05DF \u05D8\u05E7\u05E1\u05D8 \u05DE\u05D5\u05E2\u05EA\u05E7");
                return;
            }
            String cur = eNote.getText().toString();
            eNote.setText(cur.isEmpty() ? clip.toString().trim() : cur + "\n" + clip.toString().trim());
            eNote.setSelection(eNote.getText().length());
        });

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
        LinearLayout sumRow = row(box);
        rowButton(sumRow, "\uD83D\uDCAC \u05E9\u05DC\u05D7 \u05E1\u05D9\u05DB\u05D5\u05DD \u05DC\u05DC\u05E7\u05D5\u05D7 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", v -> {
            readForm();
            if (!persist()) return;
            sendClientSummary();
            showHistory();
        });
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
        sv.addView(Ui.wrapCard(this, box));
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
        t.category = String.valueOf(sCategory.getSelectedItem());
        t.equipment = eEquip.getText().toString().trim();
        t.mac = TicketDb.formatMac(eMac.getText().toString());
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
            loadCategories(String.valueOf(sCategory.getSelectedItem()));
        }
    }

    /** Pick a MAC address already used in this client's earlier tickets. */
    private void chooseMac() {
        final java.util.List<String[]> macs = db.macsForClient(
                eClient.getText().toString().trim(), ePhone.getText().toString().trim());
        if (macs.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05DB\u05EA\u05D5\u05D1\u05D5\u05EA MAC \u05E7\u05D5\u05D3\u05DE\u05D5\u05EA \u05DC\u05DC\u05E7\u05D5\u05D7 \u05D6\u05D4 - \u05D4\u05E7\u05DC\u05D3 \u05D9\u05D3\u05E0\u05D9\u05EA");
            return;
        }
        String[] labels = new String[macs.size()];
        for (int i = 0; i < macs.size(); i++) {
            String eq = macs.get(i)[1];
            labels[i] = macs.get(i)[0] + (eq.isEmpty() ? "" : "\n" + eq);
        }
        new AlertDialog.Builder(this)
                .setTitle("\u05DB\u05EA\u05D5\u05D1\u05D5\u05EA MAC \u05E9\u05DC \u05D4\u05DC\u05E7\u05D5\u05D7")
                .setItems(labels, (d, w) -> {
                    eMac.setText(macs.get(w)[0]);
                    if (eEquip.getText().toString().trim().isEmpty()) eEquip.setText(macs.get(w)[1]);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void loadCategories(String selected) {
        java.util.List<String> items = Categories.list(this);
        if (selected != null && !selected.isEmpty() && !items.contains(selected)) items.add(selected);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sCategory.setAdapter(a);
        int i = items.indexOf(selected);
        if (i < 0) i = items.indexOf("\u05D0\u05D7\u05E8");
        sCategory.setSelection(Math.max(i, 0));
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

    /** Notes history; tap a note to delete it. */
    private void showHistory() {
        history.removeAllViews();
        java.util.List<TicketDb.Note> notes = t.id > 0 ? db.notes(t.id) : new java.util.ArrayList<>();
        if (notes.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("\u05D0\u05D9\u05DF \u05D4\u05E2\u05E8\u05D5\u05EA \u05E2\u05D3\u05D9\u05D9\u05DF.");
            history.addView(none);
            return;
        }
        for (final TicketDb.Note n : notes) {
            TextView tv = new TextView(this);
            tv.setText(TicketDb.fmt(n.time) + "\n" + n.text);
            tv.setTextSize(15);
            tv.setPadding(0, dp(8), 0, dp(8));
            tv.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA \u05D4\u05E9\u05D5\u05E8\u05D4 \u05D4\u05D6\u05D0\u05EA \u05DE\u05D4\u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D4?\n\n" + n.text)
                    .setPositiveButton("\u05DE\u05D7\u05E7", (d, w) -> {
                        db.deleteNote(n.id);
                        showHistory();
                    })
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show());
            history.addView(tv);
            android.view.View line = new android.view.View(this);
            line.setBackgroundColor(0xFFDDDDDD);
            history.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2 + 1)));
        }
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
        chooseTemplate(tpl -> askProviderFollowup(() -> doSend(whatsapp, phone, email, tpl)));
    }

    private interface TplChoice {
        void chosen(Templates.Tpl tpl);
    }

    /** Lets the user pick a message template (or the standard summary). */
    private void chooseTemplate(final TplChoice next) {
        final java.util.List<Templates.Tpl> tpls = Templates.list(this);
        if (tpls.isEmpty()) {
            next.chosen(null);
            return;
        }
        String[] names = new String[tpls.size() + 1];
        names[0] = "\u05E1\u05D9\u05DB\u05D5\u05DD \u05E7\u05E8\u05D9\u05D0\u05D4 \u05E8\u05D2\u05D9\u05DC (\u05D1\u05DC\u05D9 \u05EA\u05D1\u05E0\u05D9\u05EA)";
        for (int i = 0; i < tpls.size(); i++) names[i + 1] = tpls.get(i).name;
        new AlertDialog.Builder(this)
                .setTitle("\u05D0\u05D9\u05D6\u05D5 \u05EA\u05D1\u05E0\u05D9\u05EA \u05DC\u05E9\u05DC\u05D5\u05D7?")
                .setItems(names, (d, w) -> next.chosen(w == 0 ? null : tpls.get(w - 1)))
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    /** Asks when to check back with the provider; sets the ticket's follow-up. */
    private void askProviderFollowup(final Runnable next) {
        String[] opts = {"\u05DE\u05D7\u05E8 9:00", "\u05D1\u05E2\u05D5\u05D3 \u05D9\u05D5\u05DE\u05D9\u05D9\u05DD 9:00", "\u05D1\u05E2\u05D5\u05D3 \u05E9\u05D1\u05D5\u05E2 9:00", "\u05D1\u05D7\u05E8 \u05EA\u05D0\u05E8\u05D9\u05DA \u05D5\u05E9\u05E2\u05D4", "\u05D1\u05DC\u05D9 \u05DC\u05E9\u05E0\u05D5\u05EA \u05DE\u05E2\u05E7\u05D1"};
        new AlertDialog.Builder(this)
                .setTitle("\u05DE\u05EA\u05D9 \u05DC\u05D1\u05D3\u05D5\u05E7 \u05DE\u05D5\u05DC " + t.provider + "?")
                .setItems(opts, (d, w) -> {
                    if (w <= 2) {
                        Calendar c = Calendar.getInstance();
                        c.add(Calendar.DAY_OF_MONTH, w == 0 ? 1 : (w == 1 ? 2 : 7));
                        c.set(Calendar.HOUR_OF_DAY, 9);
                        c.set(Calendar.MINUTE, 0);
                        c.set(Calendar.SECOND, 0);
                        setFollowup(c.getTimeInMillis());
                        next.run();
                    } else if (w == 3) {
                        pickFollowup(next);
                    } else {
                        next.run();
                    }
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void doSend(boolean whatsapp, String phone, String email, Templates.Tpl tpl) {
        readForm();
        if (!"Closed".equals(t.status) && !"Waiting for outsourcing".equals(t.status)) {
            t.status = "Waiting for outsourcing";
            sStatus.setSelection(java.util.Arrays.asList(TicketDb.STATUSES).indexOf(t.status));
        }
        if (!persist()) return;
        db.addNote(t.id, "\u05E0\u05E9\u05DC\u05D7 \u05DC" + t.provider + (whatsapp ? " \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : " \u05D1\u05DE\u05D9\u05D9\u05DC")
                + (t.followupAt > 0 ? " | \u05DE\u05E2\u05E7\u05D1 \u05DE\u05D5\u05DC\u05D5: " + TicketDb.fmt(t.followupAt) : ""));
        showHistory();
        String text = tpl == null ? db.summary(t) : Templates.fill(this, t, tpl.body);
        String subject = tpl == null || tpl.subject.trim().isEmpty()
                ? "\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA #" + t.id + " - " + t.title : Templates.fill(this, t, tpl.subject);
        if (whatsapp) {
            open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"
                    + DetailActivity.whatsAppNumber(phone) + "?text=" + Uri.encode(text))));
        } else {
            Intent send = new Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                    .putExtra(Intent.EXTRA_SUBJECT, subject)
                    .putExtra(Intent.EXTRA_TEXT, text);
            send.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
            open(Intent.createChooser(send, "\u05E9\u05DC\u05D9\u05D7\u05EA \u05DE\u05D9\u05D9\u05DC"));
        }
    }

    /** Saves the form; returns false if the ticket has no subject yet. */
    private boolean persist() {
        if (!t.mac.isEmpty() && !TicketDb.isValidMac(t.mac)) {
            toast("\u05DB\u05EA\u05D5\u05D1\u05EA MAC \u05DC\u05D0 \u05EA\u05E7\u05D9\u05E0\u05D4 - \u05E6\u05E8\u05D9\u05DB\u05D9\u05DD \u05DC\u05D4\u05D9\u05D5\u05EA 12 \u05EA\u05D5\u05D5\u05D9\u05DD (0-9, A-F)");
            return false;
        }
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
        boolean closing = "Closed".equals(t.status) && !"Closed".equals(oldStatus);
        if (!persist()) return;
        toast("\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + " \u05E0\u05E9\u05DE\u05E8\u05D4");
        if (closing && !t.clientPhone.trim().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("\u05D4\u05E7\u05E8\u05D9\u05D0\u05D4 \u05E0\u05E1\u05D2\u05E8\u05D4")
                    .setMessage("\u05DC\u05E9\u05DC\u05D5\u05D7 \u05DC" + (t.clientName.isEmpty() ? "\u05DC\u05E7\u05D5\u05D7" : t.clientName)
                            + " \u05E1\u05D9\u05DB\u05D5\u05DD \u05E7\u05E6\u05E8 \u05E9\u05DC \u05D4\u05D8\u05D9\u05E4\u05D5\u05DC \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4?")
                    .setPositiveButton("\uD83D\uDCAC \u05E9\u05DC\u05D7 \u05E1\u05D9\u05DB\u05D5\u05DD", (d, w) -> {
                        sendClientSummary();
                        finish();
                    })
                    .setNegativeButton("\u05DC\u05D0", (d, w) -> finish())
                    .setOnCancelListener(d -> finish())
                    .show();
            return;
        }
        finish();
    }

    /** WhatsApp to the client with a short summary of the ticket (from the editable template). */
    private void sendClientSummary() {
        String phone = t.clientPhone.trim();
        if (phone.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05DC\u05E7\u05D5\u05D7");
            return;
        }
        String text = Templates.fill(this, t, Templates.summaryTemplate(this).body);
        db.addNote(t.id, "\u05E1\u05D9\u05DB\u05D5\u05DD \u05E0\u05E9\u05DC\u05D7 \u05DC\u05DC\u05E7\u05D5\u05D7 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4");
        open(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"
                + DetailActivity.whatsAppNumber(phone) + "?text=" + Uri.encode(text))));
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
        pickFollowup(null);
    }

    private void pickFollowup(final Runnable after) {
        final Calendar c = Calendar.getInstance();
        if (followupAt > 0) c.setTimeInMillis(followupAt);
        new DatePickerDialog(this, (view, y, m, d) -> new TimePickerDialog(this, (tp, h, min) -> {
            Calendar r = Calendar.getInstance();
            r.set(y, m, d, h, min, 0);
            setFollowup(r.getTimeInMillis());
            if (after != null) after.run();
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
