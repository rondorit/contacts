package com.clientcaller.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Two-way sync of service tickets, their notes and tasks with Firestore.
 * The manager's phone shares everything; a technician's phone shares only what is assigned to him.
 * Client lists are never uploaded \u2013 only the name, phone and address that a ticket needs.
 */
final class Sync {
    static Context app;
    private static boolean applying;
    private static final List<ListenerRegistration> regs = new ArrayList<>();

    private Sync() { }

    private static SharedPreferences p() {
        return CallLogger.prefs(app);
    }

    static boolean on() {
        if (app == null) return false;
        try {
            return Cloud.user() != null && !p().getString("cloud_role", "").isEmpty() && !p().getString("cloud_org", "").isEmpty()
                    && p().getBoolean("cloud_sync", true);
        } catch (Throwable e) {
            return false;
        }
    }

    static boolean admin() {
        return Cloud.ROLE_ADMIN.equals(p().getString("cloud_role", ""));
    }

    private static String uid() {
        FirebaseUser u = Cloud.user();
        return u == null ? "" : u.getUid();
    }

    private static String myName() {
        FirebaseUser u = Cloud.user();
        return u == null ? "" : (u.getDisplayName() == null ? Cloud.email() : u.getDisplayName());
    }

    // ------------------------------------------------------------------ id mapping (local id <-> cloud doc id)

    private static String docOf(String kind, long localId) {
        return p().getString("l2c_" + kind + localId, null);
    }

    private static long localOf(String kind, String doc) {
        return p().getLong("c2l_" + kind + doc, 0);
    }

    private static void map(String kind, long localId, String doc) {
        p().edit().putString("l2c_" + kind + localId, doc).putLong("c2l_" + kind + doc, localId).apply();
    }

    // ------------------------------------------------------------------ cloud users (for assignment)

    static void cacheUsers(List<String[]> users) {
        JSONArray a = new JSONArray();
        try {
            for (String[] u : users) a.put(new JSONObject().put("uid", u[0]).put("name", u[1]).put("email", u[2]));
        } catch (Exception ignored) { }
        p().edit().putString("cloud_users", a.toString()).apply();
    }

