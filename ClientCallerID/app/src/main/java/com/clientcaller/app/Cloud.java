package com.clientcaller.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Cloud (multi-user) basics: Google sign-in, the organisation owner (first user = manager),
 * invitations and roles. Client lists stay on the phone; only work items will be shared.
 */
final class Cloud {
    static final int REQ_SIGN_IN = 501;
    static final String WEB_CLIENT_ID = "1065393018022-sdqtmgcvg902v3dr9t3knrmgsiupidnn.apps.googleusercontent.com";
    static final String ROLE_ADMIN = "admin", ROLE_TECH = "tech";

    interface Done { void on(String error); }

    private Cloud() { }

    static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    static FirebaseUser user() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    static String email() {
        FirebaseUser u = user();
        return u == null || u.getEmail() == null ? "" : u.getEmail().toLowerCase(Locale.ROOT);
    }

    static String roleHe(String r) {
        return ROLE_ADMIN.equals(r) ? "\u05DE\u05E0\u05D4\u05DC" : ROLE_TECH.equals(r) ? "\u05D8\u05DB\u05E0\u05D0\u05D9" : r;
    }

    private static GoogleSignInClient client(Context c) {
        GoogleSignInOptions o = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(c, o);
    }

    static void startSignIn(Activity a) {
        a.startActivityForResult(client(a).getSignInIntent(), REQ_SIGN_IN);
    }

    static void signOut(Activity a, Runnable after) {
        FirebaseAuth.getInstance().signOut();
        client(a).signOut().addOnCompleteListener(t -> { if (after != null) after.run(); });
    }

