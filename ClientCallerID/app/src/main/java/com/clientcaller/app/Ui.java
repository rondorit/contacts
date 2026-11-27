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
    static final int LABEL = 0xFF475569;
    static final int FIELD = 0xFFF5F8FD;
    static final int FIELD_LINE = 0xFFD6E0EE;

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
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
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
        i.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(i);
        TextView ti = text(c, title, 14, TEXT, true);
        ti.setGravity(Gravity.CENTER);
        ti.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ti.setPadding(0, dp(c, 4), 0, 0);
        t.addView(ti);
        if (sub != null && !sub.isEmpty()) {
            TextView s = text(c, sub, 12, subColor, false);
            s.setGravity(Gravity.CENTER);
            s.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
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
        n.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(n);
        TextView lb = text(c, label, 13, MUTED, false);
        lb.setGravity(Gravity.CENTER);
        lb.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        t.addView(lb);
        t.setClickable(true);
        t.setOnClickListener(l);
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
            int m = dp(c, 5);
            lp.setMargins(m, m, m, m);
            row.addView(cells.get(i), lp);
        }
        int rest = cells.size() % cols;
        if (rest != 0 && row != null) {
            for (int i = rest; i < cols; i++) {
                View spacer = new View(c);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
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

    /** Rounded, colored buttons everywhere (big buttons = filled primary). Walks the view tree. */
    static void polish(View v) {
        if (v instanceof android.widget.Button && !(v instanceof android.widget.CompoundButton)) {
            android.widget.Button b = (android.widget.Button) v;
            Context c = b.getContext();
            float sp = c.getResources().getDisplayMetrics().scaledDensity;
            boolean primary = b.getTextSize() >= 17.5f * sp;
            GradientDrawable shape = rounded(primary ? PRIMARY : 0xFFE3EDFB, 12, c);
            b.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(0x33000000), shape, null));
            b.setTextColor(primary ? Color.WHITE : PRIMARY_DARK);
            b.setStateListAnimator(null);
            b.setPadding(dp(c, 10), dp(c, 10), dp(c, 10), dp(c, 10));
            if (!b.isEnabled()) b.setAlpha(0.4f);
            ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                int m = dp(c, 4);
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
            // Small grey form labels -> darker, bold labels.
            if (Math.abs(t.getTextSize() - 13 * sp) < 0.6f * sp && (col == 0xFF777777 || col == MUTED)) {
                t.setTextColor(LABEL);
                t.setTypeface(Typeface.DEFAULT_BOLD);
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

    /** Rounded light input box; the border turns blue while typing. */
    static android.graphics.drawable.Drawable fieldBg(Context c) {
        GradientDrawable normal = rounded(FIELD, 10, c);
        normal.setStroke(dp(c, 1), FIELD_LINE);
        GradientDrawable focused = rounded(Color.WHITE, 10, c);
        focused.setStroke(dp(c, 2), PRIMARY);
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    static void styleField(android.widget.EditText e) {
        Context c = e.getContext();
        if (e.getTag(R_STYLED) != null) return;
        e.setTag(R_STYLED, Boolean.TRUE);
        e.setBackground(fieldBg(c));
        e.setPadding(dp(c, 12), dp(c, 10), dp(c, 12), dp(c, 10));
        e.setTextColor(TEXT);
        e.setHintTextColor(0xFF9AA5B5);
        ViewGroup.LayoutParams lp = e.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            if (m.topMargin == 0) m.topMargin = dp(c, 4);
            if (m.bottomMargin == 0) m.bottomMargin = dp(c, 2);
            e.setLayoutParams(m);
        }
    }

    static void styleSpinner(android.widget.Spinner s) {
        Context c = s.getContext();
        if (s.getTag(R_STYLED) != null) return;
        s.setTag(R_STYLED, Boolean.TRUE);
        s.setBackground(new SpinnerBg(c));
        s.setPadding(dp(c, 8), dp(c, 6), dp(c, 8), dp(c, 6));
        s.setPopupBackgroundDrawable(rounded(Color.WHITE, 10, c));
        ViewGroup.LayoutParams lp = s.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) lp;
            if (m.topMargin == 0) m.topMargin = dp(c, 4);
            s.setLayoutParams(m);
        }
    }

    private static final int R_STYLED = 0x7f0f0001 + 0x1234;

    /** Rounded box with a small chevron on the left side (RTL end). */
    static final class SpinnerBg extends android.graphics.drawable.Drawable {
        private final GradientDrawable box;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float d;

        SpinnerBg(Context c) {
            box = rounded(FIELD, 10, c);
            box.setStroke(dp(c, 1), FIELD_LINE);
            d = c.getResources().getDisplayMetrics().density;
            p.setColor(PRIMARY);
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
        if (overdue) return RED;
        if ("Open".equals(status)) return PRIMARY;
        if ("In progress".equals(status)) return ORANGE;
        if ("Waiting for client".equals(status)) return 0xFF7B1FA2;
        if ("Waiting for outsourcing".equals(status)) return 0xFF00897B;
        return 0xFF9E9E9E;
    }

    /** Puts a form inside a white rounded card on the light background. */
    static View wrapCard(Context c, LinearLayout box) {
        LinearLayout outer = new LinearLayout(c);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(c, 8), dp(c, 8), dp(c, 8), dp(c, 40));
        box.setBackground(rounded(Color.WHITE, 16, c));
        box.setElevation(dp(c, 2));
        outer.addView(box, cardParams(c));
        return outer;
    }
}
