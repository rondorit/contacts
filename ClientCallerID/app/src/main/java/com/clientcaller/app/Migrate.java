package com.clientcaller.app;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/** One-time move of the first single-team cloud data into orgs/beki (run on the manager's phone). */
final class Migrate {
    private Migrate() { }

    static void run(final Cloud.Done done) {
        final FirebaseFirestore db = Cloud.db();
        final String me = Cloud.user().getUid();
        final String org = Cloud.LEGACY_ORG;
        Map<String, Object> o = new HashMap<>();
        o.put("name", "Beki Projects");
        o.put("owner", me);
        o.put("created", FieldValue.serverTimestamp());
        o.put("migrated", true);
        Map<String, Object> mine = new HashMap<>();
        mine.put("email", Cloud.email());
        mine.put("name", Cloud.user().getDisplayName());
        mine.put("role", Cloud.ROLE_ADMIN);
        mine.put("joined", FieldValue.serverTimestamp());
        db.collection("orgs").document(org).set(o).addOnFailureListener(e -> done.on("ORG: " + e.getMessage()))
                .addOnSuccessListener(x -> db.collection("orgs").document(org).collection("members").document(me).set(mine)
                        .addOnFailureListener(e -> done.on("ME: " + e.getMessage()))
                        .addOnSuccessListener(x2 -> db.collection("users").get()
                                .addOnFailureListener(e -> done.on("USERS: " + e.getMessage()))
                                .addOnSuccessListener(us -> {
                                    WriteBatch b = db.batch();
                                    for (QueryDocumentSnapshot u : us) {
                                        if (u.getId().equals(me) || u.getString("role") == null) continue;
                                        Map<String, Object> m = new HashMap<>();
                                        m.put("email", u.getString("email"));
                                        m.put("name", u.getString("name"));
                                        m.put("role", u.getString("role"));
                                        m.put("joined", FieldValue.serverTimestamp());
                                        b.set(db.collection("orgs").document(org).collection("members").document(u.getId()), m);
                                    }
                                    b.commit().addOnFailureListener(e -> done.on("MEMBERS: " + e.getMessage()))
                                            .addOnSuccessListener(y -> copyRest(db, org, me, done));
                                })));
    }

    private static void copyRest(final FirebaseFirestore db, final String org, final String me, final Cloud.Done done) {
        db.collection("invites").get().addOnSuccessListener(iq -> {
            for (QueryDocumentSnapshot d : iq) {
                if (d.getString("orgId") != null) continue;
                Map<String, Object> m = new HashMap<>(d.getData());
                m.put("orgId", org);
                m.put("orgName", "Beki Projects");
                db.collection("invites").document(d.getId()).set(m);
            }
        });
        db.collection("tasks").get().addOnSuccessListener(kq -> {
            for (QueryDocumentSnapshot d : kq) {
                Map<String, Object> m = new HashMap<>(d.getData());
                m.put("updatedBy", me);
                db.collection("orgs").document(org).collection("tasks").document(d.getId()).set(m);
            }
        });
        db.collection("tickets").get().addOnSuccessListener(tq -> {
            for (final QueryDocumentSnapshot d : tq) {
                Map<String, Object> m = new HashMap<>(d.getData());
                m.put("updatedBy", me);
                db.collection("orgs").document(org).collection("tickets").document(d.getId()).set(m).addOnSuccessListener(z ->
                        db.collection("tickets").document(d.getId()).collection("notes").get().addOnSuccessListener(nq -> {
                            for (DocumentSnapshot n : nq) {
                                Map<String, Object> nm = new HashMap<>(n.getData());
                                nm.put("by", me);
                                db.collection("orgs").document(org).collection("tickets").document(d.getId())
                                        .collection("notes").document(n.getId()).set(nm);
                            }
                        }));
            }
            CallLogger.prefs(Sync.app).edit().putString("cloud_org", org).putString("cloud_role", Cloud.ROLE_ADMIN)
                    .putString("cloud_org_name", "Beki Projects").apply();
            Map<String, Object> idx = new HashMap<>();
            idx.put("currentOrg", org);
            idx.put("orgs", FieldValue.arrayUnion(org));
            db.collection("users").document(me).set(idx, com.google.firebase.firestore.SetOptions.merge());
            db.collection("org").document("main").update("migratedTo", org);
            done.on(null);
        }).addOnFailureListener(e -> done.on("\u05D4\u05E2\u05D1\u05E8\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4 (\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA): " + e.getMessage()));
    }
}
