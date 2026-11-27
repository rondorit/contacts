package com.clientcaller.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Numbers at a glance: tickets by status and category, handling times, providers, team, ratings and quotes. */
public class StatsActivity extends Activity {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05DC\u05D5\u05D7 \u05E0\u05EA\u05D5\u05E0\u05D9\u05DD");
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 12);
        box.setPadding(p, p, p, p);
        sv.addView(box);
        box.addView(Theme.header(this, "\uD83D\uDCC8 \u05DC\u05D5\u05D7 \u05E0\u05EA\u05D5\u05E0\u05D9\u05DD"));

        List<TicketDb.Ticket> all = TicketDb.get(this).all();
        long now = System.currentTimeMillis();
        int open = 0, overdue = 0, closed = 0, last30 = 0, closed30 = 0;
        double sumDays = 0;
        int nDays = 0;
        Map<String, int[]> byCat = new LinkedHashMap<>();
        Map<String, double[]> byProvider = new LinkedHashMap<>();
        Map<String, int[]> byTeam = new LinkedHashMap<>();
        for (TicketDb.Ticket t : all) {
            boolean isOpen = t.isOpen();
            if (isOpen) open++; else closed++;
            if (t.isOverdue()) overdue++;
            if (now - t.created < 30 * DAY) last30++;
            String cat = He.category(t.category);
            int[] c = byCat.get(cat);
            if (c == null) byCat.put(cat, c = new int[2]);
            c[isOpen ? 0 : 1]++;
            if (!isOpen && t.updated > t.created) {
                double d = (t.updated - t.created) / (double) DAY;
                sumDays += d;
                nDays++;
                if (now - t.updated < 30 * DAY) closed30++;
                if (!t.provider.isEmpty()) {
                    double[] pv = byProvider.get(t.provider);
                    if (pv == null) byProvider.put(t.provider, pv = new double[3]);
                    pv[1] += d;
                    pv[2]++;
                }
            }
            if (!t.provider.isEmpty()) {
                double[] pv = byProvider.get(t.provider);
                if (pv == null) byProvider.put(t.provider, pv = new double[3]);
                pv[0]++;
            }
            if (!t.assignee.isEmpty()) {
                int[] tv = byTeam.get(t.assignee);
                if (tv == null) byTeam.put(t.assignee, tv = new int[2]);
                tv[isOpen ? 0 : 1]++;
            }
        }

        LinearLayout c1 = card(box, "\u05E7\u05E8\u05D9\u05D0\u05D5\u05EA");
        line(c1, "\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA", String.valueOf(open));
        line(c1, "\u05D1\u05D0\u05D9\u05D7\u05D5\u05E8", String.valueOf(overdue));
        line(c1, "\u05E1\u05D2\u05D5\u05E8\u05D5\u05EA", String.valueOf(closed));
        line(c1, "\u05E0\u05E4\u05EA\u05D7\u05D5 \u05D1-30 \u05D4\u05D9\u05DE\u05D9\u05DD \u05D4\u05D0\u05D7\u05E8\u05D5\u05E0\u05D9\u05DD", String.valueOf(last30));
        line(c1, "\u05E0\u05E1\u05D2\u05E8\u05D5 \u05D1-30 \u05D4\u05D9\u05DE\u05D9\u05DD \u05D4\u05D0\u05D7\u05E8\u05D5\u05E0\u05D9\u05DD", String.valueOf(closed30));
        line(c1, "\u05D6\u05DE\u05DF \u05D8\u05D9\u05E4\u05D5\u05DC \u05DE\u05DE\u05D5\u05E6\u05E2", nDays == 0 ? "-" : days(sumDays / nDays));

        LinearLayout c2 = card(box, "\u05DC\u05E4\u05D9 \u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4 (\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA / \u05E1\u05D2\u05D5\u05E8\u05D5\u05EA)");
        List<Map.Entry<String, int[]>> cats = new ArrayList<>(byCat.entrySet());
        Collections.sort(cats, (a, b) -> (b.getValue()[0] + b.getValue()[1]) - (a.getValue()[0] + a.getValue()[1]));
        int max = 1;
        for (Map.Entry<String, int[]> e : cats) max = Math.max(max, e.getValue()[0] + e.getValue()[1]);
        for (Map.Entry<String, int[]> e : cats) bar(c2, e.getKey(), e.getValue()[0], e.getValue()[1], max);

        if (!byProvider.isEmpty()) {
            LinearLayout c3 = card(box, "\u05DE\u05D9\u05E7\u05D5\u05E8 \u05D7\u05D5\u05E5");
            for (Map.Entry<String, double[]> e : byProvider.entrySet()) {
                double[] v = e.getValue();
                line(c3, e.getKey(), (int) v[0] + " \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA" + (v[2] > 0 ? " \u2022 \u05DE\u05DE\u05D5\u05E6\u05E2 " + days(v[1] / v[2]) : ""));
            }
        }
        if (!byTeam.isEmpty()) {
            LinearLayout c4 = card(box, "\u05D0\u05E0\u05E9\u05D9 \u05E6\u05D5\u05D5\u05EA (\u05E4\u05EA\u05D5\u05D7\u05D5\u05EA / \u05E1\u05D2\u05D5\u05E8\u05D5\u05EA)");
            for (Map.Entry<String, int[]> e : byTeam.entrySet()) line(c4, "\uD83D\uDC77 " + e.getKey(), e.getValue()[0] + " / " + e.getValue()[1]);
        }

        List<Integer> ratings = Survey.ratings(this);
        LinearLayout c5 = card(box, "\u05E9\u05D1\u05D9\u05E2\u05D5\u05EA \u05E8\u05E6\u05D5\u05DF");
        if (ratings.isEmpty()) {
            line(c5, "\u05E2\u05D5\u05D3 \u05D0\u05D9\u05DF \u05D3\u05D9\u05E8\u05D5\u05D2\u05D9\u05DD", "");
        } else {
            double s = 0;
            int[] dist = new int[6];
            for (int r : ratings) { s += r; dist[r]++; }
            line(c5, "\u05DE\u05DE\u05D5\u05E6\u05E2", String.format(java.util.Locale.ROOT, "%.1f / 5 \u2B50", s / ratings.size()));
            line(c5, "\u05DE\u05E1\u05E4\u05E8 \u05D3\u05D9\u05E8\u05D5\u05D2\u05D9\u05DD", String.valueOf(ratings.size()));
            for (int k = 5; k >= 1; k--) bar(c5, k + " \u2B50", dist[k], 0, ratings.size());
        }

        int sent = 0, appr = 0, rej = 0;
        double apprSum = 0;
        for (QuoteDb.Quote q : QuoteDb.get(this).all()) {
            if ("Sent".equals(q.status)) sent++;
            else if ("Approved".equals(q.status)) { appr++; apprSum += q.total(); }
            else if ("Rejected".equals(q.status)) rej++;
        }
        LinearLayout c6 = card(box, "\u05D4\u05E6\u05E2\u05D5\u05EA \u05DE\u05D7\u05D9\u05E8");
        line(c6, "\u05DE\u05DE\u05EA\u05D9\u05E0\u05D5\u05EA \u05DC\u05EA\u05E9\u05D5\u05D1\u05D4", String.valueOf(sent));
        line(c6, "\u05D0\u05D5\u05E9\u05E8\u05D5", appr + (appr > 0 ? "  (" + QuoteDb.money(apprSum) + ")" : ""));
        line(c6, "\u05E0\u05D3\u05D7\u05D5", String.valueOf(rej));
        if (appr + rej > 0) line(c6, "\u05D0\u05D7\u05D5\u05D6 \u05D0\u05D9\u05E9\u05D5\u05E8", Math.round(100.0 * appr / (appr + rej)) + "%");

        setContentView(sv);
        He.rtl(this);
    }

    private static String days(double d) {
        if (d < 1) return Math.max(1, Math.round(d * 24)) + " \u05E9\u05E2\u05D5\u05EA";
        return String.format(java.util.Locale.ROOT, "%.1f \u05D9\u05DE\u05D9\u05DD", d);
    }

    private LinearLayout card(LinearLayout box, String title) {
        box.addView(Ui.sectionTitle(this, title));
        LinearLayout c = Ui.card(this);
        box.addView(c);
        return c;
    }

    private void line(LinearLayout c, String label, String value) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 5));
        TextView l = Ui.text(this, label, 15, Ui.TEXT, false);
        r.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(Ui.text(this, value, 15, Ui.TEXT, true));
        c.addView(r);
    }

    private void bar(LinearLayout c, String label, int a, int b, int max) {
        LinearLayout r = new LinearLayout(this);
        r.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        r.addView(Ui.text(this, label, 14, Ui.TEXT, false), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f));
        LinearLayout track = new LinearLayout(this);
        int h = Ui.dp(this, 10);
        android.view.View va = new android.view.View(this);
        va.setBackground(Ui.rounded(Ui.PRIMARY, 5, this));
        track.addView(va, new LinearLayout.LayoutParams(0, h, Math.max(0.0001f, a)));
        if (b > 0) {
            android.view.View vb = new android.view.View(this);
            vb.setBackground(Ui.rounded(0xFF5B6B80, 5, this));
            track.addView(vb, new LinearLayout.LayoutParams(0, h, b));
        }
        int rest = Math.max(0, max - a - b);
        if (rest > 0) track.addView(new android.view.View(this), new LinearLayout.LayoutParams(0, h, rest));
        r.addView(track, new LinearLayout.LayoutParams(0, h, 1.6f));
        r.addView(Ui.text(this, "  " + a + (b > 0 ? " / " + b : ""), 13, Ui.MUTED, false));
        c.addView(r);
    }
}
