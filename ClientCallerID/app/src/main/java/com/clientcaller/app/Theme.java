package com.clientcaller.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Deep Focus" design system: charcoal canvas, gently lifted surfaces, thin
 * hairlines and a single mint accent. Every colour, radius, spacing step and
 * text style used by the app lives here so the skin stays centralised.
 */
final class Theme {
    private Theme() { }

    // ---------------------------------------------------------------- palette
    static final int CANVAS = 0xFF0F1314;
    static final int SURFACE = 0xFF181E20;
    static final int SURFACE_RAISED = 0xFF212A2C;
    static final int HAIRLINE = 0xFF2C3739;
    static final int MINT = 0xFF3FDCA8;
    static final int MINT_CONTAINER = 0xFF16332B;
    static final int ON_MINT = 0xFF06231B;
    static final int TEXT_PRIMARY = 0xFFF3F7F6;
    static final int TEXT_SECONDARY = 0xFF9AAAA8;
    static final int TEXT_TERTIARY = 0xFF6E7F7D;
    static final int CALL_FG = 0xFF5FA8FF;
    static final int CALL_BG = 0xFF14283F;
    static final int WA_FG = 0xFF25D366;
    static final int WA_BG = 0xFF12301F;
    static final int DANGER = 0xFFFF7A7A;
    static final int WARNING = 0xFFFFC46B;

    // ---------------------------------------------------------------- spacing
    static final int S1 = 4;
    static final int S2 = 8;
    static final int S3 = 12;
    static final int S4 = 16;
    static final int S5 = 20;
    static final int S6 = 24;

    // ----------------------------------------------------------------- shapes
    static final int R_CARD = 20;
    static final int R_BUTTON = 16;
    static final int R_FIELD = 12;

    // ------------------------------------------------------------- type scale
    static final float DISPLAY = 28f;
    static final float TITLE = 20f;
    static final float SUBTITLE = 16f;
    static final float BODY = 15f;
    static final float LABEL = 13f;
    static final float CAPTION = 12f;

    static int dp(Context c, float v) {
        return (int) (v * c.getResources().getDisplayMetrics().density);
    }