    /** [uid, name, email] of every cloud team member. */
    static List<String[]> users(Context c) {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(CallLogger.prefs(c).getString("cloud_users", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new String[]{o.optString("uid"), o.optString("name"), o.optString("email")});
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static String uidOfName(String name) {
        if (name == null || name.isEmpty()) return "";
        for (String[] u : users(app)) if (u[1].equalsIgnoreCase(name) || u[2].equalsIgnoreCase(name)) return u[0];
        return "";
    }

    // ------------------------------------------------------------------ push (local -> cloud)

    static void ticketChanged(TicketDb.Ticket t) {
        if (applying || !on()) return;
        String doc = docOf("t", t.id);
        if (doc == null) {
            if (!admin()) return; // a technician shares only tickets that came from the cloud
            doc = "a" + t.id;
            map("t", t.id, doc);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("title", t.title);
        m.put("description", t.description);
        m.put("status", t.status);
        m.put("priority", t.priority);
        m.put("category", t.category);
        m.put("clientName", t.clientName);
        m.put("clientPhone", t.clientPhone);
        m.put("clientAddress", address(t));
        m.put("equipment", t.equipment);
        m.put("mac", t.mac);
        m.put("provider", t.provider);
        m.put("outStatus", t.outStatus);
        m.put("followupAt", t.followupAt);
        m.put("created", t.created);
        m.put("updated", System.currentTimeMillis());
        m.put("updatedBy", uid());
        m.put("assigneeName", t.assignee);
        String au = uidOfName(t.assignee);
        if (admin()) m.put("assigneeUid", au.isEmpty() ? "" : au);
        Cloud.col("tickets").document(doc).set(m, SetOptions.merge());
    }

    private static String address(TicketDb.Ticket t) {
        try {
            ClientStore s = Data.get(app);
            int row = t.clientPhone.isEmpty() ? -1 : s.find(t.clientPhone);
            if (row < 0) for (int r = 0; r < s.rows.size(); r++) if (s.name(r).equalsIgnoreCase(t.clientName)) { row = r; break; }
            return row >= 0 ? Nav.address(app, s, row) : "";
        } catch (Exception e) {
            return "";
        }
    }

    static void noteAdded(long ticketId, String text, long time) {
        if (applying || !on()) return;
        String doc = docOf("t", ticketId);
        if (doc == null) return;
        Map<String, Object> n = new HashMap<>();
        n.put("text", text);
        n.put("time", time);
        n.put("by", uid());
        n.put("byName", myName());
        Cloud.col("tickets").document(doc).collection("notes").add(n);
        Map<String, Object> bump = new HashMap<>();
        bump.put("updated", System.currentTimeMillis());
        bump.put("updatedBy", uid());
        bump.put("lastNote", text.length() > 120 ? text.substring(0, 120) : text);
        Cloud.col("tickets").document(doc).set(bump, SetOptions.merge());
    }

    static void ticketDeleted(long id) {
        if (applying || !on() || !admin()) return;
        String doc = docOf("t", id);
        if (doc != null) Cloud.col("tickets").document(doc).delete();
    }

    static void taskChanged(Tasks.Task t) {
        if (applying || !on()) return;
        String doc = docOf("k", t.id);
        if (doc == null) {
            if (!admin()) return;
            doc = "a" + t.id;
            map("k", t.id, doc);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("title", t.title);
        m.put("notes", t.notes);
        m.put("priority", t.priority);
        m.put("dueAt", t.dueAt);
        m.put("doneAt", t.doneAt);
        m.put("stage", t.stage);
        m.put("list", t.list);
        m.put("client", t.client);
        m.put("phone", t.phone);
        m.put("assigneeName", t.assignee);
        if (admin()) m.put("assigneeUid", uidOfName(t.assignee));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Tasks.Check k : t.items) {
            Map<String, Object> im = new HashMap<>();
            im.put("t", k.text);
            im.put("d", k.done);
            items.add(im);
        }
        m.put("items", items);
        m.put("updated", System.currentTimeMillis());
        m.put("updatedBy", uid());
        Cloud.col("tasks").document(doc).set(m, SetOptions.merge());
    }

    static void taskDeleted(long id) {
        if (applying || !on() || !admin()) return;
        String doc = docOf("k", id);
        if (doc != null) Cloud.col("tasks").document(doc).delete();
    }

    /** First time: share all existing tickets and tasks (manager only). */
    static void uploadAll(Context c) {
        if (!on() || !admin()) return;
        if (p().getBoolean("cloud_uploaded", false)) return;
        for (TicketDb.Ticket t : TicketDb.get(c).all()) {
            ticketChanged(t);
            for (TicketDb.Note n : TicketDb.get(c).notes(t.id)) noteAdded(t.id, n.text, n.time);
        }
        for (Tasks.Task k : Tasks.all(c)) taskChanged(k);
        p().edit().putBoolean("cloud_uploaded", true).apply();
    }

    // ------------------------------------------------------------------ pull (cloud -> local)

    /** Starts listening for changes made by others. Safe to call many times. */
    static synchronized void start(Context c) {
        app = c.getApplicationContext();
        if (!on()) return;
        for (ListenerRegistration r : regs) r.remove();
        regs.clear();
        Cloud.col("members").get().addOnSuccessListener(q -> {
            List<String[]> us = new ArrayList<>();
            for (QueryDocumentSnapshot d : q) us.add(new String[]{d.getId(), str(d, "name"), str(d, "email")});
            cacheUsers(us);
            uploadAll(app);
        });
        Query tq = Cloud.col("tickets");
        Query kq = Cloud.col("tasks");
        if (!admin()) {
            tq = tq.whereEqualTo("assigneeUid", uid());
            kq = kq.whereEqualTo("assigneeUid", uid());
        }
        regs.add(tq.addSnapshotListener((snap, err) -> {
            if (snap == null) return;
            for (DocumentChange ch : snap.getDocumentChanges()) {
                if (ch.getType() == DocumentChange.Type.REMOVED) continue;
                try { applyTicket(ch.getDocument()); } catch (Exception ignored) { }
            }
        }));
        regs.add(kq.addSnapshotListener((snap, err) -> {
            if (snap == null) return;
            for (DocumentChange ch : snap.getDocumentChanges()) {
                if (ch.getType() == DocumentChange.Type.REMOVED) continue;
                try { applyTask(ch.getDocument()); } catch (Exception ignored) { }
            }
        }));
    }

    static synchronized void stop() {
        for (ListenerRegistration r : regs) r.remove();
        regs.clear();
    }

    private static String str(DocumentSnapshot d, String k) {
        String v = d.getString(k);
        return v == null ? "" : v;
    }

    private static long num(DocumentSnapshot d, String k) {
        Long v = d.getLong(k);
        return v == null ? 0 : v;
    }

    private static void applyTicket(DocumentSnapshot d) {
        String doc = d.getId();
        long local = localOf("t", doc);
        long cloudUpdated = num(d, "updated");
        String seenKey = "tseen_" + doc;
        boolean mine = uid().equals(str(d, "updatedBy"));
        TicketDb db = TicketDb.get(app);
        TicketDb.Ticket t = local > 0 ? db.load(local) : null;
        if (t != null && (mine || cloudUpdated <= p().getLong(seenKey, 0))) {
            p().edit().putLong(seenKey, Math.max(cloudUpdated, p().getLong(seenKey, 0))).apply();
            return;
        }
        boolean isNew = t == null;
        if (isNew) {
            if (mine && local > 0) return;
            t = new TicketDb.Ticket();
        }
        String prevStatus = t.status;
        t.title = str(d, "title");
        t.description = str(d, "description");
        if (!str(d, "status").isEmpty()) t.status = str(d, "status");
        if (!str(d, "priority").isEmpty()) t.priority = str(d, "priority");
        if (!str(d, "category").isEmpty()) t.category = str(d, "category");
        t.clientName = str(d, "clientName");
        t.clientPhone = str(d, "clientPhone");
        t.equipment = str(d, "equipment");
        t.mac = str(d, "mac");
        t.provider = str(d, "provider");
        t.outStatus = str(d, "outStatus");
        t.followupAt = num(d, "followupAt");
        t.assignee = str(d, "assigneeName");
        applying = true;
        try {
            db.save(t);
            if (isNew) {
                map("t", t.id, doc);
                db.addNote(t.id, "\u2601\uFE0F \u05D4\u05EA\u05E7\u05D1\u05DC\u05D4 \u05DE\u05D4\u05E2\u05E0\u05DF");
            } else if (!prevStatus.equals(t.status)) {
                db.addNote(t.id, "\u2601\uFE0F \u05E1\u05D8\u05D8\u05D5\u05E1: " + He.status(prevStatus) + " \u2190 " + He.status(t.status));
            }
            try { Followups.schedule(app, t); } catch (Exception ignored) { }
        } finally {
            applying = false;
        }
        p().edit().putLong(seenKey, cloudUpdated).apply();
        pullNotes(doc, t.id);
        TicketsWidget.updateAll(app);
    }

    private static void pullNotes(final String doc, final long local) {
        final String key = "nseen_" + doc;
        long since = p().getLong(key, 0);
        Cloud.col("tickets").document(doc).collection("notes").whereGreaterThan("time", since).get()
                .addOnSuccessListener(q -> {
                    long max = p().getLong(key, 0);
                    applying = true;
                    try {
                        for (QueryDocumentSnapshot n : q) {
                            long time = num(n, "time");
                            max = Math.max(max, time);
                            if (uid().equals(str(n, "by"))) continue;
                            TicketDb.get(app).addNote(local, "\u2601\uFE0F " + str(n, "byName") + ": " + str(n, "text"));
                        }
                    } finally {
                        applying = false;
                    }
                    p().edit().putLong(key, max).apply();
                });
    }

    private static void applyTask(DocumentSnapshot d) {
        String doc = d.getId();
        long local = localOf("k", doc);
        long cloudUpdated = num(d, "updated");
        String seenKey = "kseen_" + doc;
        boolean mine = uid().equals(str(d, "updatedBy"));
        Tasks.Task t = local > 0 ? Tasks.load(app, local) : null;
        if (t != null && (mine || cloudUpdated <= p().getLong(seenKey, 0))) return;
        if (t == null) {
            if (mine && local > 0) return;
            t = new Tasks.Task();
        }
        t.title = str(d, "title");
        t.notes = str(d, "notes");
        if (!str(d, "priority").isEmpty()) t.priority = str(d, "priority");
        t.dueAt = num(d, "dueAt");
        t.doneAt = num(d, "doneAt");
        t.stage = (int) num(d, "stage");
        t.list = str(d, "list");
        t.client = str(d, "client");
        t.phone = str(d, "phone");
        t.assignee = str(d, "assigneeName");
        t.items.clear();
        Object items = d.get("items");
        if (items instanceof List) {
            for (Object o : (List<?>) items) {
                if (o instanceof Map) {
                    Map<?, ?> im = (Map<?, ?>) o;
                    t.items.add(new Tasks.Check(String.valueOf(im.get("t")), Boolean.TRUE.equals(im.get("d"))));
                }
            }
        }
        applying = true;
        try {
            Tasks.save(app, t);
            if (local == 0) map("k", t.id, doc);
        } finally {
            applying = false;
        }
        p().edit().putLong(seenKey, cloudUpdated).apply();
    }
}
