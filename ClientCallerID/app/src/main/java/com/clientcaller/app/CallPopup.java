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

/** Shows the client card over the incoming-call screen, plus a notification. */
final class CallPopup {
    private static final String CHANNEL = "client_calls";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static View overlay;
    private static String lastKey = "";
    private static long lastTime;

    private CallPopup() { }

    /** Called by both call hooks; shows the card once per call if the number is a client. */
    static void onIncoming(Context ctx, final String number) {
        final Context app = ctx.getApplicationContext();
        MAIN.post(() -> {
            ClientStore s = Data.get(app);
            int row = s.find(number);
            if (row < 0) return;
            String k = ClientStore.key(number);
            long now = System.currentTimeMillis();
            if (k.equals(lastKey) && now - lastTime < 20000) return;
            lastKey = k;
            lastTime = now;
            show(app, row);
        });
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

    private static void showOverlay(final Context app, final int row) {
        removeOverlay();
        ClientStore s = Data.get(app);
        float d = app.getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(app);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * d);
        box.setPadding(p, p, p, p);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(18 * d);
        bg.setStroke((int) (2 * d), 0xFF1E88E5);
        box.setBackground(bg);
        box.setElevation(12 * d);

        TextView tag = new TextView(app);
        tag.setText("Client calling");
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

        LinearLayout buttons = new LinearLayout(app);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button open = new Button(app);
        open.setText("Open card");
        open.setAllCaps(false);
        open.setOnClickListener(v -> {
            removeOverlay();
            Intent i = new Intent(app, DetailActivity.class)
                    .putExtra(DetailActivity.EXTRA_ROW, row)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { app.startActivity(i); } catch (Exception ignored) { }
        });
        Button close = new Button(app);
        close.setText("Close");
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
                    CHANNEL, "Client calls", NotificationManager.IMPORTANCE_HIGH));
            Intent i = new Intent(app, DetailActivity.class)
                    .putExtra(DetailActivity.EXTRA_ROW, row)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(app, row, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            String details = s.details(row);
            int nl = details.indexOf('\n');
            Notification n = new Notification.Builder(app, CHANNEL)
                    .setSmallIcon(android.R.drawable.sym_call_incoming)
                    .setContentTitle("Client calling: " + s.name(row))
                    .setContentText(nl < 0 ? details : details.substring(0, nl))
                    .setStyle(new Notification.BigTextStyle().bigText(details))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();
            nm.notify(1000 + row, n);
        } catch (Exception ignored) { }
    }
}
