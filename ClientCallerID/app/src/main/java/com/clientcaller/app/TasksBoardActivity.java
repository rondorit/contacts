package com.clientcaller.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Kanban board: to do / in progress / done. Long-press a card and drag it to another column. */
public class TasksBoardActivity extends Activity {
    private static final String[] COLS = {"\uD83D\uDCCB \u05DC\u05D1\u05D9\u05E6\u05D5\u05E2", "\uD83D\uDD27 \u05D1\u05D8\u05D9\u05E4\u05D5\u05DC", "\u2705 \u05D1\u05D5\u05E6\u05E2"};
    private final LinearLayout[] cols = new LinearLayout[3];
    private final TextView[] heads = new TextView[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05DC\u05D5\u05D7 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 10);
        root.setPadding(p, p, p, 0);
        root.addView(Theme.header(this, "\uD83D\uDDC2\uFE0F \u05DC\u05D5\u05D7 \u05DE\u05E9\u05D9\u05DE\u05D5\u05EA"));
        root.addView(Ui.text(this, "\u05DC\u05D7\u05D9\u05E6\u05D4 \u05D0\u05E8\u05D5\u05DB\u05D4 \u05E2\u05DC \u05DB\u05E8\u05D8\u05D9\u05E1 \u05D5\u05D2\u05E8\u05D9\u05E8\u05D4 \u05DC\u05E2\u05DE\u05D5\u05D3\u05D4 \u05D0\u05D7\u05E8\u05EA", 13, Ui.MUTED, false));

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setFillViewport(true);
        LinearLayout board = new LinearLayout(this);
        int colW = (int) (getResources().getDisplayMetrics().widthPixels * 0.78f);
        for (int i = 0; i < 3; i++) {
            final int stage = i;
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setBackground(Ui.rounded(0x14FFFFFF, 14, this));
            col.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
            heads[i] = Ui.text(this, COLS[i], 16, Ui.TEXT, true);
            col.addView(heads[i]);
            ScrollView sv = new ScrollView(this);
            cols[i] = new LinearLayout(this);
            cols[i].setOrientation(LinearLayout.VERTICAL);
            cols[i].setMinimumHeight(Ui.dp(this, 400));
            sv.addView(cols[i]);
            col.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            col.setOnDragListener((v, e) -> onDrag(v, e, stage));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(colW, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 10));
            board.addView(col, lp);
        }
        hs.addView(board);
        root.addView(hs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        He.rtl(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        for (LinearLayout c : cols) c.removeAllViews();
        int[] n = new int[3];
        List<Tasks.Task> all = Tasks.all(this);
        long weekAgo = System.currentTimeMillis() - 7L * 24 * 3600_000L;
        for (final Tasks.Task t : all) {
            int st = t.isDone() ? 2 : t.stage == 1 ? 1 : 0;
            if (st == 2 && t.doneAt < weekAgo) continue; // done column: last 7 days
            n[st]++;
            cols[st].addView(card(t));
        }
        for (int i = 0; i < 3; i++) heads[i].setText(COLS[i] + " (" + n[i] + ")" + (i == 2 ? " \u2013 7 \u05D9\u05DE\u05D9\u05DD" : ""));
    }

    private View card(final Tasks.Task t) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.rounded(Theme.SURFACE, 12, this));
        int p = Ui.dp(this, 10);
        c.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, Ui.dp(this, 6), 0, 0);
        c.setLayoutParams(lp);
        if (!t.list.isEmpty()) c.addView(Ui.text(this, "\u25CF " + t.list, 12, TasksActivity.listColor(t.list), true));
        c.addView(Ui.text(this, ("High".equals(t.priority) ? "\uD83D\uDD34 " : "") + t.title, 15, t.isDone() ? Ui.MUTED : Ui.TEXT, true));
        StringBuilder sub = new StringBuilder();
        if (t.dueAt > 0) sub.append(t.isOverdue() ? "\u23F0 " : "\uD83D\uDCC5 ").append(TicketDb.fmt(t.dueAt));
        if (!t.items.isEmpty()) sub.append(sub.length() > 0 ? "  " : "").append(t.progress());
        if (!t.assignee.isEmpty()) sub.append(sub.length() > 0 ? "  " : "").append("\uD83D\uDC77 ").append(t.assignee);
        if (sub.length() > 0) c.addView(Ui.text(this, sub.toString(), 12, t.isOverdue() ? Ui.RED : Ui.MUTED, false));
        c.setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class).putExtra(Tasks.EXTRA_ID, t.id)));
        c.setOnLongClickListener(v -> {
            v.startDragAndDrop(ClipData.newPlainText("task", String.valueOf(t.id)), new View.DragShadowBuilder(v), t.id, 0);
            v.setAlpha(0.4f);
            return true;
        });
        return c;
    }

    private boolean onDrag(View col, DragEvent e, int stage) {
        switch (e.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return true;
            case DragEvent.ACTION_DRAG_ENTERED:
                col.setBackground(Ui.rounded(0x333FB68B, 14, this));
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
                col.setBackground(Ui.rounded(0x14FFFFFF, 14, this));
                return true;
            case DragEvent.ACTION_DROP:
                col.setBackground(Ui.rounded(0x14FFFFFF, 14, this));
                Object st = e.getLocalState();
                if (st instanceof Long) move((Long) st, stage);
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                col.setBackground(Ui.rounded(0x14FFFFFF, 14, this));
                refresh();
                return true;
        }
        return true;
    }

    private void move(long id, int stage) {
        Tasks.Task t = Tasks.load(this, id);
        if (t == null) return;
        if (stage == 2) {
            if (!t.isDone()) Tasks.setDone(this, id, true);
        } else {
            if (t.isDone()) Tasks.setDone(this, id, false);
            t = Tasks.load(this, id);
            t.stage = stage;
            Tasks.save(this, t);
        }
    }
}
