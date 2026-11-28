package com.clientcaller.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** "My day": overdue and today's tasks, ticket follow-ups, quote follow-ups and recurring jobs, in time order. */
public class TodayActivity extends Activity {
    private LinearLayout list;

    private static final class Item {
        long when;
        boolean late;
        String icon, title, sub;
        Intent open;
        Runnable done;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05D4\u05D9\u05D5\u05DD \u05E9\u05DC\u05D9");
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 12);
        box.setPadding(p, p, p, p);
        box.addView(Theme.header(this, "\uD83D\uDCC5 \u05D4\u05D9\u05D5\u05DD \u05E9\u05DC\u05D9"));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        box.addView(list);
        sv.addView(box);
        setContentView(sv);
        He.rtl(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        long eod = end.getTimeInMillis(), now = System.currentTimeMillis();
        List<Item> items = new ArrayList<>();

        for (final Tasks.Task t : Tasks.all(this)) {
            if (t.isDone() || t.dueAt == 0 || t.dueAt > eod) continue;
            Item it = new Item();
            it.when = t.dueAt;
            it.late = t.dueAt < now;
            it.icon = t.title.startsWith("\uD83D\uDCDE") ? "" : "\u2705 ";
            it.title = t.title;
            it.sub = "\u05DE\u05E9\u05D9\u05DE\u05D4" + (t.client.isEmpty() ? "" : " \u2022 " + t.client) + (t.items.isEmpty() ? "" : " \u2022 " + t.progress());
            it.open = new Intent(this, TasksActivity.class).putExtra(Tasks.EXTRA_ID, t.id);
            it.done = () -> Tasks.setDone(this, t.id, true);
            items.add(it);
        }
        for (TicketDb.Ticket t : TicketDb.get(this).all()) {
            if (!t.isOpen() || t.followupAt == 0 || t.followupAt > eod) continue;
            Item it = new Item();
            it.when = t.followupAt;
            it.late = t.followupAt < now;
            it.icon = "\uD83C\uDFAB ";
            it.title = "#" + t.id + " " + t.title;
            it.sub = "\u05DE\u05E2\u05E7\u05D1 \u05E7\u05E8\u05D9\u05D0\u05D4 \u2022 " + t.clientName + " \u2022 " + He.status(t.status);
            it.open = new Intent(this, TicketActivity.class).putExtra(TicketActivity.EXTRA_TICKET, t.id);
            items.add(it);
        }
        for (QuoteDb.Quote q : QuoteDb.get(this).all()) {
            if (!"Sent".equals(q.status) || q.followupAt == 0 || q.followupAt > eod) continue;
            Item it = new Item();
            it.when = q.followupAt;
            it.late = q.followupAt < now;
            it.icon = "\uD83E\uDDFE ";
            it.title = "\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8 " + q.id + " \u2013 " + q.clientName;
            it.sub = "\u05DE\u05E2\u05E7\u05D1 \u05D4\u05E6\u05E2\u05D4 \u2022 " + QuoteDb.money(q.total());
            it.open = new Intent(this, QuoteActivity.class).putExtra(QuoteActivity.EXTRA_QUOTE, q.id);
            items.add(it);
        }
        for (Recurring.Task r : Recurring.all(this)) {
            if (!r.active || r.nextAt == 0 || r.nextAt > eod) continue;
            Item it = new Item();
            it.when = r.nextAt;
            it.icon = "\uD83D\uDD01 ";
            it.title = r.title;
            it.sub = "\u05EA\u05D7\u05D6\u05D5\u05E7\u05D4 \u05DE\u05D7\u05D6\u05D5\u05E8\u05D9\u05EA" + (r.client.isEmpty() ? "" : " \u2022 " + r.client);
            items.add(it);
        }
        Collections.sort(items, (a, b) -> Long.compare(a.when, b.when));

        int late = 0;
        for (Item it : items) if (it.late) late++;
        TextView sum = Ui.text(this, items.isEmpty() ? "\u05D0\u05D9\u05DF \u05DE\u05E9\u05D4\u05D5 \u05E4\u05EA\u05D5\u05D7 \u05DC\u05D4\u05D9\u05D5\u05DD \uD83C\uDF89"
                : items.size() + " \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD \u05DC\u05D4\u05D9\u05D5\u05DD" + (late > 0 ? " \u2022 " + late + " \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8" : ""), 15, late > 0 ? Ui.RED : Ui.MUTED, true);
        sum.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        list.addView(sum);

        boolean lateHead = false, todayHead = false;
        for (final Item it : items) {
            if (it.late && !lateHead) { list.addView(Ui.sectionTitle(this, "\u23F0 \u05D1\u05D0\u05D9\u05D7\u05D5\u05E8")); lateHead = true; }
            if (!it.late && !todayHead) { list.addView(Ui.sectionTitle(this, "\u2600\uFE0F \u05D4\u05D9\u05D5\u05DD")); todayHead = true; }
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setBackground(Ui.rounded(Theme.SURFACE, 12, this));
            int p = Ui.dp(this, 10);
            card.setPadding(p, p, p, p);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
            TextView time = Ui.text(this, String.format(java.util.Locale.ROOT, "%tH:%<tM", it.when), 15, it.late ? Ui.RED : Ui.PRIMARY, true);
            time.setPadding(0, 0, 0, 0);
            card.addView(time, new LinearLayout.LayoutParams(Ui.dp(this, 54), LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout txt = new LinearLayout(this);
            txt.setOrientation(LinearLayout.VERTICAL);
            txt.addView(Ui.text(this, it.icon + it.title, 15, Ui.TEXT, true));
            txt.addView(Ui.text(this, it.sub, 12, Ui.MUTED, false));
            card.addView(txt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            if (it.done != null) {
                TextView ok = Ui.text(this, "\u2714", 20, Ui.PRIMARY, true);
                ok.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
                ok.setOnClickListener(v -> { it.done.run(); refresh(); });
                card.addView(ok);
            }
            if (it.open != null) card.setOnClickListener(v -> startActivity(it.open));
            list.addView(card, lp);
        }
    }
}
