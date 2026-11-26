package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Shows the client card over the incoming-call screen, plus a notification. */
final class CallPopup {
    private static final String CHANNEL = "client_calls";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static View overlay;
    private static String tagOverride;
    private static String lastKey = "";
    private static long lastTime;

    private CallPopup() { }

    /** Called by both call hooks; shows the card once per call if the number is a client. */
    static void onIncoming(Context ctx, final String number) {
        final Context app = ctx.getApplicationContext();
        MAIN.post(() -> {
            ClientStore s = Data.get(app);
            int row = s.find(number);
            String contact = row < 0 ? PhoneContacts.name(app, number) : null;
            if (row < 0 && contact == null) return;
            String k = ClientStore.key(number);
            long now = System.currentTimeMillis();
            if (k.equals(lastKey) && now - lastTime < 20000) return;
            lastKey = k;
            lastTime = now;
            if (row >= 0) show(app, row);
            else if (Settings.canDrawOverlays(app)) showContactOverlay(app, contact, number);
        });
    }

    /**
     * WhatsApp call: we only get the name WhatsApp shows (contact name, or the number if not saved).
     * Find the client by number, by Excel name, or through the phone contact's numbers.
     */
    static String onWhatsApp(Context ctx, List<String> candidates) {
        Context app = ctx.getApplicationContext();
        ClientStore s = Data.get(app);
        int row = -1;
        String number = null, who = candidates.get(0);
        for (String shown : candidates) {
            String digits = shown.replaceAll("[^0-9]", "");
            if (digits.length() >= 8) {
                if (number == null) number = shown;
                row = s.find(shown);
            }
            if (row < 0) {
                for (int r = 0; r < s.rows.size(); r++) {
                    if (s.name(r).trim().equalsIgnoreCase(shown)) { row = r; break; }
                }
            }
            if (row < 0) {
                for (String ph : PhoneContacts.numbersForName(app, shown)) {
                    int r = s.find(ph);
                    if (r >= 0) { row = r; break; }
                    if (number == null) number = ph;
                }
            }
            if (row >= 0) { who = shown; break; }
        }
        String key = "wa:" + who;
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) && now - lastTime < 120000) return "\u05DB\u05D1\u05E8 \u05D4\u05D5\u05E6\u05D2 (" + who + ")";
        if (!Settings.canDrawOverlays(app)) return "\u05D0\u05D9\u05DF \u05D4\u05E8\u05E9\u05D0\u05EA \u05D4\u05E6\u05D2\u05D4 \u05DE\u05E2\u05DC \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D5\u05EA";
        if (row >= 0) {
            lastKey = key;
            lastTime = now;
            tagOverride = "\uD83D\uDCAC \u05DC\u05E7\u05D5\u05D7 \u05DE\u05EA\u05E7\u05E9\u05E8 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4";
            show(app, row);
            return "\u05D6\u05D5\u05D4\u05D4 \u05DB\u05DC\u05E7\u05D5\u05D7: " + s.name(row);
        }
        if (number != null) {
            lastKey = key;
            lastTime = now;
            tagOverride = "\uD83D\uDCAC \u05DE\u05EA\u05E7\u05E9\u05E8 \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4";
            showContactOverlay(app, who, number);
            return "\u05DC\u05D0 \u05D1\u05D0\u05E7\u05E1\u05DC, \u05D4\u05D5\u05E6\u05D2 \u05DB\u05D0\u05D9\u05E9 \u05E7\u05E9\u05E8: " + who;
        }
        return "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0 \u05D1\u05D0\u05E7\u05E1\u05DC \u05D5\u05DC\u05D0 \u05D1\u05D0\u05E0\u05E9\u05D9 \u05D4\u05E7\u05E9\u05E8: " + who;
    }

    static void show(Context ctx, int row) {
        Context app = ctx.getApplicationContext();
        notify(app, row);
        if (Settings.canDrawOverlays(app)) showOverlay(app, row);
    }

    static void hide() {
        MAIN.post(CallPopup::removeOverlay);
    }

    private static void removeOverlay() {
        if (overlay == null) return;
        try {
            WindowManager wm = (WindowManager) overlay.getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(overlay);
        } catch (Exception ignored) { }
        overlay = null;
    }

    /** Popup for a caller found in the phone's contacts (not in the Excel list). */
    private static void showContactOverlay(final Context app, final String name, final String number) {
        removeOverlay();
        float d = app.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(app);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int p = (int) (16 * d);
        box.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(18 * d);
        bg.setStroke((int) (2 * d), 0xFF43A047);
        box.setBackground(bg);
        box.setElevation(12 * d);

        TextView tag = new TextView(app);
        tag.setText(tagOverride != null ? tagOverride : "\u05DE\u05EA\u05E7\u05E9\u05E8 \u05DE\u05D0\u05E0\u05E9\u05D9 \u05D4\u05E7\u05E9\u05E8 \u05D1\u05D8\u05DC\u05E4\u05D5\u05DF");
        tagOverride = null;
        tag.setTextColor(0xFF43A047);
        tag.setTextSize(13);
        box.addView(tag);

        TextView nm = new TextView(app);
        nm.setText(name);
        nm.setTextColor(Color.BLACK);
        nm.setTextSize(22);
        nm.setTypeface(null, Typeface.BOLD);
        box.addView(nm);

        TextView ph = new TextView(app);
        ph.setText(number);
        ph.setTextColor(0xFF333333);
        ph.setTextSize(15);
        ph.setPadding(0, (int) (4 * d), 0, (int) (8 * d));
        box.addView(ph);

        try {
            java.util.List<String> keys = new java.util.ArrayList<>();
            keys.add(ClientStore.key(number));
            int openCount = 0;
            String last = "";
            for (TicketDb.Ticket t : TicketDb.get(app).forClient(name, keys)) {
                if (!t.isOpen()) continue;
                if (openCount == 0) last = "#" + t.id + " " + t.title + " (" + He.status(t.status) + ")";
                openCount++;
            }
            if (openCount > 0) {
                TextView tk = new TextView(app);
                tk.setText("\u1F3AB \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA: " + openCount + "\n" + last);
                tk.setTextColor(0xFFD32F2F);
                tk.setTextSize(15);
                tk.setPadding(0, 0, 0, (int) (8 * d));
                box.addView(tk);
            }
        } catch (Exception ignored) { }

        LinearLayout buttons = new LinearLayout(app);
        Button ticket = new Button(app);
        ticket.setText("\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4");
        ticket.setAllCaps(false);
        ticket.setOnClickListener(v -> {
            removeOverlay();
            Intent i = new Intent(app, TicketActivity.class)
                    .putExtra(TicketActivity.EXTRA_NAME, name)
                    .putExtra(TicketActivity.EXTRA_PHONE, number)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { app.startActivity(i); } catch (Exception ignored) { }
        });
        Button close = new Button(app);
        close.setText("\u05E1\u05D2\u05D5\u05E8");
        close.setAllCaps(false);
        close.setOnClickListener(v -> removeOverlay());
        buttons.addView(ticket, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(close, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(buttons);

        FrameLayout frame = new FrameLayout(app);
        int m = (int) (12 * d);
        frame.setPadding(m, m, m, m);
        frame.addView(box);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP;
        lp.y = (int) (70 * d);
        try {
            ((WindowManager) app.getSystemService(Context.WINDOW_SERVICE)).addView(frame, lp);
            overlay = frame;
        } catch (Exception ignored) { }
    }

    private static void showOverlay(final Context app, final int row) {
        removeOverlay();
        ClientStore s = Data.get(app);
        float d = app.getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(app);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int p = (int) (16 * d);
        box.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(18 * d);
        bg.setStroke((int) (2 * d), 0xFF1E88E5);
        box.setBackground(bg);
        box.setElevation(12 * d);

        TextView tag = new TextView(app);
        tag.setText(tagOverride != null ? tagOverride : "\u05DC\u05E7\u05D5\u05D7 \u05DE\u05EA\u05E7\u05E9\u05E8");
        tagOverride = null;
        tag.setTextColor(0xFF1E88E5);
        tag.setTextSize(13);
        box.addView(tag);

        TextView name = new TextView(app);
        name.setText(s.name(row));
        name.setTextColor(Color.BLACK);
        name.setTextSize(22);
        name.setTypeface(null, Typeface.BOLD);
        box.addView(name);

        TextView body = new TextView(app);
        body.setText(s.details(row));
        body.setTextColor(0xFF333333);
        body.setTextSize(15);
        body.setMaxLines(10);
        body.setEllipsize(TextUtils.TruncateAt.END);
        body.setPadding(0, (int) (6 * d), 0, (int) (8 * d));
        box.addView(body);

        // Open tickets of this client, so you know what the call is probably about.
        try {
            int openCount = 0;
            String last = "";
            for (TicketDb.Ticket t : TicketDb.get(app).forClient(s.name(row), s.keysForRow(row))) {
                if (!t.isOpen()) continue;
                if (openCount == 0) last = "#" + t.id + " " + t.title + " (" + He.status(t.status) + ")";
                openCount++;
            }
            if (openCount > 0) {
                TextView tk = new TextView(app);
                tk.setText("\uD83C\uDFAB \u05E7\u05E8\u05D9\u05D0\u05D5\u05EA \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA: " + openCount + "\n" + last);
                tk.setTextColor(0xFFD32F2F);
                tk.setTextSize(15);
                tk.setPadding(0, 0, 0, (int) (8 * d));
                box.addView(tk);
            }
        } catch (Exception ignored) { }

        LinearLayout buttons = new LinearLayout(app);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button ticket = new Button(app);
        ticket.setText("\u05E7\u05E8\u05D9\u05D0\u05D4 \u05D7\u05D3\u05E9\u05D4");
        ticket.setAllCaps(false);
        ticket.setOnClickListener(v -> {
            removeOverlay();
            Intent i = new Intent(app, TicketActivity.class)
                    .putExtra(DetailActivity.EXTRA_ROW, row)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { app.startActivity(i); } catch (Exception ignored) { }
        });
        buttons.addView(ticket, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button open = new Button(app);
        open.setText("\u05DB\u05E8\u05D8\u05D9\u05E1");
        open.setAllCaps(false);
        open.setOnClickListener(v -> {
            removeOverlay();
            Intent i = new Intent(app, DetailActivity.class)
                    .putExtra(DetailActivity.EXTRA_ROW, row)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { app.startActivity(i); } catch (Exception ignored) { }
        });
        Button close = new Button(app);
        close.setText("\u05E1\u05D2\u05D5\u05E8");
        close.setAllCaps(false);
        close.setOnClickListener(v -> removeOverlay());
        buttons.addView(open, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        buttons.addView(close, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        box.addView(buttons);

        FrameLayout frame = new FrameLayout(app);
        int m = (int) (12 * d);
        frame.setPadding(m, m, m, m);
        frame.addView(box);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP;
        lp.y = (int) (70 * d);

        try {
            WindowManager wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
            wm.addView(frame, lp);
            overlay = frame;
        } catch (Exception ignored) { }
    }

    private static void notify(Context app, int row) {
        try {
            ClientStore s = Data.get(app);
            NotificationManager nm = app.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "\u05E9\u05D9\u05D7\u05D5\u05EA \u05DC\u05E7\u05D5\u05D7\u05D5\u05EA", NotificationManager.IMPORTANCE_HIGH));
            Intent i = new Intent(app, DetailActivity.class)
                    .putExtra(DetailActivity.EXTRA_ROW, row)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(app, row, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String details = s.details(row);
            int nl = details.indexOf('\n');
            Notification n = new Notification.Builder(app, CHANNEL)
                    .setSmallIcon(android.R.drawable.sym_call_incoming)
                    .setContentTitle("\u05DC\u05E7\u05D5\u05D7 \u05DE\u05EA\u05E7\u05E9\u05E8: " + s.name(row))
                    .setContentText(nl < 0 ? details : details.substring(0, nl))
                    .setStyle(new Notification.BigTextStyle().bigText(details))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(1000 + row, n);
        } catch (Exception ignored) { }
    }
}
