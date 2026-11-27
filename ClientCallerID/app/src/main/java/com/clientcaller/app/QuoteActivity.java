package com.clientcaller.app;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Create / edit a price quote: recipient, lines from the price list or free, discount, 18% VAT, send. */
public class QuoteActivity extends Activity {
    static final String EXTRA_QUOTE = "quote";
    static final String EXTRA_TICKET = "ticket";

    private QuoteDb db;
    private QuoteDb.Quote q;
    private String oldStatus;
    private EditText eTitle, eName, ePhone, eEmail, eDiscount, eValid, eNotes;
    private Spinner sStatus;
    private LinearLayout linesBox;
    private TextView totals, tFollow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = QuoteDb.get(this);
        long id = getIntent().getLongExtra(EXTRA_QUOTE, 0);
        q = id > 0 ? db.load(id) : null;
        if (q == null) {
            q = new QuoteDb.Quote();
            ClientStore s = Data.get(this);
            int row = getIntent().getIntExtra(DetailActivity.EXTRA_ROW, -1);
            long ticketId = getIntent().getLongExtra(EXTRA_TICKET, 0);
            if (ticketId > 0) {
                TicketDb.Ticket t = TicketDb.get(this).load(ticketId);
                if (t != null) {
                    q.ticketId = t.id;
                    q.clientName = t.clientName;
                    q.clientPhone = t.clientPhone;
                    q.title = t.title;
                    row = t.clientPhone.isEmpty() ? -1 : s.find(t.clientPhone);
                }
            }
            if (row >= 0 && row < s.rows.size()) fillFromRow(s, row);
        }
        oldStatus = q.status;
        setTitle(q.id > 0 ? "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id : "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 \u05D7\u05D3\u05E9\u05D4");

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        box.setPadding(p, p, p, p);

        TextView head = Ui.text(this, q.id > 0 ? "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 \u05DE\u05E1' " + q.id + "  \u2022  " + TicketDb.fmt(q.created).substring(0, 10)
                : "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 \u05D7\u05D3\u05E9\u05D4", 20, Ui.TEXT, true);
        box.addView(head);
        if (q.ticketId > 0) box.addView(Ui.text(this, "\u05DE\u05E7\u05D5\u05E9\u05E8\u05EA \u05DC\u05E7\u05E8\u05D9\u05D0\u05D4 #" + q.ticketId, 13, Ui.MUTED, false));

        eTitle = field(box, "\u05E0\u05D5\u05E9\u05D0 \u05D4\u05D4\u05E6\u05E2\u05D4", q.title, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        label(box, "\u05DC\u05DB\u05D1\u05D5\u05D3");
        Button pick = new Button(this);
        pick.setAllCaps(false);
        pick.setText("\uD83D\uDC65 \u05D1\u05D7\u05E8 \u05DC\u05E7\u05D5\u05D7 \u05DE\u05D4\u05E8\u05E9\u05D9\u05DE\u05D4");
        pick.setOnClickListener(v -> pickClient());
        box.addView(pick);
        eName = field(box, "\u05E9\u05DD \u05D4\u05DC\u05E7\u05D5\u05D7 / \u05D4\u05D2\u05D5\u05E3", q.clientName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        ePhone = field(box, "\u05D8\u05DC\u05E4\u05D5\u05DF", q.clientPhone, InputType.TYPE_CLASS_PHONE);
        eEmail = field(box, "\u05DE\u05D9\u05D9\u05DC", q.clientEmail, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        box.addView(Ui.sectionTitle(this, "\u05E4\u05E8\u05D9\u05D8\u05D9\u05DD"));
        linesBox = new LinearLayout(this);
        linesBox.setOrientation(LinearLayout.VERTICAL);
        box.addView(linesBox);
        LinearLayout addRow = new LinearLayout(this);
        rowButton(addRow, "+ \u05DE\u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", v -> pickFromCatalog());
        rowButton(addRow, "+ \u05E4\u05E8\u05D9\u05D8 \u05D7\u05D5\u05E4\u05E9\u05D9", v -> editLine(-1));
        box.addView(addRow);

        eDiscount = field(box, "\u05D4\u05E0\u05D7\u05D4 (%)", q.discountPct > 0 ? QuoteDb.qty(q.discountPct) : "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        eDiscount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { showTotals(); }
        });
        totals = new TextView(this);
        totals.setTextSize(16);
        totals.setPadding(dp(12), dp(10), dp(12), dp(10));
        totals.setBackground(Ui.rounded(0xFFE3EDFB, 12, this));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(10);
        box.addView(totals, tlp);

        eValid = field(box, "\u05EA\u05D5\u05E7\u05E3 \u05D4\u05D4\u05E6\u05E2\u05D4 (\u05D9\u05DE\u05D9\u05DD)", String.valueOf(q.validDays), InputType.TYPE_CLASS_NUMBER);
        eNotes = field(box, "\u05D4\u05E2\u05E8\u05D5\u05EA / \u05EA\u05E0\u05D0\u05D9\u05DD", q.notes,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        eNotes.setMinLines(2);

        label(box, "\u05E1\u05D8\u05D8\u05D5\u05E1");
        sStatus = new Spinner(this);
        ArrayAdapter<String> sa = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, QuoteDb.STATUS_HE);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sStatus.setAdapter(sa);
        sStatus.setSelection(Math.max(0, java.util.Arrays.asList(QuoteDb.STATUSES).indexOf(q.status)));
        box.addView(sStatus);

        label(box, "\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05DC\u05D1\u05D3\u05D5\u05E7 \u05D0\u05D9\u05E9\u05D5\u05E8");
        tFollow = new TextView(this);
        tFollow.setTextSize(16);
        box.addView(tFollow);
        LinearLayout fRow = new LinearLayout(this);
        rowButton(fRow, "\u05D1\u05E2\u05D5\u05D3 3 \u05D9\u05DE\u05D9\u05DD", v -> followIn(3));
        rowButton(fRow, "\u05D1\u05E2\u05D5\u05D3 \u05E9\u05D1\u05D5\u05E2", v -> followIn(7));
        rowButton(fRow, "\u05DC\u05DC\u05D0", v -> {
            q.followupAt = 0;
            showFollow();
        });
        box.addView(fRow);
        showFollow();

        Button save = new Button(this);
        save.setAllCaps(false);
        save.setTextSize(18);
        save.setText("\u05E9\u05DE\u05D5\u05E8 \u05D4\u05E6\u05E2\u05D4");
        save.setOnClickListener(v -> {
            if (persist()) {
                toast("\u05D4\u05D4\u05E6\u05E2\u05D4 \u05E0\u05E9\u05DE\u05E8\u05D4");
                finish();
            }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(16);
        box.addView(save, slp);

        box.addView(Ui.sectionTitle(this, "\u05E9\u05DC\u05D9\u05D7\u05D4 \u05DC\u05DC\u05E7\u05D5\u05D7"));
        LinearLayout s1 = new LinearLayout(this);
        rowButton(s1, "\uD83D\uDCC4 PDF \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", v -> send(true, true));
        rowButton(s1, "\uD83D\uDCC4 PDF \u05D1\u05DE\u05D9\u05D9\u05DC", v -> send(true, false));
        box.addView(s1);
        LinearLayout s2 = new LinearLayout(this);
        rowButton(s2, "\uD83D\uDCAC \u05D8\u05E7\u05E1\u05D8 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4", v -> send(false, true));
        rowButton(s2, "\u2709\uFE0F \u05D8\u05E7\u05E1\u05D8 \u05D1\u05DE\u05D9\u05D9\u05DC", v -> send(false, false));
        box.addView(s2);
        LinearLayout s3 = new LinearLayout(this);
        rowButton(s3, "\uD83D\uDC41\uFE0F \u05EA\u05E6\u05D5\u05D2\u05EA PDF", v -> previewPdf());
        if (q.id > 0) rowButton(s3, "\uD83D\uDDD1\uFE0F \u05DE\u05D7\u05E7 \u05D4\u05E6\u05E2\u05D4", v -> new AlertDialog.Builder(this)
                .setMessage("\u05DC\u05DE\u05D7\u05D5\u05E7 \u05D0\u05EA \u05D4\u05E6\u05E2\u05EA \u05D4\u05DE\u05D7\u05D9\u05E8 " + q.id + "?")
                .setPositiveButton("\u05DE\u05D7\u05E7", (d, w) -> Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id, () -> {
                    cancelReminder(this, q.id);
                    db.delete(q.id);
                    finish();
                }))
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show());
        box.addView(s3);

        ScrollView sv = new ScrollView(this);
        sv.addView(Ui.wrapCard(this, box));
        setContentView(sv);
        He.rtl(this);
        showLines();
    }

    private void fillFromRow(ClientStore s, int row) {
        q.clientName = s.name(row);
        q.clientPhone = s.firstPhone(row);
        for (String v : s.rows.get(row)) {
            if (v.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { q.clientEmail = v.trim(); break; }
        }
    }

    // ------------------------------------------------------------------ recipient

    private void pickClient() {
        final ClientStore s = Data.get(this);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText search = new EditText(this);
        search.setHint("\uD83D\uDD0D \u05D7\u05D9\u05E4\u05D5\u05E9");
        lay.addView(search);
        final ListView lv = new ListView(this);
        final List<Integer> shown = new ArrayList<>();
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lv.setAdapter(ad);
        lay.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));
        final Runnable filter = () -> {
            shown.clear();
            ad.clear();
            for (int r : s.search(search.getText().toString())) {
                if (shown.size() >= 200) break;
                shown.add(r);
                ad.add(s.name(r) + "   " + s.firstPhone(r));
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence x, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence x, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { filter.run(); }
        });
        filter.run();
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("\u05D1\u05D7\u05E8 \u05DC\u05E7\u05D5\u05D7").setView(lay)
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null).create();
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            fillFromRow(s, shown.get(pos));
            eName.setText(q.clientName);
            ePhone.setText(q.clientPhone);
            eEmail.setText(q.clientEmail);
            dlg.dismiss();
        });
        dlg.show();
    }

    // ------------------------------------------------------------------ lines

    private void showLines() {
        linesBox.removeAllViews();
        for (int i = 0; i < q.lines.size(); i++) {
            final int idx = i;
            QuoteDb.Line l = q.lines.get(i);
            LinearLayout card = Ui.rowCard(this, (i + 1) + ". " + l.name,
                    QuoteDb.qty(l.qty) + " \u00D7 " + QuoteDb.money(l.price) + " = " + QuoteDb.money(l.total()),
                    "", 0, v -> editLine(idx));
            card.setBackground(Ui.rounded(0xFFF3F6FB, 12, this));
            card.setElevation(0);
            linesBox.addView(card, Ui.cardParams(this));
        }
        if (q.lines.isEmpty()) linesBox.addView(Ui.text(this, "  \u05E2\u05D3\u05D9\u05D9\u05DF \u05D0\u05D9\u05DF \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD", 14, Ui.MUTED, false));
        showTotals();
    }

    private double discountValue() {
        try {
            return Math.max(0, Math.min(100, Double.parseDouble(eDiscount.getText().toString().trim())));
        } catch (Exception e) {
            return 0;
        }
    }

    private void showTotals() {
        if (totals == null) return;
        q.discountPct = discountValue();
        StringBuilder sb = new StringBuilder();
        sb.append("\u05E1\u05DB\u05D5\u05DD \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE: ").append(QuoteDb.money(q.subtotal()));
        if (q.discountPct > 0) sb.append("\n\u05D4\u05E0\u05D7\u05D4 ").append(QuoteDb.qty(q.discountPct)).append("%: -").append(QuoteDb.money(q.discount()));
        sb.append("\n\u05DE\u05E2\"\u05DE 18%: ").append(QuoteDb.money(q.vat()));
        sb.append("\n\u05E1\u05D4\"\u05DB \u05DC\u05EA\u05E9\u05DC\u05D5\u05DD: ").append(QuoteDb.money(q.total()));
        totals.setText(sb.toString());
    }

    private void editLine(final int idx) {
        final QuoteDb.Line l = idx >= 0 ? q.lines.get(idx) : new QuoteDb.Line();
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText name = field(lay, "\u05EA\u05D9\u05D0\u05D5\u05E8 \u05D4\u05E4\u05E8\u05D9\u05D8 / \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA", l.name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText qty = field(lay, "\u05DB\u05DE\u05D5\u05EA", QuoteDb.qty(l.qty), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText price = field(lay, "\u05DE\u05D7\u05D9\u05E8 \u05DC\u05D9\u05D7\u05D9\u05D3\u05D4 \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE (\u20AA)", l.price == 0 ? "" : QuoteDb.qty(l.price),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(idx >= 0 ? "\u05E2\u05E8\u05D9\u05DB\u05EA \u05E4\u05E8\u05D9\u05D8" : "\u05E4\u05E8\u05D9\u05D8 \u05D7\u05D3\u05E9")
                .setView(lay)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    l.name = name.getText().toString().trim();
                    l.qty = parse(qty.getText().toString(), 1);
                    l.price = parse(price.getText().toString(), 0);
                    if (l.name.isEmpty()) {
                        toast("\u05D7\u05E1\u05E8 \u05EA\u05D9\u05D0\u05D5\u05E8");
                        return;
                    }
                    if (idx < 0) q.lines.add(l);
                    showLines();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (idx >= 0) b.setNeutralButton("\u05D4\u05E1\u05E8", (d, w) -> {
            q.lines.remove(idx);
            showLines();
        });
        b.show();
    }

    static double parse(String s, double def) {
        try {
            return Double.parseDouble(s.replace("\u20AA", "").replace(",", "").trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void pickFromCatalog() {
        final List<QuoteDb.Item> items = db.catalog();
        if (items.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage("\u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF \u05E8\u05D9\u05E7. \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD \u05DC\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF?")
                    .setPositiveButton("\u05E4\u05EA\u05D7 \u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", (d, w) -> startActivity(new Intent(this, CatalogActivity.class)))
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show();
            return;
        }
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(dp(16), dp(8), dp(16), 0);
        final EditText search = new EditText(this);
        search.setHint("\uD83D\uDD0D \u05D7\u05D9\u05E4\u05D5\u05E9 \u05D1\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF");
        lay.addView(search);
        final ListView lv = new ListView(this);
        final List<QuoteDb.Item> shown = new ArrayList<>();
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        lv.setAdapter(ad);
        lay.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(360)));
        final Runnable filter = () -> {
            String f = search.getText().toString().trim().toLowerCase(Locale.ROOT);
            shown.clear();
            ad.clear();
            for (QuoteDb.Item it : items) {
                if (!f.isEmpty() && !(it.name + " " + it.category).toLowerCase(Locale.ROOT).contains(f)) continue;
                shown.add(it);
                ad.add(it.name + "   " + QuoteDb.money(it.price) + (it.unit.isEmpty() ? "" : " / " + it.unit));
            }
        };
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence x, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence x, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { filter.run(); }
        });
        filter.run();
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("\u05D1\u05D7\u05E8 \u05DE\u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF").setView(lay)
                .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null).create();
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            QuoteDb.Item it = shown.get(pos);
            QuoteDb.Line l = new QuoteDb.Line();
            l.name = it.name + (it.unit.isEmpty() ? "" : " (" + it.unit + ")");
            l.price = it.price;
            l.qty = 1;
            q.lines.add(l);
            showLines();
            toast("\u05E0\u05D5\u05E1\u05E3: " + it.name);
        });
        dlg.show();
    }

    // ------------------------------------------------------------------ follow-up

    private void followIn(int days) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, days);
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        q.followupAt = c.getTimeInMillis();
        showFollow();
    }

    private void showFollow() {
        tFollow.setText(q.followupAt == 0 ? "\u05DC\u05DC\u05D0 \u05EA\u05D6\u05DB\u05D5\u05E8\u05EA" : TicketDb.fmt(q.followupAt));
    }

    static void scheduleReminder(Context c, QuoteDb.Quote q) {
        try {
            AlarmManager am = c.getSystemService(AlarmManager.class);
            PendingIntent pi = reminderIntent(c, q.id);
            am.cancel(pi);
            if (q.isOpen() && q.followupAt > System.currentTimeMillis()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, q.followupAt, pi);
            }
        } catch (Exception ignored) { }
    }

    static void cancelReminder(Context c, long id) {
        try {
            c.getSystemService(AlarmManager.class).cancel(reminderIntent(c, id));
        } catch (Exception ignored) { }
    }

    private static PendingIntent reminderIntent(Context c, long id) {
        Intent i = new Intent(c, FollowupReceiver.class).setAction(FollowupReceiver.ACTION_QUOTE).putExtra(EXTRA_QUOTE, id);
        return PendingIntent.getBroadcast(c, 200000 + (int) id, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // ------------------------------------------------------------------ save / send

    private boolean persist() {
        q.title = eTitle.getText().toString().trim();
        q.clientName = eName.getText().toString().trim();
        q.clientPhone = ePhone.getText().toString().trim();
        q.clientEmail = eEmail.getText().toString().trim();
        q.discountPct = discountValue();
        q.validDays = (int) parse(eValid.getText().toString(), 30);
        q.notes = eNotes.getText().toString().trim();
        q.status = QuoteDb.STATUSES[sStatus.getSelectedItemPosition()];
        if (q.clientName.isEmpty()) {
            toast("\u05D7\u05E1\u05E8 \u05E9\u05DD \u05DC\u05E7\u05D5\u05D7");
            return false;
        }
        boolean isNew = q.id == 0;
        db.save(q);
        if (q.ticketId > 0 && (isNew || !q.status.equals(oldStatus))) {
            TicketDb.get(this).addNote(q.ticketId, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + ": " + QuoteDb.statusHe(q.status)
                    + " (" + QuoteDb.money(q.total()) + ")");
        }
        oldStatus = q.status;
        scheduleReminder(this, q);
        return true;
    }

    private void previewPdf() {
        if (!persist()) return;
        try {
            File f = QuotePdf.build(this, q);
            Uri u = FilesProvider.uriFor(f);
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(u, "application/pdf")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05D4\u05E6\u05D9\u05D2: " + e.getMessage());
        }
    }

    private void send(boolean pdf, boolean whatsapp) {
        if (!persist()) return;
        if (q.lines.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD \u05D1\u05D4\u05E6\u05E2\u05D4");
            return;
        }
        String phone = q.clientPhone;
        String email = q.clientEmail;
        if (whatsapp && phone.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05D8\u05DC\u05E4\u05D5\u05DF \u05DC\u05DC\u05E7\u05D5\u05D7");
            return;
        }
        if (!whatsapp && email.isEmpty()) {
            toast("\u05D0\u05D9\u05DF \u05DE\u05D9\u05D9\u05DC \u05DC\u05DC\u05E7\u05D5\u05D7");
            return;
        }
        if ("Draft".equals(q.status)) {
            q.status = "Sent";
            sStatus.setSelection(1);
            if (q.followupAt == 0) {
                followIn(7);
            }
            persist();
        }
        String subject = "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + (q.title.isEmpty() ? "" : " - " + q.title);
        String body = QuoteDb.text(this, q);
        try {
            if (pdf) {
                File f = QuotePdf.build(this, q);
                Uri u = FilesProvider.uriFor(f);
                String caption = "\u05E9\u05DC\u05D5\u05DD " + q.clientName + ", \u05DE\u05E6\u05D5\u05E8\u05E4\u05EA \u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id
                        + ". \u05E1\u05D4\"\u05DB \u05DC\u05EA\u05E9\u05DC\u05D5\u05DD: " + QuoteDb.money(q.total())
                        + ".\n\u05DC\u05D0\u05D9\u05E9\u05D5\u05E8 \u05D4\u05D4\u05E6\u05E2\u05D4 \u05D0\u05E4\u05E9\u05E8 \u05E4\u05E9\u05D5\u05D8 \u05DC\u05D4\u05E9\u05D9\u05D1: \"\u05DE\u05D0\u05E9\u05E8/\u05EA\".";
                Intent i = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                        .putExtra(Intent.EXTRA_STREAM, u)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.setClipData(ClipData.newRawUri("", u));
                if (whatsapp) {
                    i.putExtra(Intent.EXTRA_TEXT, caption);
                    i.putExtra("jid", DetailActivity.whatsAppNumber(phone) + "@s.whatsapp.net");
                    if (!startWith(i, "com.whatsapp") && !startWith(i, "com.whatsapp.w4b")) {
                        startActivity(Intent.createChooser(i, "\u05E9\u05DC\u05D9\u05D7\u05EA \u05D4\u05E6\u05E2\u05EA \u05D4\u05DE\u05D7\u05D9\u05E8"));
                    }
                } else {
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{email})
                            .putExtra(Intent.EXTRA_SUBJECT, subject)
                            .putExtra(Intent.EXTRA_TEXT, caption + "\n\n" + Issuer.signature(this));
                    i.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
                    startActivity(Intent.createChooser(i, "\u05E9\u05DC\u05D9\u05D7\u05EA \u05D4\u05E6\u05E2\u05EA \u05D4\u05DE\u05D7\u05D9\u05E8"));
                }
            } else if (whatsapp) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"
                        + DetailActivity.whatsAppNumber(phone) + "?text=" + Uri.encode(body))));
            } else {
                TicketActivity.openEmail(this, email, "", subject, body);
            }
            if (q.ticketId > 0) TicketDb.get(this).addNote(q.ticketId, "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + " \u05E0\u05E9\u05DC\u05D7\u05D4 \u05DC\u05DC\u05E7\u05D5\u05D7"
                    + (whatsapp ? " \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4" : " \u05D1\u05DE\u05D9\u05D9\u05DC") + (pdf ? " (PDF)" : ""));
        } catch (Exception e) {
            toast("\u05D4\u05E9\u05DC\u05D9\u05D7\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage());
        }
    }

    private boolean startWith(Intent i, String pkg) {
        try {
            Intent x = new Intent(i).setPackage(pkg);
            startActivity(x);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ helpers

    private void label(LinearLayout box, String text) {
        TextView l = Ui.text(this, text, 13, Ui.MUTED, false);
        l.setPadding(0, dp(12), 0, 0);
        box.addView(l);
    }

    private EditText field(LinearLayout box, String label, String value, int type) {
        label(box, label);
        EditText e = new EditText(this);
        e.setInputType(type);
        e.setText(value);
        box.addView(e);
        return e;
    }

    private void rowButton(LinearLayout row, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setOnClickListener(l);
        row.addView(b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
