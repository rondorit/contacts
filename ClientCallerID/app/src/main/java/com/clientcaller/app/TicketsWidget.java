package com.clientcaller.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Home-screen widget: open / overdue counts and a "new ticket" button. */
public class TicketsWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c);
    }

    static void updateAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, TicketsWidget.class));
            if (ids == null || ids.length == 0) return;
            int open = 0, overdue = 0;
            for (TicketDb.Ticket t : TicketDb.get(c).all()) {
                if (!t.isOpen()) continue;
                open++;
                if (t.isOverdue()) overdue++;
            }
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_tickets);
            v.setTextViewText(R.id.w_open, String.valueOf(open));
            v.setTextViewText(R.id.w_overdue, String.valueOf(overdue));
            v.setTextColor(R.id.w_overdue, overdue > 0 ? 0xFFFF6B6B : 0xFF8B949E);
            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            Intent list = new Intent(c, TicketsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.setOnClickPendingIntent(R.id.w_counts, PendingIntent.getActivity(c, 9980, list, flags));
            Intent late = new Intent(c, TicketsActivity.class).putExtra(TicketsActivity.EXTRA_FILTER, TicketsActivity.OVERDUE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.setOnClickPendingIntent(R.id.w_late_box, PendingIntent.getActivity(c, 9981, late, flags));
            Intent add = new Intent(c, TicketActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            v.setOnClickPendingIntent(R.id.w_new, PendingIntent.getActivity(c, 9982, add, flags));
            m.updateAppWidget(ids, v);
        } catch (Exception ignored) { }
    }
}
