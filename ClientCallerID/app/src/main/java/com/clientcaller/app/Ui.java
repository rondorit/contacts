package com.clientcaller.app;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Shared look: Deep Focus colours, cards, pills and icon tiles. */
final class Ui {
    // Semantic aliases kept for the existing screens; all resolve to Theme tokens.
    static final int PRIMARY = Theme.MINT;
    static final int PRIMARY_DARK = Theme.MINT;
    static final int BG = Theme.CANVAS;
    static final int TEXT = Theme.TEXT_PRIMARY;
    static final int MUTED = Theme.TEXT_SECONDARY;
    static final int RED = Theme.DANGER;
    static final int GREEN = Theme.MINT;
    static final int ORANGE = Theme.WARNING;
    static final int LABEL = Theme.TEXT_SECONDARY;
    static final int FIELD = Theme.SURFACE_RAISED;
    static final int FIELD_LINE = Theme.HAIRLINE;

    private Ui() { }

    static int dp(Context c, float v) {
        return Theme.dp(c, v);
    }

    static GradientDrawable rounded(int color, float radiusDp, Context c) {
        return Theme.shape(color, radiusDp, c);
    }

    /** Lifted surface card with a hairline; no heavy shadow on the dark canvas. */
    static LinearLayout card(Context c) {
        return Theme.card(c);
    }

    static LinearLayout.LayoutParams cardParams(Context c) {
        return Theme.cardParams(c);
    }

    static TextView text(Context c, String s, float size, int color, boolean bold) {
        return Theme.text(c, s, size, color, bold ? 2 : 0);
    }

    static TextView sectionTitle(Context c, String s) {
        return Theme.sectionHeader(c, s);
    }

    /** Square-ish tile: big icon, title, optional small status line. */
    static LinearLayout tile(Context c, String icon, String title, String sub, int subColor, View.OnClickListener l) {
        LinearLayout t = card(c);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setMinimumHeight(dp(c, 104));
        TextView i = Theme.text(c, icon, 26, Theme.TEXT_PRIMARY, 0);
        i.setGravity(Gravity.CENTER);
        i.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(i);
        TextView ti = Theme.label(c, title, Theme.TEXT_PRIMARY);
        ti.setGravity(Gravity.CENTER);
        ti.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ti.setPadding(0, dp(c, Theme.S1), 0, 0);
        t.addView(ti);
        if (sub != null && !sub.isEmpty()) {
            TextView s = Theme.caption(c, sub, subColor == 0 ? Theme.TEXT_SECONDARY : subColor);
            s.setGravity(Gravity.CENTER);
            s.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            s.setMaxLines(2);
            t.addView(s);
        }
        t.setClickable(true);
        t.setOnClickListener(l);
        t.setForeground(Theme.pressable(c, null));
        return t;
    }

    /** Statistic card: big number with a label. */
    static LinearLayout stat(Context c, String icon, String number, String label, int color, View.OnClickListener l) {
        LinearLayout t = card(c);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView n = Theme.display(c, icon + "  " + number, color);
        n.setGravity(Gravity.CENTER);
        n.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(n);
        TextView lb = Theme.caption(c, label, Theme.TEXT_SECONDARY);
        lb.setGravity(Gravity.CENTER);
        lb.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(lb);
        t.setClickable(true);
        t.setOnClickListener(l);
        t.setForeground(Theme.pressable(c, null));
        return t;
    }