    /** Plain rounded shape. */
    static GradientDrawable shape(int color, float radiusDp, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Card: surface fill, 20dp corners, 1dp hairline. Dark themes lift with value, not shadow. */
    static GradientDrawable cardBg(Context c) {
        GradientDrawable g = shape(SURFACE, R_CARD, c);
        g.setStroke(dp(c, 1), HAIRLINE);
        return g;
    }

    /** Raised surface for sheets, dialogs and pressed cards. */
    static GradientDrawable raisedBg(Context c) {
        GradientDrawable g = shape(SURFACE_RAISED, R_CARD, c);
        g.setStroke(dp(c, 1), HAIRLINE);
        return g;
    }

    /** Fully rounded pill. */
    static GradientDrawable pillBg(Context c, int color) {
        return shape(color, 999, c);
    }

    /** Tinted container that matches a foreground accent. */
    static int badgeBg(int fg) {
        if (fg == MINT) return MINT_CONTAINER;
        if (fg == CALL_FG) return CALL_BG;
        if (fg == WA_FG) return WA_BG;
        return (fg & 0x00FFFFFF) | 0x26000000;
    }

    static RippleDrawable ripple(Context c, Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    static Drawable pressable(Context c, Drawable content) {
        return ripple(c, content);
    }

    // ------------------------------------------------------------- typography
    /** 0 = regular, 1 = medium/semibold, 2 = bold. System sans-serif renders Hebrew via Noto Sans Hebrew. */
    static Typeface typeface(int weight) {
        if (weight >= 2) return Typeface.create("sans-serif", Typeface.BOLD);
        if (weight == 1) return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        return Typeface.create("sans-serif", Typeface.NORMAL);
    }

    static TextView text(Context c, String s, float sizeSp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(normalizeTextSize(sizeSp));
        t.setTextColor(color);
        t.setTypeface(typeface(weight));
        t.setLineSpacing(dp(c, 4), 1f);
        t.setLetterSpacing(0f);
        t.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        return t;
    }

    static float normalizeTextSize(float sizeSp) {
        if (sizeSp <= 12.25f) return CAPTION;
        if (sizeSp <= 14f) return LABEL;
        if (sizeSp <= 15.5f) return BODY;
        if (sizeSp <= 18f) return SUBTITLE;
        if (sizeSp <= 24f) return TITLE;
        return DISPLAY;
    }

    static TextView display(Context c, String s, int color) {
        return text(c, s, DISPLAY, color, 2);
    }

    static TextView title(Context c, String s, int color) {
        return text(c, s, TITLE, color, 1);
    }

    static TextView subtitle(Context c, String s, int color) {
        return text(c, s, SUBTITLE, color, 1);
    }

    static TextView body(Context c, String s, int color) {
        return text(c, s, BODY, color, 0);
    }

    static TextView label(Context c, String s, int color) {
        return text(c, s, LABEL, color, 1);
    }

    static TextView caption(Context c, String s, int color) {
        return text(c, s, CAPTION, color, 0);
    }

    /** Small muted section header above a group of cards. */
    static TextView sectionHeader(Context c, String s) {
        TextView t = label(c, s, TEXT_SECONDARY);
        t.setPadding(dp(c, S1), dp(c, S5), dp(c, S1), dp(c, S2));
        return t;
    }

    /** In-content screen title (the app has no action bar). */
    static TextView header(Context c, String s) {
        TextView t = title(c, s, TEXT_PRIMARY);
        t.setPadding(dp(c, S5), dp(c, S4), dp(c, S5), dp(c, S2));
        return t;
    }

    /** Pill badge: tinted container + matching foreground. */
    static TextView badge(Context c, String s, int fg) {
        TextView b = text(c, s, CAPTION, fg, 1);
        b.setBackground(pillBg(c, badgeBg(fg)));
        b.setPadding(dp(c, S3), dp(c, S1), dp(c, S3), dp(c, S1));
        b.setGravity(Gravity.CENTER);
        return b;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(HAIRLINE);
        return v;
    }

    // ---------------------------------------------------------------- buttons
    static GradientDrawable buttonShape(Context c, boolean primary) {
        GradientDrawable g = shape(primary ? MINT : SURFACE_RAISED, R_BUTTON, c);
        if (!primary) g.setStroke(dp(c, 1), HAIRLINE);
        return g;
    }

    /** Primary = mint fill + onMint label. Secondary = raised surface + hairline + textPrimary. */
    static void styleButton(Button b, boolean primary) {
        Context c = b.getContext();
        b.setAllCaps(false);
        b.setBackground(ripple(c, buttonShape(c, primary)));
        b.setTextColor(primary ? ON_MINT : TEXT_PRIMARY);
        b.setTypeface(typeface(1));
        b.setTextSize(BODY);
        b.setLetterSpacing(0f);
        b.setLineSpacing(dp(c, 4), 1f);
        b.setMinHeight(dp(c, 48));
        b.setMinimumHeight(dp(c, 48));
        b.setPadding(dp(c, S4), dp(c, S3), dp(c, S4), dp(c, S3));
        b.setStateListAnimator(null);
        b.setElevation(0);
    }

    static Button primaryButton(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        styleButton(b, true);
        return b;
    }

    static Button secondaryButton(Context c, String s) {
        Button b = new Button(c);
        b.setText(s);
        styleButton(b, false);
        return b;
    }

    // ----------------------------------------------------------------- fields
    /** Raised input box; the hairline turns mint while typing. */
    static Drawable fieldBg(Context c) {
        GradientDrawable normal = shape(SURFACE_RAISED, R_FIELD, c);
        normal.setStroke(dp(c, 1), HAIRLINE);
        GradientDrawable focused = shape(SURFACE_RAISED, R_FIELD, c);
        focused.setStroke(dp(c, 2), MINT);
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    // ------------------------------------------------------------------ cards
    static LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(cardBg(c));
        l.setElevation(0);
        int p = dp(c, S4);
        l.setPadding(p, p, p, p);
        return l;
    }

    static LinearLayout.LayoutParams cardParams(Context c) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = dp(c, S1);
        lp.setMargins(m, m, m, m);
        return lp;
    }

    // ------------------------------------------------------------ window chrome
    /** Canvas status + navigation bars with light icons, no action bar. */
    static void applyWindow(Activity a) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(CANVAS);
        w.setNavigationBarColor(CANVAS);
        w.setBackgroundDrawable(new ColorDrawable(CANVAS));
        View d = w.getDecorView();
        int flags = d.getSystemUiVisibility();
        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        d.setSystemUiVisibility(flags);
    }

    static void applyRtl(View v) {
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    }
}
