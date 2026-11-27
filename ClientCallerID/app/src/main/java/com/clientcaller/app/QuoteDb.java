package com.clientcaller.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Price list (catalog) and price quotes, stored on the phone (quotes.db). */
final class QuoteDb extends SQLiteOpenHelper {
    static final double VAT = 0.18;
    static final String[] STATUSES = {"Draft", "Sent", "Approved", "Rejected"};
    static final String[] STATUS_HE = {"\u05D8\u05D9\u05D5\u05D8\u05D4", "\u05E0\u05E9\u05DC\u05D7\u05D4", "\u05D0\u05D5\u05E9\u05E8\u05D4", "\u05E0\u05D3\u05D7\u05EA\u05D4"};

    static String statusHe(String k) {
        for (int i = 0; i < STATUSES.length; i++) if (STATUSES[i].equals(k)) return STATUS_HE[i];
        return k;
    }

    static final class Item {
        long id;
        String name = "", unit = "", category = "";
        double price;
    }

    static final class Line {
        String name = "";
        double qty = 1, price;

        double total() {
            return qty * price;
        }
    }

    static final class Quote {
        long id, ticketId, created, updated, followupAt;
        String clientName = "", clientPhone = "", clientEmail = "", title = "", notes = "", status = "Draft";
        int validDays = 30;
        double discountPct;
        final List<Line> lines = new ArrayList<>();

        double subtotal() {
            double s = 0;
            for (Line l : lines) s += l.total();
            return round(s);
        }

        double discount() {
            return round(subtotal() * discountPct / 100.0);
        }

        double net() {
            return round(subtotal() - discount());
        }

        double vat() {
            return round(net() * VAT);
        }

        double total() {
            return round(net() + vat());
        }

        boolean isOpen() {
            return "Draft".equals(status) || "Sent".equals(status);
        }
    }

    static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    static String money(double v) {
        return "\u20AA" + String.format(Locale.US, "%,.2f", v);
    }

    static String qty(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.2f", v);
    }

    private static QuoteDb instance;

    static synchronized QuoteDb get(Context c) {
        if (instance == null) instance = new QuoteDb(c.getApplicationContext());
        return instance;
    }

    private QuoteDb(Context c) {
        super(c, "quotes.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE catalog (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, price REAL, unit TEXT, category TEXT)");
        db.execSQL("CREATE TABLE quotes (_id INTEGER PRIMARY KEY AUTOINCREMENT, client_name TEXT, client_phone TEXT,"
                + " client_email TEXT, ticket_id INTEGER, title TEXT, notes TEXT, status TEXT, valid_days INTEGER,"
                + " discount REAL, created INTEGER, updated INTEGER, followup_at INTEGER)");
        db.execSQL("CREATE TABLE quote_lines (_id INTEGER PRIMARY KEY AUTOINCREMENT, quote_id INTEGER, pos INTEGER,"
                + " name TEXT, qty REAL, price REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    private static String str(Cursor c, int i) {
        String s = c.getString(i);
        return s == null ? "" : s;
    }

    // ------------------------------------------------------------------ catalog

    List<Item> catalog() {
        List<Item> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("catalog", new String[]{"_id", "name", "price", "unit", "category"},
                null, null, null, null, "category COLLATE NOCASE, name COLLATE NOCASE")) {
            while (c.moveToNext()) {
                Item it = new Item();
                it.id = c.getLong(0);
                it.name = str(c, 1);
                it.price = c.getDouble(2);
                it.unit = str(c, 3);
                it.category = str(c, 4);
                out.add(it);
            }
        }
        return out;
    }

    void saveItem(Item it) {
        ContentValues v = new ContentValues();
        v.put("name", it.name);
        v.put("price", it.price);
        v.put("unit", it.unit);
        v.put("category", it.category);
        SQLiteDatabase db = getWritableDatabase();
        if (it.id == 0) it.id = db.insert("catalog", null, v);
        else db.update("catalog", v, "_id=?", new String[]{String.valueOf(it.id)});
    }

    void deleteItem(long id) {
        getWritableDatabase().delete("catalog", "_id=?", new String[]{String.valueOf(id)});
    }

    void clearCatalog() {
        getWritableDatabase().delete("catalog", null, null);
    }

    // ------------------------------------------------------------------ quotes

    private static final String[] QCOLS = {"_id", "client_name", "client_phone", "client_email", "ticket_id", "title",
            "notes", "status", "valid_days", "discount", "created", "updated", "followup_at"};

    private Quote readQuote(Cursor c, boolean withLines) {
        Quote q = new Quote();
        q.id = c.getLong(0);
        q.clientName = str(c, 1);
        q.clientPhone = str(c, 2);
        q.clientEmail = str(c, 3);
        q.ticketId = c.getLong(4);
        q.title = str(c, 5);
        q.notes = str(c, 6);
        q.status = str(c, 7).isEmpty() ? "Draft" : str(c, 7);
        q.validDays = c.getInt(8);
        q.discountPct = c.getDouble(9);
        q.created = c.getLong(10);
        q.updated = c.getLong(11);
        q.followupAt = c.getLong(12);
        if (withLines) loadLines(q);
        return q;
    }

