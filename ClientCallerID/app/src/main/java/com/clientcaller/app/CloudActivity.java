package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/** \u2601\uFE0F Cloud (preview): sign in, see your role; the manager invites and manages users. */
public class CloudActivity extends Activity {
    private LinearLayout box;
    private String myRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Sync.app = getApplicationContext();
        setTitle("\u05E2\u05E0\u05DF \u2013 \u05E6\u05D5\u05D5\u05EA");
        ScrollView sv = new ScrollView(this);
        box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 12);
        box.setPadding(p, p, p, p);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
        refresh();
    }

    private void refresh() {
        box.removeAllViews();
        box.addView(Theme.header(this, "\u2601\uFE0F \u05E2\u05E0\u05DF \u2013 \u05E2\u05D1\u05D5\u05D3\u05EA \u05E6\u05D5\u05D5\u05EA (\u05E0\u05D9\u05E1\u05D9\u05D5\u05E0\u05D9)"));
        FirebaseUser u = Cloud.user();
        if (u == null) {
            box.addView(Ui.text(this, "\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD \u05D7\u05E9\u05D1\u05D5\u05DF Google \u05DE\u05D7\u05D1\u05E8\u05EA \u05D0\u05D5\u05EA\u05DA \u05DC\u05E6\u05D5\u05D5\u05EA \u05E9\u05DC\u05DA. \u05E8\u05E9\u05D9\u05DE\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7\u05D5\u05EA \u05E0\u05E9\u05D0\u05E8\u05EA \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF \u05E9\u05DC\u05DA \u2013 \u05D1\u05E2\u05E0\u05DF \u05D9\u05D9\u05E9\u05DE\u05E8\u05D5 \u05E8\u05E7 \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05D5\u05DE\u05E9\u05D9\u05DE\u05D5\u05EA.", 15, Ui.TEXT, false));
            box.addView(button("\uD83D\uDD11 \u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD Google", v -> Cloud.startSignIn(this)));
            return;
        }
        box.addView(Ui.text(this, "\u05DE\u05D7\u05D5\u05D1\u05E8: " + u.getDisplayName() + "\n" + Cloud.email(), 15, Ui.TEXT, true));
        final TextView role = Ui.text(this, "\u05D1\u05D5\u05D3\u05E7 \u05D4\u05E8\u05E9\u05D0\u05D5\u05EA\u2026", 14, Ui.MUTED, false);
        box.addView(role);
        Cloud.join(err -> {
            if (Cloud.NO_ORG.equals(err)) {
                role.setText("\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D4\u05D6\u05DE\u05E0\u05D4 \u05DC\u05D7\u05E9\u05D1\u05D5\u05DF \u05D4\u05D6\u05D4.");
                box.addView(Ui.text(this, "\u05D0\u05DD \u05D4\u05D5\u05D6\u05DE\u05E0\u05EA \u2013 \u05D1\u05D3\u05D5\u05E7 \u05E9\u05E0\u05DB\u05E0\u05E1\u05EA \u05E2\u05DD \u05D0\u05D5\u05EA\u05D4 \u05DB\u05EA\u05D5\u05D1\u05EA Gmail. \u05D0\u05E4\u05E9\u05E8 \u05D2\u05DD \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D0\u05E8\u05D2\u05D5\u05DF \u05D7\u05D3\u05E9 (\u05EA\u05D4\u05D9\u05D4 \u05D4\u05DE\u05E0\u05D4\u05DC \u05E9\u05DC\u05D5).", 14, Ui.MUTED, false), 3);
                box.addView(button("\uD83C\uDFE2 \u05E4\u05EA\u05D9\u05D7\u05EA \u05D0\u05E8\u05D2\u05D5\u05DF \u05D7\u05D3\u05E9", v -> newOrgDialog()), 4);
                return;
            }
            if (err != null) { role.setText(err); return; }
            myRole = CallLogger.prefs(this).getString("cloud_role", "");
            Sync.start(this);
            role.setText("\u05D0\u05E8\u05D2\u05D5\u05DF: " + CallLogger.prefs(this).getString("cloud_org_name", "") + "  \u2022  \u05EA\u05E4\u05E7\u05D9\u05D3: " + Cloud.roleHe(myRole));
            if (Cloud.ROLE_ADMIN.equals(myRole)) adminSection();
        });
        final boolean syncOn = CallLogger.prefs(this).getBoolean("cloud_sync", true);
        box.addView(button(syncOn ? "\uD83D\uDD04 \u05E1\u05E0\u05DB\u05E8\u05D5\u05DF \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05D5\u05DE\u05E9\u05D9\u05DE\u05D5\u05EA: \u05E4\u05D5\u05E2\u05DC (\u05DC\u05D7\u05E5 \u05DC\u05DB\u05D9\u05D1\u05D5\u05D9)" : "\u23F8\uFE0F \u05E1\u05E0\u05DB\u05E8\u05D5\u05DF \u05DB\u05D1\u05D5\u05D9 (\u05DC\u05D7\u05E5 \u05DC\u05D4\u05E4\u05E2\u05DC\u05D4)", v -> {
            CallLogger.prefs(this).edit().putBoolean("cloud_sync", !syncOn).apply();
            if (!syncOn) Sync.start(this); else Sync.stop();
            refresh();
        }));
        box.addView(button("\uD83D\uDEAA \u05D4\u05EA\u05E0\u05EA\u05E7\u05D5\u05EA", v -> {
            Sync.stop();
            CallLogger.prefs(this).edit().remove("cloud_role").remove("cloud_org").apply();
            Cloud.signOut(this, this::refresh);
        }));
    }

    private void adminSection() {
        box.addView(Ui.sectionTitle(this, "\uD83D\uDC65 \u05D0\u05E0\u05E9\u05D9 \u05D4\u05E6\u05D5\u05D5\u05EA \u05D1\u05E2\u05E0\u05DF"));
        final LinearLayout users = Ui.card(this);
        box.addView(users);
        Cloud.col("members").get().addOnSuccessListener(q -> {
            java.util.List<String[]> cache = new java.util.ArrayList<>();
            for (QueryDocumentSnapshot d0 : q) cache.add(new String[]{d0.getId(), d0.getString("name"), d0.getString("email")});
            Sync.cacheUsers(cache);
            for (QueryDocumentSnapshot d : q) {
                final String uid = d.getId(), name = d.getString("name"), mail = d.getString("email"), r = d.getString("role");
                TextView tv = Ui.text(this, (Cloud.ROLE_ADMIN.equals(r) ? "\u2B50 " : "\uD83D\uDC77 ") + name + "\n" + mail + " \u2022 " + Cloud.roleHe(r), 15, Ui.TEXT, false);
                tv.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
                if (!uid.equals(Cloud.user().getUid())) tv.setOnClickListener(v -> userOptions(uid, name, r));
                users.addView(tv);
            }
        });
        box.addView(Ui.sectionTitle(this, "\u2709\uFE0F \u05D4\u05D6\u05DE\u05E0\u05D5\u05EA \u05E9\u05DE\u05DE\u05EA\u05D9\u05E0\u05D5\u05EA"));
        final LinearLayout inv = Ui.card(this);
        box.addView(inv);
        Cloud.db().collection("invites").whereEqualTo("orgId", Cloud.org()).get().addOnSuccessListener(q -> {
            if (q.isEmpty()) inv.addView(Ui.text(this, "\u05D0\u05D9\u05DF \u05D4\u05D6\u05DE\u05E0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", 14, Ui.MUTED, false));
            for (QueryDocumentSnapshot d : q) {
                final String mail = d.getId();
                TextView tv = Ui.text(this, "\u23F3 " + d.getString("name") + " \u2013 " + mail + " \u2022 " + Cloud.roleHe(d.getString("role")), 14, Ui.TEXT, false);
                tv.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
                tv.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("\u05DC\u05D1\u05D8\u05DC \u05D0\u05EA \u05D4\u05D4\u05D6\u05DE\u05E0\u05D4 \u05E9\u05DC " + mail + "?")
                        .setPositiveButton("\u05D1\u05D8\u05DC \u05D4\u05D6\u05DE\u05E0\u05D4", (dd, w) -> Cloud.db().collection("invites").document(mail).delete()
                                .addOnSuccessListener(x -> refresh()))
                        .setNegativeButton("\u05E1\u05D2\u05D5\u05E8", null).show());
                inv.addView(tv);
            }
        });
        box.addView(button("\u2795 \u05D4\u05D6\u05DE\u05DF \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA", v -> inviteDialog()));
    }

    private void userOptions(final String uid, final String name, final String role) {
        String[] opts = {Cloud.ROLE_ADMIN.equals(role) ? "\uD83D\uDC77 \u05D4\u05E4\u05D5\u05DA \u05DC\u05D8\u05DB\u05E0\u05D0\u05D9" : "\u2B50 \u05D4\u05E4\u05D5\u05DA \u05DC\u05DE\u05E0\u05D4\u05DC", "\uD83D\uDDD1\uFE0F \u05D4\u05E1\u05E8 \u05DE\u05D4\u05E6\u05D5\u05D5\u05EA"};
        new AlertDialog.Builder(this).setTitle(name).setItems(opts, (d, w) -> {
            if (w == 0) {
                Cloud.col("members").document(uid)
                        .update("role", Cloud.ROLE_ADMIN.equals(role) ? Cloud.ROLE_TECH : Cloud.ROLE_ADMIN)
                        .addOnSuccessListener(x -> refresh());
            } else {
                Auth.confirm(this, "\u05D4\u05E1\u05E8\u05EA " + name + " \u05DE\u05D4\u05E6\u05D5\u05D5\u05EA", () -> Cloud.col("members").document(uid).delete()
                        .addOnSuccessListener(x -> refresh()));
            }
        }).show();
    }

    private void newOrgDialog() {
        final EditText name = new EditText(this);
        name.setHint("\u05E9\u05DD \u05D4\u05D0\u05E8\u05D2\u05D5\u05DF / \u05D4\u05E2\u05E1\u05E7");
        new AlertDialog.Builder(this).setTitle("\u05D0\u05E8\u05D2\u05D5\u05DF \u05D7\u05D3\u05E9").setView(name)
                .setPositiveButton("\u05E6\u05D5\u05E8", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    Cloud.createOrg(n, err -> {
                        if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                        refresh();
                    });
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null).show();
    }

    private void inviteDialog() {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        final EditText name = new EditText(this);
        name.setHint("\u05E9\u05DD");
        lay.addView(name);
        final EditText mail = new EditText(this);
        mail.setHint("\u05DB\u05EA\u05D5\u05D1\u05EA Gmail \u05E9\u05DC\u05D5");
        mail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        lay.addView(mail);
        final android.widget.CheckBox admin = new android.widget.CheckBox(this);
        admin.setText("\u05DE\u05E0\u05D4\u05DC (\u05E8\u05D5\u05D0\u05D4 \u05D5\u05DE\u05E0\u05D4\u05DC \u05D4\u05DB\u05DC)");
        lay.addView(admin);
        new AlertDialog.Builder(this).setTitle("\u05D4\u05D6\u05DE\u05E0\u05EA \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA").setView(lay)
                .setPositiveButton("\u05D4\u05D6\u05DE\u05DF", (d, w) -> {
                    final String e = mail.getText().toString().trim();
                    final String n = name.getText().toString().trim();
                    if (!e.matches("[^@\\s]+@[^@\\s]+\\.[a-zA-Z]{2,}")) { Toast.makeText(this, "\u05DB\u05EA\u05D5\u05D1\u05EA \u05DE\u05D9\u05D9\u05DC \u05DC\u05D0 \u05EA\u05E7\u05D9\u05E0\u05D4", Toast.LENGTH_SHORT).show(); return; }
                    String dom = e.substring(e.indexOf('@') + 1).toLowerCase(java.util.Locale.ROOT);
                    if (!dom.equals("gmail.com") && (dom.startsWith("gmai") || dom.startsWith("gmial") || dom.startsWith("gamil") || dom.startsWith("gnail") || dom.equals("gmail.co"))) {
                        Toast.makeText(this, "\u05E0\u05E8\u05D0\u05D4 \u05E9\u05D9\u05E9 \u05D8\u05E2\u05D5\u05EA \u05D1\u05DB\u05EA\u05D5\u05D1\u05EA: " + dom + " (\u05D4\u05EA\u05DB\u05D5\u05D5\u05E0\u05EA \u05DC-gmail.com?)", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Cloud.invite(e, n, admin.isChecked() ? Cloud.ROLE_ADMIN : Cloud.ROLE_TECH, err -> {
                        if (err != null) { Toast.makeText(this, "\u05E0\u05DB\u05E9\u05DC: " + err, Toast.LENGTH_LONG).show(); return; }
                        refresh();
                        String msg = "\u05D4\u05D9\u05D9 " + n + ", \u05D4\u05D5\u05D6\u05DE\u05E0\u05EA \u05DC\u05E6\u05D5\u05D5\u05EA \u05E9\u05DC " + CallLogger.prefs(this).getString("cloud_org_name", "") + " \u05D1\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 On IT.\n"
                                + "\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD \u05D7\u05E9\u05D1\u05D5\u05DF Google: " + e + "\n\n"
                                + "\uD83D\uDCF1 \u05D0\u05E0\u05D3\u05E8\u05D5\u05D0\u05D9\u05D3:\n1. \u05D4\u05EA\u05E7\u05E0\u05D4: https://github.com/rondorit/contacts/releases/tag/preview\n"
                                + "2. \u05D1\u05DE\u05E1\u05DA \u05D4\u05E8\u05D0\u05E9\u05D9 \u2190 \u2601\uFE0F \u05E2\u05E0\u05DF \u2013 \u05E6\u05D5\u05D5\u05EA \u2190 \u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD Google.\n\n"
                                + "\uD83C\uDF4E \u05D0\u05D9\u05D9\u05E4\u05D5\u05DF:\n1. \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D1\u05E1\u05E4\u05D0\u05E8\u05D9: https://rondorit.github.io/contacts\n"
                                + "2. \u05DC\u05DC\u05D7\u05D5\u05E5 \"\u05DB\u05E0\u05D9\u05E1\u05D4 \u05E2\u05DD Google\" \u05D5\u05DC\u05D1\u05D7\u05D5\u05E8 \u05D0\u05EA " + e + ".\n"
                                + "3. \u05DB\u05E4\u05EA\u05D5\u05E8 \u05D4\u05E9\u05D9\u05EA\u05D5\u05E3 (\u05E8\u05D9\u05D1\u05D5\u05E2 \u05E2\u05DD \u05D7\u05E5) \u2190 \"\u05D4\u05D5\u05E1\u05E3 \u05DC\u05DE\u05E1\u05DA \u05D4\u05D1\u05D9\u05EA\" \u2190 \u05D4\u05D5\u05E1\u05E3.\n"
                                + "4. \u05DC\u05E4\u05EA\u05D5\u05D7 \u05D0\u05EA On IT \u05DE\u05D4\u05D0\u05D9\u05D9\u05E7\u05D5\u05DF \u05D5\u05DC\u05D4\u05D9\u05DB\u05E0\u05E1 \u05E9\u05D5\u05D1 \u05E2\u05DD Google.";
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, msg), "\u05E9\u05DC\u05D9\u05D7\u05EA \u05D4\u05D4\u05D6\u05DE\u05E0\u05D4"));
                    });
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null).show();
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setOnClickListener(l);
        return b;
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != Cloud.REQ_SIGN_IN) return;
        Cloud.onSignInResult(data, err -> {
            if (err != null) Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            refresh();
        });
    }
}