    /** Lays views out in rows of `cols` equal-width cells. */
    static void grid(Context c, LinearLayout parent, List<View> cells, int cols) {
        LinearLayout row = null;
        int fullCells = cells.size() - (cells.size() % cols);
        for (int i = 0; i < cells.size(); i++) {
            if (i % cols == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                parent.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            // A partial last row must size its tiles to their content, otherwise they collapse to the spacer height.
            int h = i < fullCells ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, h, 1f);
            int m = dp(c, Theme.S1);
            lp.setMargins(m, m, m, m);
            row.addView(cells.get(i), lp);
        }
        int rest = cells.size() % cols;
        if (rest != 0 && row != null) {
            for (int i = rest; i < cols; i++) {
                View spacer = new View(c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
                int m = dp(c, Theme.S1);
                lp.setMargins(m, m, m, m);
                row.addView(spacer, lp);
            }
        }
    }

    /** List row card: title line + muted second line. */
    static LinearLayout rowCard(Context c, String title, String sub, String badge, int badgeColor, View.OnClickListener l) {
        LinearLayout t = card(c);
        if (badge != null && !badge.isEmpty()) {
            TextView b = Theme.badge(c, badge, badgeColor == 0 ? Theme.MINT : badgeColor);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(c, Theme.S1);
            t.addView(b, lp);
        }
        t.addView(Theme.subtitle(c, title, Theme.TEXT_PRIMARY));
        if (sub != null && !sub.isEmpty()) t.addView(Theme.body(c, sub, Theme.TEXT_SECONDARY));
        t.setClickable(true);
        t.setOnClickListener(l);
        t.setForeground(Theme.pressable(c, null));
        return t;
    }

    /** Applies the Deep Focus skin to every standard widget in the tree. */
    static void polish(View v) {
        if (v instanceof android.widget.Button && !(v instanceof android.widget.CompoundButton)) {
            android.widget.Button b = (android.widget.Button) v;
            Context c = b.getContext();
            float sp = c.getResources().getDisplayMetrics().scaledDensity;
            boolean primary = b.getTextSize() >= 17.5f * sp;
            Theme.styleButton(b, primary);
            if (!b.isEnabled()) b.setAlpha(0.4f);
            ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                int m = dp(c, Theme.S1);
                ((ViewGroup.MarginLayoutParams) lp).setMargins(m, m, m, m);
                b.setLayoutParams(lp);
            }
        } else if (v instanceof android.widget.EditText) {
            styleField((android.widget.EditText) v);
        } else if (v instanceof android.widget.Spinner) {
            styleSpinner((android.widget.Spinner) v);
        } else if (v instanceof TextView && !(v instanceof android.widget.Button)) {
            TextView t = (TextView) v;
            float sp = t.getResources().getDisplayMetrics().scaledDensity;
            int col = t.getCurrentTextColor();
            t.setTextSize(Theme.normalizeTextSize(t.getTextSize() / sp));
            t.setLetterSpacing(0f);
            t.setLineSpacing(dp(t.getContext(), 4), 1f);
            // Small grey form labels -> secondary, medium labels.
            if (Math.abs(t.getTextSize() - 13 * sp) < 0.6f * sp
                    && (col == 0xFF777777 || col == MUTED || col == Theme.TEXT_SECONDARY)) {
                t.setTextColor(Theme.TEXT_SECONDARY);
                t.setTypeface(Theme.typeface(1));
            }
            int hg = t.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
            if (t.getTextAlignment() == View.TEXT_ALIGNMENT_GRAVITY && hg != Gravity.CENTER_HORIZONTAL) {
                t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
        } else if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) polish(g.getChildAt(i));
        }
    }

    static android.graphics.drawable.Drawable fieldBg(Context c) {
        return Theme.fieldBg(c);
    }

    static void styleField(android.widget.EditText e) {
        Context c = e.getContext();
        if (e.getTag(R_STYLED) != null) return;
        e.setTag(R_STYLED, Boolean.TRUE);
        e.setBackground(Theme.fieldBg(c));
        e.setPadding(dp(c, Theme.S3), dp(c, Theme.S3), dp(c, Theme.S3), dp(c, Theme.S3));
        e.setTextColor(Theme.TEXT_PRIMARY);
        e.setHintTextColor(Theme.TEXT_TERTIARY);
        e.setTextSize(Theme.BODY);
        e.setTypeface(Theme.typeface(0));
        e.setLetterSpacing(0f);
        e.setLineSpacing(dp(c, 4), 1f);
        e.setMinHeight(dp(c, 48));
        e.setMinimumHeight(dp(c, 48));
        ViewGroup.LayoutParams lp = e.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            if (m.topMargin == 0) m.topMargin = dp(c, Theme.S1);
            if (m.bottomMargin == 0) m.bottomMargin = dp(c, Theme.S1);
            e.setLayoutParams(m);
        }
    }

    static void styleSpinner(android.widget.Spinner s) {
        Context c = s.getContext();
        if (s.getTag(R_STYLED) != null) return;
        s.setTag(R_STYLED, Boolean.TRUE);
        s.setBackground(new SpinnerBg(c));
        s.setPadding(dp(c, Theme.S2), dp(c, Theme.S2), dp(c, Theme.S2), dp(c, Theme.S2));
        s.setPopupBackgroundDrawable(Theme.raisedBg(c));
        ViewGroup.LayoutParams lp = s.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            if (m.topMargin == 0) m.topMargin = dp(c, Theme.S1);
            s.setLayoutParams(m);
        }
    }

    private static final int R_STYLED = 0x7f0f0001 + 0x1234;

    /** Raised box with a small mint chevron on the left side (RTL end). */
    static final class SpinnerBg extends android.graphics.drawable.Drawable {
        private final GradientDrawable box;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float d;

        SpinnerBg(Context c) {
            box = Theme.shape(Theme.SURFACE_RAISED, Theme.R_FIELD, c);
            box.setStroke(Theme.dp(c, 1), Theme.HAIRLINE);
            d = c.getResources().getDisplayMetrics().density;
            p.setColor(Theme.MINT);
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(2 * d);
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        }

        @Override public void draw(android.graphics.Canvas cv) {
            android.graphics.Rect r = getBounds();
            box.setBounds(r);
            box.draw(cv);
            float cx = r.left + 16 * d, cy = r.exactCenterY();
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(cx - 5 * d, cy - 2.5f * d);
            path.lineTo(cx, cy + 2.5f * d);
            path.lineTo(cx + 5 * d, cy - 2.5f * d);
            cv.drawPath(path, p);
        }

        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(android.graphics.ColorFilter f) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }

        @Override public boolean getPadding(android.graphics.Rect pad) {
            pad.set((int) (30 * d), (int) (6 * d), (int) (10 * d), (int) (6 * d));
            return true;
        }
    }

    /** Status colour for ticket cards. */
    static int statusColor(String status, boolean overdue) {
        if (overdue) return Theme.DANGER;
        if ("Open".equals(status)) return Theme.MINT;
        if ("In progress".equals(status)) return Theme.WARNING;
        if ("Waiting for client".equals(status)) return Theme.CALL_FG;
        if ("Waiting for outsourcing".equals(status)) return Theme.TEXT_SECONDARY;
        return Theme.TEXT_TERTIARY;
    }

    /** Puts a form inside a raised card on the charcoal canvas. */
    static View wrapCard(Context c, LinearLayout box) {
        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(c, Theme.S3), dp(c, Theme.S3), dp(c, Theme.S3), dp(c, Theme.S6));
        box.setBackground(Theme.cardBg(c));
        box.setElevation(0);
        outer.addView(box, cardParams(c));
        return outer;
    }
}