    private void loadLines(Quote q) {
        q.lines.clear();
        try (Cursor c = getReadableDatabase().query("quote_lines", new String[]{"name", "qty", "price"},
                "quote_id=?", new String[]{String.valueOf(q.id)}, null, null, "pos")) {
            while (c.moveToNext()) {
                Line l = new Line();
                l.name = str(c, 0);
                l.qty = c.getDouble(1);
                l.price = c.getDouble(2);
                q.lines.add(l);
            }
        }
    }

    Quote load(long id) {
        try (Cursor c = getReadableDatabase().query("quotes", QCOLS, "_id=?", new String[]{String.valueOf(id)},
                null, null, null)) {
            return c.moveToFirst() ? readQuote(c, true) : null;
        }
    }

    List<Quote> all() {
        List<Quote> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("quotes", QCOLS, null, null, null, null, "updated DESC")) {
            while (c.moveToNext()) out.add(readQuote(c, true));
        }
        return out;
    }

    void save(Quote q) {
        long now = System.currentTimeMillis();
        if (q.created == 0) q.created = now;
        q.updated = now;
        ContentValues v = new ContentValues();
        v.put("client_name", q.clientName);
        v.put("client_phone", q.clientPhone);
        v.put("client_email", q.clientEmail);
        v.put("ticket_id", q.ticketId);
        v.put("title", q.title);
        v.put("notes", q.notes);
        v.put("status", q.status);
        v.put("valid_days", q.validDays);
        v.put("discount", q.discountPct);
        v.put("created", q.created);
        v.put("updated", q.updated);
        v.put("followup_at", q.followupAt);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (q.id == 0) q.id = db.insert("quotes", null, v);
            else db.update("quotes", v, "_id=?", new String[]{String.valueOf(q.id)});
            db.delete("quote_lines", "quote_id=?", new String[]{String.valueOf(q.id)});
            for (int i = 0; i < q.lines.size(); i++) {
                Line l = q.lines.get(i);
                ContentValues lv = new ContentValues();
                lv.put("quote_id", q.id);
                lv.put("pos", i);
                lv.put("name", l.name);
                lv.put("qty", l.qty);
                lv.put("price", l.price);
                db.insert("quote_lines", null, lv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void delete(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("quote_lines", "quote_id=?", new String[]{String.valueOf(id)});
        db.delete("quotes", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Text version of the quote (WhatsApp / email body). */
    static String text(Context c, Quote q) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 \u05DE\u05E1' ").append(q.id);
        if (!q.title.isEmpty()) sb.append(" - ").append(q.title);
        sb.append("\n\u05DC\u05DB\u05D1\u05D5\u05D3: ").append(q.clientName).append('\n');
        sb.append("\u05EA\u05D0\u05E8\u05D9\u05DA: ").append(TicketDb.fmt(q.created).substring(0, 10)).append("\n\n");
        int n = 1;
        for (Line l : q.lines) {
            sb.append(n++).append(". ").append(l.name).append(" - ").append(qty(l.qty)).append(" \u00D7 ")
                    .append(money(l.price)).append(" = ").append(money(l.total())).append('\n');
        }
        sb.append("\n\u05E1\u05DB\u05D5\u05DD \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE: ").append(money(q.subtotal()));
        if (q.discountPct > 0) sb.append("\n\u05D4\u05E0\u05D7\u05D4 ").append(qty(q.discountPct)).append("%: -").append(money(q.discount()));
        sb.append("\n\u05DE\u05E2\"\u05DE 18%: ").append(money(q.vat()));
        sb.append("\n\u05E1\u05D4\"\u05DB \u05DC\u05EA\u05E9\u05DC\u05D5\u05DD: ").append(money(q.total()));
        sb.append("\n\n\u05D4\u05D4\u05E6\u05E2\u05D4 \u05D1\u05EA\u05D5\u05E7\u05E3 \u05DC-").append(q.validDays).append(" \u05D9\u05D5\u05DD.");
        if (!q.notes.isEmpty()) sb.append("\n\n").append(q.notes);
        sb.append("\n\n\u05DC\u05D0\u05D9\u05E9\u05D5\u05E8 \u05D4\u05D4\u05E6\u05E2\u05D4 \u05D0\u05E4\u05E9\u05E8 \u05E4\u05E9\u05D5\u05D8 \u05DC\u05D4\u05E9\u05D9\u05D1 \u05DC\u05D4\u05D5\u05D3\u05E2\u05D4 \u05D6\u05D5: \"\u05DE\u05D0\u05E9\u05E8/\u05EA\".");
        sb.append("\n\n").append(Issuer.signature(c));
        return sb.toString();
    }
}
