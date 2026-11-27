package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Equipment installed at a client (type, model, serial, MAC, warranty), with its ticket history. */
final class Equipment {
    static final String[] TYPES = {"\uD83D\uDCFA \u05DE\u05DE\u05D9\u05E8 / \u05E1\u05D8\u05E8\u05D9\u05DE\u05E8", "\uD83D\uDCF6 \u05E8\u05D0\u05D5\u05D8\u05E8 / \u05DE\u05D5\u05D3\u05DD", "\uD83D\uDD00 \u05DE\u05EA\u05D2", "\uD83D\uDCE1 \u05E0\u05E7\u05D5\u05D3\u05EA \u05D2\u05D9\u05E9\u05D4", "\uD83D\uDCF7 \u05DE\u05E6\u05DC\u05DE\u05D4", "\uD83C\uDFA5 \u05DE\u05E7\u05DC\u05D9\u05D8 (NVR/DVR)",
            "\uD83D\uDCBB \u05DE\u05D7\u05E9\u05D1", "\uD83D\uDDA8\uFE0F \u05DE\u05D3\u05E4\u05E1\u05EA", "\uD83D\uDCDE \u05D8\u05DC\u05E4\u05D5\u05DF", "\uD83D\uDD0B UPS", "\uD83D\uDD27 \u05D0\u05D7\u05E8"};

    static final class Item {
        long id, warranty, created;
        String client = "", type = "", model = "", serial = "", mac = "", notes = "";

        String line() {
            StringBuilder sb = new StringBuilder(type.isEmpty() ? "\uD83D\uDD27 \u05E6\u05D9\u05D5\u05D3" : type);
            if (!model.isEmpty()) sb.append(" \u2022 ").append(model);
            if (!serial.isEmpty()) sb.append("\nS/N: ").append(serial);
            if (!mac.isEmpty()) sb.append(serial.isEmpty() ? "\n" : "   ").append("MAC: ").append(mac);
            if (warranty > 0) {
                sb.append("\n\u05D0\u05D7\u05E8\u05D9\u05D5\u05EA \u05E2\u05D3 ").append(TicketDb.fmt(warranty).substring(0, 10));
                if (warranty < System.currentTimeMillis()) sb.append("  \u26A0\uFE0F \u05E4\u05D2\u05D4");
            }
            if (!notes.isEmpty()) sb.append("\n").append(notes);
            return sb.toString();
        }
    }

    private Equipment() { }

    static List<Item> forClient(Context c, String client) {
        List<Item> out = new ArrayList<>();
        try (Cursor cur = TicketDb.get(c).getReadableDatabase().query("equipment",
                new String[]{"_id", "client", "type", "model", "serial", "mac", "warranty", "notes", "created"},
                "client=?", new String[]{client}, null, null, "created")) {
            while (cur.moveToNext()) {
                Item it = new Item();
                it.id = cur.getLong(0);
                it.client = s(cur, 1);
                it.type = s(cur, 2);
                it.model = s(cur, 3);
                it.serial = s(cur, 4);
                it.mac = s(cur, 5);
                it.warranty = cur.getLong(6);
                it.notes = s(cur, 7);
                it.created = cur.getLong(8);
                out.add(it);
            }
        }
        return out;
    }

    private static String s(Cursor c, int i) {
        String v = c.getString(i);
        return v == null ? "" : v;
    }

    static void save(Context c, Item it) {
        ContentValues v = new ContentValues();
        v.put("client", it.client);
        v.put("type", it.type);
        v.put("model", it.model);
        v.put("serial", it.serial);
        v.put("mac", it.mac);
        v.put("warranty", it.warranty);
        v.put("notes", it.notes);
        if (it.created == 0) it.created = System.currentTimeMillis();
        v.put("created", it.created);
        if (it.id == 0) it.id = TicketDb.get(c).getWritableDatabase().insert("equipment", null, v);
        else TicketDb.get(c).getWritableDatabase().update("equipment", v, "_id=?", new String[]{String.valueOf(it.id)});
    }

