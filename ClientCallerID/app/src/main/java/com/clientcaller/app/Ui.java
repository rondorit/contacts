package com.clientcaller.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Shared look: colors, cards and icon tiles. */
final class Ui {
    static final int PRIMARY = 0xFF1565C0;
    static final int PRIMARY_DARK = 0xFF0D47A1;
    static final int BG = 0xFFF3F6FB;
    static final int TEXT = 0xFF1F2937;
    static final int MUTED = 0xFF6B7280;
    static final int RED = 0xFFD32F2F;
    static final int GREEN = 0xFF2E7D32;
    static final int ORANGE = 0xFFEF6C00;

    private Ui() { }

    static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    static GradientDrawable rounded(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** White rounded card with a soft shadow. */
    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(rounded(Color.WHITE, 14, c));
        l.setElevation(dp(c, 2));
        int p = dp(c, 14);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams cardParams(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = dp(c, 5);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    static TextView text(Context c, String s, float size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    static TextView sectionTitle(Context c, String s) {
        TextView t = text(c, s, 15, PRIMARY_DARK, true);
        t.setPadding(dp(c, 6), dp(c, 16), dp(c, 6), dp(c, 4));
        return t;
    }

    /** Square-ish tile: big icon, title, optional small status line. */
    static LinearLayout tile(Context c, String icon, String title, String sub, int subColor, View.OnClickListener l) {
        LinearLayout t = card(c);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setMinimumHeight(dp(c, 104));
        TextView i = text(c, icon, 28, TEXT, false);
        i.setGravity(Gravity.CENTER);
        t.addView(i);
        TextView ti = text(c, title, 14, TEXT, true);
        ti.setGravity(Gravity.CENTER);
        ti.setPadding(0, dp(c, 4), 0, 0);
        t.addView(ti);
        if (sub != null && !sub.isEmpty()) {
            TextView s = text(c, sub, 12, subColor, false);
            s.setGravity(Gravity.CENTER);
            s.setMaxLines(2);
            t.addView(s);
        }
        t.setClickable(true);
        t.setOnClickListener(l);
        t.setForeground(c.getDrawable(android.R.drawable.list_selector_background));
        return t;
    }

    /** Statistic card: big number with a label. */
    static LinearLayout stat(Context c, String icon, String number, String label, int color, View.OnClickListener l) {
        LinearLayout t = card(c);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView n = text(c, icon + "  " + number, 26, color, true);
        n.setGravity(Gravity.CENTER);
        t.addView(n);
        TextView lb = text(c, label, 13, MUTED, false);
        lb.setGravity(Gravity.CENTER);
        t.addView(lb);
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }

    /** Lays views out in rows of `cols` equal-width cells. */
    static void grid(Context c, LinearLayout parent, List<View> cells, int cols) {
        LinearLayout row = null;
        for (int i = 0; i < cells.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                parent.addView(row);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            int m = dp(c, 5);
            lp.setMargins(m, m, m, m);
            row.addView(cells.get(i), lp);
        }
        int rest = cells.size() % cols;
        if (rest != 0 && row != null) {
            for (int i = rest; i < cols; i++) {
                View spacer = new View(c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 1, 1f);
                int m = dp(c, 5);
                lp.setMargins(m, m, m, m);
                row.addView(spacer, lp);
            }
        }
    }

    /** List row card: title line + muted second line. */
    static LinearLayout rowCard(Context c, String title, String sub, String badge, int badgeColor, View.OnClickListener l) {
        LinearLayout t = card(c);
        if (badge != null && !badge.isEmpty()) {
            TextView b = text(c, badge, 12, Color.WHITE, true);
            b.setBackground(rounded(badgeColor, 10, c));
            b.setPadding(dp(c, 8), dp(c, 2), dp(c, 8), dp(c, 2));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(c, 4);
            t.addView(b, lp);
        }
        t.addView(text(c, title, 16, TEXT, true));
        if (sub != null && !sub.isEmpty()) t.addView(text(c, sub, 14, MUTED, false));
        t.setClickable(true);
        t.setOnClickListener(l);
        return t;
    }
}