    /** Handles the Google sign-in result, then joins the organisation. */
    static void onSignInResult(Intent data, Done done) {
        try {
            GoogleSignInAccount acc = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(acc.getIdToken(), null))
                    .addOnSuccessListener(r -> done.on(null))
                    .addOnFailureListener(e -> done.on("\u05D4\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
        } catch (ApiException e) {
            done.on("\u05D4\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD Google \u05D1\u05D5\u05D8\u05DC\u05D4 \u05D0\u05D5 \u05E0\u05DB\u05E9\u05DC\u05D4 (\u05E7\u05D5\u05D3 " + e.getStatusCode() + ")");
        }
    }

    // ------------------------------------------------------------------ organisations

    static final String LEGACY_ORG = "beki";

    /** The organisation this phone works with (empty until joined). */
    static String org() {
        return Sync.app == null ? "" : CallLogger.prefs(Sync.app).getString("cloud_org", "");
    }

    static com.google.firebase.firestore.CollectionReference col(String name) {
        return db().collection("orgs").document(org()).collection(name);
    }

    private static void setOrg(String orgId, String role, String orgName) {
        CallLogger.prefs(Sync.app).edit().putString("cloud_org", orgId).putString("cloud_role", role == null ? "" : role)
                .putString("cloud_org_name", orgName == null ? "" : orgName).apply();
        Map<String, Object> m = new HashMap<>();
        m.put("currentOrg", orgId);
        m.put("orgs", FieldValue.arrayUnion(orgId));
        m.put("email", email());
        db().collection("users").document(user().getUid()).set(m, com.google.firebase.firestore.SetOptions.merge());
    }

    /** Result codes for join(). */
    static final String NO_ORG = "NO_ORG";

    /**
     * After sign-in: find this user's organisation, migrate the old single-team data if this is its manager,
     * accept an invitation, or report NO_ORG so the screen can offer to create a new organisation.
     */
    static void join(Done done) {
        final FirebaseUser u = user();
        if (u == null) { done.on("\u05DC\u05D0 \u05DE\u05D7\u05D5\u05D1\u05E8"); return; }
        db().collection("users").document(u.getUid()).get().addOnCompleteListener(t -> {
            DocumentSnapshot ud = t.isSuccessful() ? t.getResult() : null;
            String cur = ud != null && ud.exists() ? ud.getString("currentOrg") : null;
            if (cur != null && !cur.isEmpty()) { useOrg(cur, done); return; }
            // Old single-team data?
            db().collection("org").document("main").get().addOnCompleteListener(o -> {
                DocumentSnapshot old = o.isSuccessful() ? o.getResult() : null;
                if (old != null && old.exists() && u.getUid().equals(old.getString("admin"))) {
                    Migrate.run(done);
                    return;
                }
                // A member of the migrated team?
                db().collection("orgs").document(LEGACY_ORG).collection("members").document(u.getUid()).get()
                        .addOnCompleteListener(lm -> {
                            if (lm.isSuccessful() && lm.getResult() != null && lm.getResult().exists()) {
                                useOrg(LEGACY_ORG, done);
                                return;
                            }
                            acceptInvite(done);
                        });
            });
        });
    }

    private static void useOrg(final String orgId, final Done done) {
        db().collection("orgs").document(orgId).collection("members").document(user().getUid()).get().addOnCompleteListener(m -> {
            if (!m.isSuccessful() || m.getResult() == null || !m.getResult().exists()) { acceptInvite(done); return; }
            final String role = m.getResult().getString("role");
            db().collection("orgs").document(orgId).get().addOnCompleteListener(o -> {
                String name = o.isSuccessful() && o.getResult() != null ? o.getResult().getString("name") : "";
                setOrg(orgId, role, name);
                done.on(null);
            });
        });
    }

    private static void acceptInvite(final Done done) {
        db().collection("invites").document(email()).get().addOnCompleteListener(inv -> {
            DocumentSnapshot d = inv.isSuccessful() ? inv.getResult() : null;
            if (d == null || !d.exists() || d.getString("orgId") == null) { done.on(NO_ORG); return; }
            final String orgId = d.getString("orgId"), role = d.getString("role") == null ? ROLE_TECH : d.getString("role");
            final String orgName = d.getString("orgName");
            Map<String, Object> m = new HashMap<>();
            m.put("email", email());
            m.put("name", user().getDisplayName() == null ? email() : user().getDisplayName());
            m.put("role", role);
            m.put("joined", FieldValue.serverTimestamp());
            db().collection("orgs").document(orgId).collection("members").document(user().getUid()).set(m)
                    .addOnSuccessListener(x -> {
                        db().collection("invites").document(email()).delete();
                        setOrg(orgId, role, orgName);
                        done.on(null);
                    })
                    .addOnFailureListener(e -> done.on("\u05D4\u05D4\u05E6\u05D8\u05E8\u05E4\u05D5\u05EA \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
        });
    }

    /** Creates a new organisation with this user as its manager. */
    static void createOrg(final String name, final Done done) {
        final String id = db().collection("orgs").document().getId();
        Map<String, Object> o = new HashMap<>();
        o.put("name", name);
        o.put("owner", user().getUid());
        o.put("created", FieldValue.serverTimestamp());
        db().collection("orgs").document(id).set(o).addOnSuccessListener(x -> {
            Map<String, Object> m = new HashMap<>();
            m.put("email", email());
            m.put("name", user().getDisplayName() == null ? email() : user().getDisplayName());
            m.put("role", ROLE_ADMIN);
            m.put("joined", FieldValue.serverTimestamp());
            db().collection("orgs").document(id).collection("members").document(user().getUid()).set(m)
                    .addOnSuccessListener(y -> { setOrg(id, ROLE_ADMIN, name); done.on(null); })
                    .addOnFailureListener(e -> done.on(e.getMessage()));
        }).addOnFailureListener(e -> done.on("\u05D9\u05E6\u05D9\u05E8\u05EA \u05D4\u05D0\u05E8\u05D2\u05D5\u05DF \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
    }

    static void invite(String email, String name, String role, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("role", role);
        m.put("orgId", org());
        m.put("orgName", CallLogger.prefs(Sync.app).getString("cloud_org_name", ""));
        m.put("by", email());
        m.put("created", FieldValue.serverTimestamp());
        db().collection("invites").document(email.trim().toLowerCase(Locale.ROOT)).set(m)
                .addOnSuccessListener(x -> done.on(null))
                .addOnFailureListener(e -> done.on(e.getMessage()));
    }
}