    static void delete(Context c, long id) {
        TicketDb.get(c).getWritableDatabase().delete("equipment", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Tickets of this client that mention the item's serial, MAC or model. */
    static List<TicketDb.Ticket> history(Context c, Item it) {
        List<TicketDb.Ticket> out = new ArrayList<>();
        String mac = TicketDb.formatMac(it.mac);
        for (TicketDb.Ticket t : TicketDb.get(c).all()) {
            if (!t.clientName.equalsIgnoreCase(it.client)) continue;
            String hay = (t.equipment + " " + t.mac + " " + t.description + " " + t.title).toLowerCase();
            boolean hit = (!it.serial.isEmpty() && hay.contains(it.serial.toLowerCase()))
                    || (!mac.isEmpty() && TicketDb.formatMac(t.mac).equalsIgnoreCase(mac))
                    || (!it.model.isEmpty() && hay.contains(it.model.toLowerCase()));
            if (hit) out.add(t);
        }
        return out;
    }

    // ------------------------------------------------------------------ UI for the client card

    /** Fills the section in the client card. */
    static void show(final Activity a, final LinearLayout box, final String client, final String phone) {
        box.removeAllViews();
        List<Item> items = forClient(a, client);
        box.addView(Ui.sectionTitle(a, "\uD83E\uDDF0 \u05E6\u05D9\u05D5\u05D3 \u05D0\u05E6\u05DC \u05D4\u05DC\u05E7\u05D5\u05D7 (" + items.size() + ")"));
        LinearLayout card = Ui.card(a);
        for (final Item it : items) {
            TextView tv = Ui.text(a, it.line(), 15, Ui.TEXT, false);
            tv.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
            tv.setOnClickListener(v -> options(a, box, client, phone, it));
            card.addView(tv);
        }
        TextView add = Ui.text(a, "\u2795 \u05D4\u05D5\u05E1\u05E3 \u05E6\u05D9\u05D5\u05D3", 15, Ui.PRIMARY, true);
        add.setPadding(0, Ui.dp(a, 10), 0, Ui.dp(a, 6));
        add.setOnClickListener(v -> {
            Item it = new Item();
            it.client = client;
            edit(a, it, () -> show(a, box, client, phone));
        });
        card.addView(add);
        box.addView(card);
    }

    private static void options(final Activity a, final LinearLayout box, final String client, final String phone, final Item it) {
        final List<TicketDb.Ticket> hist = history(a, it);
        String[] opts = {"\u270F\uFE0F \u05E2\u05E8\u05D9\u05DB\u05D4", "\uD83D\uDCDD \u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4 \u05E2\u05DC \u05D4\u05E4\u05E8\u05D9\u05D8", "\uD83D\uDD58 \u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D9\u05EA \u05EA\u05E7\u05DC\u05D5\u05EA (" + hist.size() + ")"};
        new AlertDialog.Builder(a)
                .setTitle(it.type + (it.model.isEmpty() ? "" : " \u2022 " + it.model))
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        edit(a, it, () -> show(a, box, client, phone));
                    } else if (w == 1) {
                        String eq = (it.type.replaceAll("^\\S+\\s", "") + " " + it.model + (it.serial.isEmpty() ? "" : " S/N " + it.serial)).trim();
                        a.startActivity(new Intent(a, TicketActivity.class)
                                .putExtra(TicketActivity.EXTRA_NAME, client)
                                .putExtra(TicketActivity.EXTRA_PHONE, phone)
                                .putExtra(TicketActivity.EXTRA_EQUIP, eq)
                                .putExtra(TicketActivity.EXTRA_MAC, it.mac));
                    } else {
                        if (hist.isEmpty()) {
                            android.widget.Toast.makeText(a, "\u05D0\u05D9\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E7\u05D5\u05D3\u05DE\u05D5\u05EA \u05E2\u05DC \u05D4\u05E4\u05E8\u05D9\u05D8", android.widget.Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] rows = new String[hist.size()];
                        for (int k = 0; k < hist.size(); k++) {
                            TicketDb.Ticket t = hist.get(k);
                            rows[k] = "#" + t.id + " \u2022 " + TicketDb.fmt(t.created).substring(0, 10) + " \u2022 " + He.status(t.status) + "\n" + t.title;
                        }
                        new AlertDialog.Builder(a).setTitle("\u05D4\u05D9\u05E1\u05D8\u05D5\u05E8\u05D9\u05D9\u05EA \u05EA\u05E7\u05DC\u05D5\u05EA").setItems(rows, (d2, w2) ->
                                a.startActivity(new Intent(a, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, hist.get(w2).id))).show();
                    }
                })
                .show();
    }

