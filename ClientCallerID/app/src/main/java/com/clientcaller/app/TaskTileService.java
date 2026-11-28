package com.clientcaller.app;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile: "+ \u05DE\u05E9\u05D9\u05DE\u05D4" opens a new task (long press = by voice). */
public class TaskTileService extends TileService {
    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t == null) return;
        int open = 0;
        try { open = Tasks.openCount(this); } catch (Exception ignored) { }
        t.setLabel("\u05DE\u05E9\u05D9\u05DE\u05D4 \u05D7\u05D3\u05E9\u05D4");
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(open == 0 ? "\u05D0\u05D9\u05DF \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA" : open + " \u05E4\u05EA\u05D5\u05D7\u05D5\u05EA");
        t.setState(Tile.STATE_ACTIVE);
        t.updateTile();
    }

    @Override
    public void onClick() {
        Intent i = new Intent(this, TasksActivity.class).putExtra("new", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 9960, i,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        } else {
            startActivityAndCollapse(i);
        }
    }
}
