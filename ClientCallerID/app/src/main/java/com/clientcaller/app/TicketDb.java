package com.clientcaller.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Service tickets and their notes, stored on the phone (SQLite). */
final class TicketDb extends SQLiteOpenHelper {

    static final String[] STATUSES = {
            "Open", "In progress", "Waiting for client", "Waiting for outsourcing", "Closed"};
    static final String[] PRIORITIES = {"Low", "Normal", "Urgent"};
    static final String[] CATEGORIES = {
            "Hardware", "Software", "Network", "Printer", "Email", "Backup", "Security", "Other"};

    static final class Ticket {
        long id;
        String clientName = "", clientPhone = "", title = "", description = "";
        String status = "Open", priority = "Normal", category = "Other";
        String equipment = "", outsource = "", provider = "", mac = "", assignee = "", outStatus = "";
        long created, updated, followupAt, eventId;

        boolean isOpen() {
            return !"Closed".equals(status);
        }

        boolean isOverdue() {
            return isOpen() && followupAt > 0 && followupAt < System.currentTimeMillis();
        }
    }

    static final class Provider {
        long id;
        String name = "", phone = "", email = "";
    }

    static final String[] DEFAULT_PROVIDERS = {
            "\u05D4\u05D5\u05D8-\u05E9\u05D9\u05E8\u05D5\u05EA \u05DE\u05D5\u05E1\u05D3\u05D9", "\u05E7\u05D5\u05DE\u05E4\u05D1\u05D9\u05D6", "\u05D4\u05D5\u05E8\u05D9\u05D9\u05D6\u05D5\u05DF \u05E6\u05D7\u05D9", "\u05D4\u05D5\u05E8\u05D9\u05D9\u05D6\u05D5\u05DF-\u05D0\u05D5\u05E8\u05DF", "\u05DC\u05D9\u05D0\u05EA-\u05DE\u05D8\"\u05DC", "\u05D9\u05D5\u05D0\u05D1-\u05EA\u05E9\u05EA\u05D9\u05D5\u05EA"};

    static final class Note {
        long id;
        long time;
        String text;
    }

    private static TicketDb instance;

    static synchronized TicketDb get(Context c) {
        if (instance == null) instance = new TicketDb(c.getApplicationContext());
        return instance;
    }