    static void edit(final Activity a, final Item it, final Runnable after) {
        LinearLayout lay = new LinearLayout(a);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(a, 16), Ui.dp(a, 8), Ui.dp(a, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final TextView type = Ui.text(a, it.type.isEmpty() ? "\u05D1\u05D7\u05E8 \u05E1\u05D5\u05D2 \u05E6\u05D9\u05D5\u05D3 \u25BE" : it.type + " \u25BE", 16, Ui.PRIMARY, true);
        type.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        final String[] chosen = {it.type};
        type.setOnClickListener(v -> new AlertDialog.Builder(a).setItems(TYPES, (d, w) -> {
            chosen[0] = TYPES[w];
            type.setText(TYPES[w] + " \u25BE");
        }).show());
        lay.addView(type);
        final EditText model = field(a, lay, "\u05D3\u05D2\u05DD / \u05D9\u05E6\u05E8\u05DF", it.model, InputType.TYPE_CLASS_TEXT);
        final EditText serial = field(a, lay, "\u05DE\u05E1\u05E4\u05E8 \u05E1\u05D9\u05D3\u05D5\u05E8\u05D9", it.serial, InputType.TYPE_CLASS_TEXT);
        LinearLayout scanRow = new LinearLayout(a);
        final EditText mac = field(a, lay, "MAC", it.mac, InputType.TYPE_CLASS_TEXT);
        TextView scan = Ui.text(a, "\uD83D\uDCF7 \u05E1\u05E8\u05D5\u05E7 \u05D1\u05E8\u05E7\u05D5\u05D3 \u05DC\u05DE\u05E1\u05E4\u05E8 \u05D4\u05E1\u05D9\u05D3\u05D5\u05E8\u05D9", 14, Ui.PRIMARY, false);
        scan.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        scan.setOnClickListener(v -> Scanner.scan(a, code -> serial.setText(code)));
        lay.addView(scan);
        TextView scan2 = Ui.text(a, "\uD83D\uDCF7 \u05E1\u05E8\u05D5\u05E7 \u05D1\u05E8\u05E7\u05D5\u05D3 \u05DC-MAC", 14, Ui.PRIMARY, false);
        scan2.setPadding(0, Ui.dp(a, 6), 0, Ui.dp(a, 6));
        scan2.setOnClickListener(v -> Scanner.scan(a, code -> mac.setText(TicketDb.formatMac(code))));
        lay.addView(scan2);
        final long[] warranty = {it.warranty};
        final TextView w = Ui.text(a, warranty[0] > 0 ? "\u05D0\u05D7\u05E8\u05D9\u05D5\u05EA \u05E2\u05D3 " + TicketDb.fmt(warranty[0]).substring(0, 10) + " \u25BE" : "\u05EA\u05D0\u05E8\u05D9\u05DA \u05E1\u05D9\u05D5\u05DD \u05D0\u05D7\u05E8\u05D9\u05D5\u05EA \u25BE", 15, Ui.PRIMARY, false);
        w.setPadding(0, Ui.dp(a, 8), 0, Ui.dp(a, 8));
        w.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            if (warranty[0] > 0) cal.setTimeInMillis(warranty[0]);
            else cal.add(Calendar.YEAR, 1);
            new DatePickerDialog(a, (dp, y, m, d) -> {
                Calendar c2 = Calendar.getInstance();
                c2.set(y, m, d, 12, 0, 0);
                warranty[0] = c2.getTimeInMillis();
                w.setText("\u05D0\u05D7\u05E8\u05D9\u05D5\u05EA \u05E2\u05D3 " + TicketDb.fmt(warranty[0]).substring(0, 10) + " \u25BE");
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        lay.addView(w);
        final EditText notes = field(a, lay, "\u05D4\u05E2\u05E8\u05D5\u05EA", it.notes, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        ScrollView sv = new ScrollView(a);
        sv.addView(lay);
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle(it.id == 0 ? "\u05E6\u05D9\u05D5\u05D3 \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05E6\u05D9\u05D5\u05D3")
                .setView(sv)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, x) -> {
                    it.type = chosen[0];
                    it.model = model.getText().toString().trim();
                    it.serial = serial.getText().toString().trim();
                    it.mac = mac.getText().toString().trim();
                    it.notes = notes.getText().toString().trim();
                    it.warranty = warranty[0];
                    save(a, it);
                    if (after != null) after.run();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (it.id > 0) b.setNeutralButton("\u05DE\u05D7\u05E7", (d, x) -> Auth.confirm(a, "\u05DE\u05D7\u05D9\u05E7\u05EA \u05D4\u05E6\u05D9\u05D5\u05D3", () -> {
            delete(a, it.id);
            if (after != null) after.run();
        }));
        b.show();
    }

    private static EditText field(Activity a, LinearLayout lay, String hint, String val, int type) {
        lay.addView(Ui.text(a, hint, 12, Ui.MUTED, false));
        EditText e = new EditText(a);
        e.setInputType(type);
        e.setText(val);
        lay.addView(e);
        return e;
    }
}
