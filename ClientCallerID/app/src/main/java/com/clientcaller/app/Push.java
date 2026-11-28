package com.clientcaller.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Cloud push notifications (sent by the Firebase function in functions/index.js):
 * new ticket/task assigned to me, notes, status changes by technicians.
 * The device token is stored in users/{uid}.fcmTokens.
 */
public class Push extends FirebaseMessagingService {

    /** Called after sign-in / on app start: registers this phone for pushes. */
    static void register(Context c) {
        try {
            if (Cloud.user() == null) return;
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(Push::save);
        } catch (Throwable ignored) { }
    }

    /** On sign-out: stop pushes to this phone for the old account. */
    static void unregister() {
        try {
            if (Cloud.user() == null) return;
            final String uid = Cloud.user().getUid();
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(t -> {
                Map<String, Object> m = new HashMap<>();
                m.put("fcmTokens", FieldValue.arrayRemove(t));
                Cloud.db().collection("users").document(uid).set(m, SetOptions.merge());
            });
        } catch (Throwable ignored) { }
    }

    private static void save(String token) {
        if (token == null || Cloud.user() == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("fcmTokens", FieldValue.arrayUnion(token));
        Cloud.db().collection("users").document(Cloud.user().getUid()).set(m, SetOptions.merge());
    }

    @Override
    public void onNewToken(String token) {
        save(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Map<String, String> d = msg.getData();
        String title = d.get("title"), body = d.get("body");
        if (title == null && msg.getNotification() != null) {
            title = msg.getNotification().getTitle();
            body = msg.getNotification().getBody();
        }
        if (title == null) return;
        Context c = getApplicationContext();
        String kind = String.valueOf(d.get("kind")), doc = String.valueOf(d.get("doc"));
        // Pull the change into the phone right away.
        try { Sync.start(c); } catch (Throwable ignored) { }
        Intent open = Sync.openIntent(c, kind, doc).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("cloud", "\u05E2\u05D3\u05DB\u05D5\u05E0\u05D9 \u05E6\u05D5\u05D5\u05EA (\u05E2\u05E0\u05DF)", NotificationManager.IMPORTANCE_HIGH));
        int nid = 700000 + Math.abs(doc.hashCode() % 90000);
        nm.notify(nid, new Notification.Builder(c, "cloud")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle(title)
                .setContentText(body == null ? "" : body)
                .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(c, nid, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
                .build());
    }
}
