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
                    .addOnSuccessListener(r -> join(done))
                    .addOnFailureListener(e -> done.on("\u05D4\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
        } catch (ApiException e) {
            done.on("\u05D4\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD Google \u05D1\u05D5\u05D8\u05DC\u05D4 \u05D0\u05D5 \u05E0\u05DB\u05E9\u05DC\u05D4 (\u05E7\u05D5\u05D3 " + e.getStatusCode() + ")");
        }
    }

    /**
     * After sign-in: the very first user creates the organisation and becomes its manager.
     * Anyone else needs an invitation (by e-mail) from the manager.
     */
    static void join(Done done) {
        final FirebaseUser u = user();
        if (u == null) { done.on("\u05DC\u05D0 \u05DE\u05D7\u05D5\u05D1\u05E8"); return; }
        db().collection("users").document(u.getUid()).get().addOnCompleteListener(t -> {
            if (t.isSuccessful() && t.getResult() != null && t.getResult().exists()) { done.on(null); return; }
            db().collection("org").document("main").get().addOnCompleteListener(o -> {
                if (!o.isSuccessful()) { done.on("\u05D0\u05D9\u05DF \u05D2\u05D9\u05E9\u05D4 \u05DC\u05E2\u05E0\u05DF: " + o.getException().getMessage()); return; }
                DocumentSnapshot org = o.getResult();
                if (org == null || !org.exists()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("admin", u.getUid());
                    m.put("adminEmail", email());
                    m.put("name", "Beki Projects");
                    m.put("created", FieldValue.serverTimestamp());
                    db().collection("org").document("main").set(m)
                            .addOnSuccessListener(x -> createUser(ROLE_ADMIN, done))
                            .addOnFailureListener(e -> done.on("\u05D9\u05E6\u05D9\u05E8\u05EA \u05D4\u05D0\u05E8\u05D2\u05D5\u05DF \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
                    return;
                }
                db().collection("invites").document(email()).get().addOnCompleteListener(inv -> {
                    if (!inv.isSuccessful() || inv.getResult() == null || !inv.getResult().exists()) {
                        done.on("\u05D4\u05D7\u05E9\u05D1\u05D5\u05DF " + email() + " \u05E2\u05D5\u05D3 \u05DC\u05D0 \u05D4\u05D5\u05D6\u05DE\u05DF. \u05D1\u05E7\u05E9 \u05DE\u05D4\u05DE\u05E0\u05D4\u05DC \u05DC\u05D4\u05D6\u05DE\u05D9\u05DF \u05D0\u05D5\u05EA\u05D5.");
                        return;
                    }
                    String role = inv.getResult().getString("role");
                    createUser(role == null ? ROLE_TECH : role, done);
                });
            });
        });
    }

    private static void createUser(String role, Done done) {
        FirebaseUser u = user();
        Map<String, Object> m = new HashMap<>();
        m.put("email", email());
        m.put("name", u.getDisplayName() == null ? email() : u.getDisplayName());
        m.put("role", role);
        m.put("joined", FieldValue.serverTimestamp());
        db().collection("users").document(u.getUid()).set(m)
                .addOnSuccessListener(x -> {
                    db().collection("invites").document(email()).delete();
                    done.on(null);
                })
                .addOnFailureListener(e -> done.on("\u05D4\u05D4\u05E6\u05D8\u05E8\u05E4\u05D5\u05EA \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage()));
    }

    static void invite(String email, String name, String role, Done done) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("role", role);
        m.put("by", email());
        m.put("created", FieldValue.serverTimestamp());
        db().collection("invites").document(email.trim().toLowerCase(Locale.ROOT)).set(m)
                .addOnSuccessListener(x -> done.on(null))
                .addOnFailureListener(e -> done.on(e.getMessage()));
    }
}