    private TicketDb(Context c) {
        super(c, "tickets.db", null, 10);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tickets (_id INTEGER PRIMARY KEY AUTOINCREMENT, client_name TEXT,"
                + " client_phone TEXT, title TEXT, description TEXT, status TEXT, priority TEXT,"
                + " category TEXT, equipment TEXT, outsource TEXT, created INTEGER, updated INTEGER,"
                + " followup_at INTEGER, event_id INTEGER, provider TEXT, mac TEXT, assignee TEXT, out_status TEXT)");
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, ticket_id INTEGER,"
                + " time INTEGER, text TEXT)");
        createProviders(db);
        createSent(db);
        createItems(db);
        createMore(db);
        Tasks.create(db);
    }

    private static void createSent(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS sent (_id INTEGER PRIMARY KEY AUTOINCREMENT, ticket_id INTEGER,"
                + " client_name TEXT, client_phone TEXT, time INTEGER, to_addr TEXT, cc_addr TEXT,"
                + " subject TEXT, body TEXT)");
    }

    private static void createProviders(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE providers (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phone TEXT, email TEXT)");
        for (String n : DEFAULT_PROVIDERS) {
            ContentValues v = new ContentValues();
            v.put("name", n);
            v.put("phone", "");
            v.put("email", "");
            db.insert("providers", null, v);
        }
    }

    private static void createMore(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS equipment (_id INTEGER PRIMARY KEY AUTOINCREMENT, client TEXT, type TEXT,"
                + " model TEXT, serial TEXT, mac TEXT, warranty INTEGER, notes TEXT, created INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS recurring (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, client TEXT,"
                + " phone TEXT, category TEXT, every_days INTEGER, next_at INTEGER, active INTEGER)");
    }

    private static void createItems(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS items (_id INTEGER PRIMARY KEY AUTOINCREMENT, ticket_id INTEGER, kind TEXT,"
                + " name TEXT, qty REAL, price REAL, created INTEGER)");
    }

    /** A line of work time or material used in a ticket. */
    static final class Item {
        long id, ticketId, created;
        String kind = "part", name = "";
        double qty = 1, price;

        double total() {
            return qty * price;
        }
    }

    java.util.List<Item> items(long ticketId) {
        java.util.List<Item> out = new java.util.ArrayList<>();
        try (Cursor c = getReadableDatabase().query("items", new String[]{"_id", "kind", "name", "qty", "price", "created"},
                "ticket_id=?", new String[]{String.valueOf(ticketId)}, null, null, "created")) {
            while (c.moveToNext()) {
                Item it = new Item();
                it.id = c.getLong(0);
                it.ticketId = ticketId;
                it.kind = str(c, 1);
                it.name = str(c, 2);
                it.qty = c.getDouble(3);
                it.price = c.getDouble(4);
                it.created = c.getLong(5);
                out.add(it);
            }
        }
        return out;
    }

    void saveItem(Item it) {
        ContentValues v = new ContentValues();
        v.put("ticket_id", it.ticketId);
        v.put("kind", it.kind);
        v.put("name", it.name);
        v.put("qty", it.qty);
        v.put("price", it.price);
        if (it.created == 0) it.created = System.currentTimeMillis();
        v.put("created", it.created);
        if (it.id == 0) it.id = getWritableDatabase().insert("items", null, v);
        else getWritableDatabase().update("items", v, "_id=?", new String[]{String.valueOf(it.id)});
    }

    void deleteItem(long id) {
        getWritableDatabase().delete("items", "_id=?", new String[]{String.valueOf(id)});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) createSent(db);
        if (oldVersion < 5) db.execSQL("ALTER TABLE tickets ADD COLUMN assignee TEXT");
        if (oldVersion < 6) db.execSQL("ALTER TABLE tickets ADD COLUMN out_status TEXT");
        if (oldVersion < 7) createItems(db);
        if (oldVersion < 8) createMore(db);
        if (oldVersion < 9) Tasks.create(db);
        if (oldVersion < 10 && oldVersion >= 9) Tasks.addPlace(db);
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tickets ADD COLUMN provider TEXT");
            createProviders(db);
        }
        if (oldVersion < 3 && oldVersion >= 2) {
            db.execSQL("ALTER TABLE tickets ADD COLUMN mac TEXT");
        } else if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tickets ADD COLUMN mac TEXT");
        }
    }

    private static final String[] COLS = {"_id", "client_name", "client_phone", "title", "description",
            "status", "priority", "category", "equipment", "outsource", "created", "updated",
            "followup_at", "event_id", "provider", "mac", "assignee", "out_status"};

    private static Ticket read(Cursor c) {
        Ticket t = new Ticket();
        t.id = c.getLong(0);
        t.clientName = str(c, 1);
        t.clientPhone = str(c, 2);
        t.title = str(c, 3);
        t.description = str(c, 4);
        t.status = str(c, 5);
        t.priority = str(c, 6);
        t.category = str(c, 7);
        t.equipment = str(c, 8);
        t.outsource = str(c, 9);
        t.created = c.getLong(10);
        t.updated = c.getLong(11);
        t.followupAt = c.getLong(12);
        t.eventId = c.getLong(13);
        t.provider = str(c, 14);
        t.mac = str(c, 15);
        t.assignee = str(c, 16);
        t.outStatus = str(c, 17);
        return t;
    }

    private static String str(Cursor c, int i) {
        String s = c.getString(i);
        return s == null ? "" : s;
    }

    long save(Ticket t) {
        long now = System.currentTimeMillis();
        if (t.created == 0) t.created = now;
        t.updated = now;
        ContentValues v = new ContentValues();
        v.put("client_name", t.clientName);
        v.put("client_phone", t.clientPhone);
        v.put("title", t.title);
        v.put("description", t.description);
        v.put("status", t.status);
        v.put("priority", t.priority);
        v.put("category", t.category);
        v.put("equipment", t.equipment);
        v.put("outsource", t.outsource);
        v.put("created", t.created);
        v.put("updated", t.updated);
        v.put("followup_at", t.followupAt);
        v.put("event_id", t.eventId);
        v.put("provider", t.provider);
        v.put("mac", t.mac);
        v.put("assignee", t.assignee);
        v.put("out_status", t.outStatus);
        SQLiteDatabase db = getWritableDatabase();
        if (t.id == 0) {
            t.id = db.insert("tickets", null, v);
        } else {
            db.update("tickets", v, "_id=?", new String[]{String.valueOf(t.id)});
        }
        return t.id;
    }

    Ticket load(long id) {
        try (Cursor c = getReadableDatabase().query("tickets", COLS, "_id=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    List<Ticket> all() {
        List<Ticket> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("tickets", COLS, null, null, null, null,
                "updated DESC")) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    void delete(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("notes", "ticket_id=?", new String[]{String.valueOf(id)});
        db.delete("tickets", "_id=?", new String[]{String.valueOf(id)});
    }

    void deleteNote(long noteId) {
        getWritableDatabase().delete("notes", "_id=?", new String[]{String.valueOf(noteId)});
    }

    void addNote(long ticketId, String text) {
        ContentValues v = new ContentValues();
        v.put("ticket_id", ticketId);
        v.put("time", System.currentTimeMillis());
        v.put("text", text);
        getWritableDatabase().insert("notes", null, v);
    }

    List<Note> notes(long ticketId) {
        List<Note> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("notes", new String[]{"time", "text", "_id"},
                "ticket_id=?", new String[]{String.valueOf(ticketId)}, null, null, "time ASC")) {
            while (c.moveToNext()) {
                Note n = new Note();
                n.time = c.getLong(0);
                n.text = str(c, 1);
                n.id = c.getLong(2);
                out.add(n);
            }
        }
        return out;
    }

    /** Tickets that belong to a client, matched by phone number or name. */
    List<Ticket> forClient(String name, List<String> phoneKeys) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : all()) {
            String k = ClientStore.key(t.clientPhone);
            boolean byPhone = k.length() >= 7 && phoneKeys.contains(k);
            boolean byName = name != null && !name.isEmpty() && name.equalsIgnoreCase(t.clientName.trim());
            if (byPhone || byName) out.add(t);
        }
        return out;
    }

    // ------------------------------------------------------------------ outsourcing providers

    List<Provider> providers() {
        List<Provider> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("providers", new String[]{"_id", "name", "phone", "email"},
                null, null, null, null, "name COLLATE NOCASE")) {
            while (c.moveToNext()) {
                Provider p = new Provider();
                p.id = c.getLong(0);
                p.name = str(c, 1);
                p.phone = str(c, 2);
                p.email = str(c, 3);
                out.add(p);
            }
        }
        return out;
    }

    Provider providerByName(String name) {
        for (Provider p : providers()) if (p.name.equals(name)) return p;
        return null;
    }

    void saveProvider(Provider p) {
        ContentValues v = new ContentValues();
        v.put("name", p.name);
        v.put("phone", p.phone);
        v.put("email", p.email);
        SQLiteDatabase db = getWritableDatabase();
        if (p.id == 0) p.id = db.insert("providers", null, v);
        else db.update("providers", v, "_id=?", new String[]{String.valueOf(p.id)});
    }

    void deleteProvider(long id) {
        getWritableDatabase().delete("providers", "_id=?", new String[]{String.valueOf(id)});
    }

    /** "aabb.ccdd.eeff", "AA-BB-CC-DD-EE-FF" ... -> "AA:BB:CC:DD:EE:FF". Other text is kept as typed. */
    static String formatMac(String raw) {
        String hex = raw == null ? "" : raw.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        if (hex.length() != 12) return raw == null ? "" : raw.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) sb.append(':');
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    static boolean isValidMac(String mac) {
        return mac.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}");
    }

    /** MAC addresses already recorded in this client's tickets, with the equipment they belong to. */
    List<String[]> macsForClient(String name, String phone) {
        List<String> keys = new ArrayList<>();
        String k = ClientStore.key(phone);
        if (k.length() >= 7) keys.add(k);
        List<String[]> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Ticket t : forClient(name, keys)) {
            if (t.mac.isEmpty() || seen.contains(t.mac)) continue;
            seen.add(t.mac);
            out.add(new String[]{t.mac, t.equipment});
        }
        return out;
    }

    // ------------------------------------------------------------------ sent emails

    static final class Sent {
        long id, ticketId, time;
        String clientName = "", clientPhone = "", to = "", cc = "", subject = "", body = "";
    }

    void addSent(Sent m) {
        ContentValues v = new ContentValues();
        v.put("ticket_id", m.ticketId);
        v.put("client_name", m.clientName);
        v.put("client_phone", m.clientPhone);
        v.put("time", System.currentTimeMillis());
        v.put("to_addr", m.to);
        v.put("cc_addr", m.cc);
        v.put("subject", m.subject);
        v.put("body", m.body);
        getWritableDatabase().insert("sent", null, v);
    }

    void deleteSent(long id) {
        getWritableDatabase().delete("sent", "_id=?", new String[]{String.valueOf(id)});
    }

    /** Emails sent about this client (matched by phone or name), newest first. */
    List<Sent> sentForClient(String name, List<String> phoneKeys) {
        List<Sent> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("sent", new String[]{"_id", "ticket_id", "client_name",
                "client_phone", "time", "to_addr", "cc_addr", "subject", "body"}, null, null, null, null, "time DESC")) {
            while (c.moveToNext()) {
                Sent m = new Sent();
                m.id = c.getLong(0);
                m.ticketId = c.getLong(1);
                m.clientName = str(c, 2);
                m.clientPhone = str(c, 3);
                m.time = c.getLong(4);
                m.to = str(c, 5);
                m.cc = str(c, 6);
                m.subject = str(c, 7);
                m.body = str(c, 8);
                String k = ClientStore.key(m.clientPhone);
                boolean byPhone = k.length() >= 7 && phoneKeys.contains(k);
                boolean byName = name != null && !name.isEmpty() && name.equalsIgnoreCase(m.clientName.trim());
                if (byPhone || byName) out.add(m);
            }
        }
        return out;
    }

    static String fmt(long time) {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(time));
    }

    /** Plain-text summary for sharing (WhatsApp, email, outsourcing partner). */
    String summary(Ticket t) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u05E7\u05E8\u05D9\u05D0\u05EA \u05E9\u05D9\u05E8\u05D5\u05EA #").append(t.id).append('\n');
        if (!t.clientName.isEmpty()) sb.append("\u05DC\u05E7\u05D5\u05D7: ").append(t.clientName).append('\n');
        if (!t.clientPhone.isEmpty()) sb.append("\u05D8\u05DC\u05E4\u05D5\u05DF: ").append(t.clientPhone).append('\n');
        sb.append("\u05E0\u05D5\u05E9\u05D0: ").append(t.title).append('\n');
        if (!t.description.isEmpty()) sb.append("\u05EA\u05D9\u05D0\u05D5\u05E8 \u05D4\u05EA\u05E7\u05DC\u05D4: ").append(t.description).append('\n');
        if (!t.equipment.isEmpty()) sb.append("\u05E6\u05D9\u05D5\u05D3: ").append(t.equipment).append('\n');
        if (!t.mac.isEmpty()) sb.append("\u05DB\u05EA\u05D5\u05D1\u05EA MAC: ").append(t.mac).append('\n');
        if (!t.provider.isEmpty()) sb.append("\u05E0\u05D5\u05EA\u05DF \u05E9\u05D9\u05E8\u05D5\u05EA: ").append(t.provider).append('\n');
        if (!t.outsource.isEmpty()) sb.append("\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5: ").append(t.outsource).append('\n');
        sb.append("\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4: ").append(He.category(t.category)).append(" | \u05D3\u05D7\u05D9\u05E4\u05D5\u05EA: ").append(He.priority(t.priority))
                .append(" | \u05E1\u05D8\u05D8\u05D5\u05E1: ").append(He.status(t.status)).append('\n');
        if (t.followupAt > 0) sb.append("\u05DE\u05E2\u05E7\u05D1: ").append(fmt(t.followupAt)).append('\n');
        sb.append("\u05E0\u05E4\u05EA\u05D7\u05D4: ").append(fmt(t.created)).append('\n');
        List<Note> notes = notes(t.id);
        if (!notes.isEmpty()) {
            sb.append("\n\u05D4\u05E2\u05E8\u05D5\u05EA:\n");
            for (Note n : notes) sb.append(fmt(n.time)).append(" - ").append(n.text).append('\n');
        }
        return sb.toString();
    }
}
