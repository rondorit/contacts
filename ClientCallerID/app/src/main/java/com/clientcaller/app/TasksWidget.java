package com.clientcaller.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

/** Home-screen widget with the next open tasks: tick to complete, tap to open, + for a new task. */
public class TasksWidget extends AppWidgetProvider {
    private static final int[] ROW = {R.id.wt_row0, R.id.wt_row1, R.id.wt_row2, R.id.wt_row3, R.id.wt_row4};
    private static final int[] DONE = {R.id.wt_done0, R.id.wt_done1, R.id.wt_done2, R.id.wt_done3, R.id.wt_done4};
    private static final int[] OPEN = {R.id.wt_open0, R.id.wt_open1, R.id.wt_open2, R.id.wt_open3, R.id.wt_open4};
    private static final int[] TITLE = {R.id.wt_title0, R.id.wt_title1, R.id.wt_title2, R.id.wt_title3, R.id.wt_title4};
    private static final int[] SUB = {R.id.wt_sub0, R.id.wt_sub1, R.id.wt_sub2, R.id.wt_sub3, R.id.wt_sub4};

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        updateAll(c);
    }

    static void updateAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, TasksWidget.class));
            if (ids == null || ids.length == 0) return;
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_tasks);
            int flags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
            List<Tasks.Task> all = Tasks.all(c);
            int open = 0, late = 0;
            for (Tasks.Task t : all) if (!t.isDone()) { open++; if (t.isOverdue()) late++; }
            v.setTextViewText(R.id.wt_head, "\u2705 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA (" + open + ")"
                    + (late > 0 ? "  \u2022  " + late + " \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8" : ""));
            Intent list = new Intent(c, TasksActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            v.setOnClickPendingIntent(R.id.wt_head, PendingIntent.getActivity(c, 9970, list, flags));
            Intent add = new Intent(c, TasksActivity.class).putExtra("new", true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            v.setOnClickPendingIntent(R.id.wt_new, PendingIntent.getActivity(c, 9971, add, flags));
            int k = 0;
            for (Tasks.Task t : all) {
                if (t.isDone() || k >= ROW.length) continue;
                v.setViewVisibility(ROW[k], View.VISIBLE);
                v.setTextViewText(TITLE[k], ("High".equals(t.priority) ? "\uD83D\uDD34 " : "") + t.title);
                StringBuilder sub = new StringBuilder();
                if (t.dueAt > 0) sub.append(t.isOverdue() ? "\u23F0 " : "").append(TicketDb.fmt(t.dueAt));
                if (t.hasPlace()) sub.append(sub.length() > 0 ? "  \u2022  " : "").append("\uD83D\uDCCD ").append(t.place);
                if (!t.client.isEmpty()) sub.append(sub.length() > 0 ? "  \u2022  " : "").append(t.client);
                v.setTextViewText(SUB[k], sub.toString());
                v.setTextColor(SUB[k], t.isOverdue() ? 0xFFFF6B6B : 0xFF8B949E);
                v.setViewVisibility(SUB[k], sub.length() == 0 ? View.GONE : View.VISIBLE);
                Intent done = new Intent(c, FollowupReceiver.class).setAction(Tasks.ACTION_DONE).putExtra(Tasks.EXTRA_ID, t.id);
                v.setOnClickPendingIntent(DONE[k], PendingIntent.getBroadcast(c, 810000 + (int) t.id, done, flags));
                Intent op = new Intent(c, TasksActivity.class).putExtra(Tasks.EXTRA_ID, t.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                v.setOnClickPendingIntent(OPEN[k], PendingIntent.getActivity(c, 820000 + (int) t.id, op, flags));
                k++;
            }
            for (int r = k; r < ROW.length; r++) v.setViewVisibility(ROW[r], View.GONE);
            v.setViewVisibility(R.id.wt_empty, k == 0 ? View.VISIBLE : View.GONE);
            m.updateAppWidget(ids, v);
        } catch (Exception ignored) { }
    }
}
