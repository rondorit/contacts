package com.clientcaller.app;

import android.content.Context;

import java.io.File;

/** Keeps the imported client list in memory and on the phone's private storage. */
final class Data {
    static final String FILE = "clients.tsv";
    private static ClientStore cache;

    private Data() { }

    static synchronized ClientStore get(Context ctx) {
        if (cache == null) {
            cache = new ClientStore();
            File f = new File(ctx.getApplicationContext().getFilesDir(), FILE);
            if (f.exists()) {
                try { cache.loadFile(f); } catch (Exception ignored) { }
            }
        }
        return cache;
    }

    static synchronized void set(ClientStore s) {
        cache = s;
    }
}
