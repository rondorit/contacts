package com.clientcaller.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Location reminders for tasks (geofences): a notification when you arrive at the place. */
final class Geo {
    static final int REQ_FINE = 91, REQ_BACKGROUND = 92;
    static final int[] RADII = {100, 200, 500, 1000};

    private Geo() { }

    static boolean fineGranted(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean backgroundGranted(Context c) {
        return Build.VERSION.SDK_INT < 29
                || c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static PendingIntent intent(Context c) {
        Intent i = new Intent(c, Receiver.class);
        return PendingIntent.getBroadcast(c, 900000, i, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Adds, refreshes or removes the geofence of a task. */
    static void sync(Context c, Tasks.Task t) {
        try {
            GeofencingClient gc = LocationServices.getGeofencingClient(c);
            String id = "task_" + t.id;
            if (t.isDone() || !t.hasPlace() || !fineGranted(c) || !backgroundGranted(c)) {
                gc.removeGeofences(Collections.singletonList(id));
                return;
            }
            Geofence g = new Geofence.Builder()
                    .setRequestId(id)
                    .setCircularRegion(t.lat, t.lng, Math.max(100, t.radius))
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setNotificationResponsiveness(60_000)
                    .build();
            GeofencingRequest r = new GeofencingRequest.Builder().setInitialTrigger(0).addGeofence(g).build();
            gc.addGeofences(r, intent(c));
        } catch (SecurityException ignored) {
        } catch (Throwable ignored) { }
    }

    static void remove(Context c, long taskId) {
        try {
            LocationServices.getGeofencingClient(c).removeGeofences(Collections.singletonList("task_" + taskId));
        } catch (Throwable ignored) { }
    }

    /** Fires when entering a task's place. */
    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            GeofencingEvent ev = GeofencingEvent.fromIntent(intent);
            if (ev == null || ev.hasError() || ev.getGeofenceTransition() != Geofence.GEOFENCE_TRANSITION_ENTER) return;
            List<Geofence> list = ev.getTriggeringGeofences();
            if (list == null) return;
            for (Geofence g : list) {
                try {
                    long id = Long.parseLong(g.getRequestId().substring(5));
                    Tasks.Task t = Tasks.load(c, id);
                    if (t == null || t.isDone()) continue;
                    Tasks.Task shown = t;
                    shown.notes = "\uD83D\uDCCD \u05D4\u05D2\u05E2\u05EA \u05DC" + t.place + (t.notes.isEmpty() ? "" : "\n" + t.notes);
                    Tasks.notify(c, shown);
                } catch (Exception ignored) { }
            }
        }
    }

    // ------------------------------------------------------------------ picking a place

    interface Picked { void on(String place, double lat, double lng, int radius); }

    /** Asks for the place: current location, the client's address or a typed address. */
    static void pick(final Activity a, final String client, final Picked cb) {
        if (!fineGranted(a)) {
            new AlertDialog.Builder(a)
                    .setMessage("\u05DB\u05D3\u05D9 \u05DC\u05D4\u05D6\u05DB\u05D9\u05E8 \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC\u05DE\u05E7\u05D5\u05DD, \u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05E6\u05E8\u05D9\u05DB\u05D4 \u05D4\u05E8\u05E9\u05D0\u05EA \u05DE\u05D9\u05E7\u05D5\u05DD. \u05DC\u05D0\u05E9\u05E8 \u05E2\u05DB\u05E9\u05D9\u05D5?")
                    .setPositiveButton("\u05D0\u05D9\u05E9\u05D5\u05E8", (d, w) -> a.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_FINE))
                    .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                    .show();
            return;
        }
        final String addr = client == null || client.isEmpty() ? "" : clientAddress(a, client);
        String[] opts = addr.isEmpty()
                ? new String[]{"\uD83D\uDCCD \u05D4\u05DE\u05D9\u05E7\u05D5\u05DD \u05D4\u05E0\u05D5\u05DB\u05D7\u05D9 \u05E9\u05DC\u05D9", "\u2328\uFE0F \u05D4\u05E7\u05DC\u05D3 \u05DB\u05EA\u05D5\u05D1\u05EA", "\u2716 \u05DC\u05DC\u05D0 \u05DE\u05D9\u05E7\u05D5\u05DD"}
                : new String[]{"\uD83D\uDCCD \u05D4\u05DE\u05D9\u05E7\u05D5\u05DD \u05D4\u05E0\u05D5\u05DB\u05D7\u05D9 \u05E9\u05DC\u05D9", "\u2328\uFE0F \u05D4\u05E7\u05DC\u05D3 \u05DB\u05EA\u05D5\u05D1\u05EA", "\u2716 \u05DC\u05DC\u05D0 \u05DE\u05D9\u05E7\u05D5\u05DD", "\uD83C\uDFE0 \u05DB\u05EA\u05D5\u05D1\u05EA \u05D4\u05DC\u05E7\u05D5\u05D7: " + addr};
        new AlertDialog.Builder(a).setTitle("\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05D1\u05D4\u05D2\u05E2\u05D4 \u05DC\u2026").setItems(opts, (d, w) -> {
            if (w == 0) current(a, cb);
            else if (w == 1) typed(a, cb);
            else if (w == 2) cb.on("", 0, 0, 0);
            else geocode(a, addr, cb);
        }).show();
    }

    private static void current(final Activity a, final Picked cb) {
        Toast.makeText(a, "\u05DE\u05D0\u05EA\u05E8 \u05DE\u05D9\u05E7\u05D5\u05DD\u2026", Toast.LENGTH_SHORT).show();
        try {
            LocationServices.getFusedLocationProviderClient(a).getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(loc -> {
                        if (loc == null) {
                            Toast.makeText(a, "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0 \u05DE\u05D9\u05E7\u05D5\u05DD. \u05D5\u05D3\u05D0 \u05E9\u05D4-GPS \u05E4\u05D5\u05E2\u05DC", Toast.LENGTH_LONG).show();
                            return;
                        }
                        nameAndRadius(a, "\u05D4\u05DE\u05D9\u05E7\u05D5\u05DD \u05E9\u05E0\u05E9\u05DE\u05E8", loc.getLatitude(), loc.getLongitude(), cb);
                    })
                    .addOnFailureListener(e -> Toast.makeText(a, "\u05D0\u05D9\u05EA\u05D5\u05E8 \u05E0\u05DB\u05E9\u05DC: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (SecurityException e) {
            Toast.makeText(a, "\u05D0\u05D9\u05DF \u05D4\u05E8\u05E9\u05D0\u05EA \u05DE\u05D9\u05E7\u05D5\u05DD", Toast.LENGTH_SHORT).show();
        }
    }

    private static void typed(final Activity a, final Picked cb) {
        final EditText e = new EditText(a);
        e.setHint("\u05E8\u05D7\u05D5\u05D1, \u05DE\u05E1\u05E4\u05E8, \u05E2\u05D9\u05E8");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        new AlertDialog.Builder(a).setTitle("\u05DB\u05EA\u05D5\u05D1\u05EA").setView(e)
                .setPositiveButton("\u05D7\u05E4\u05E9", (d, w) -> {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty()) geocode(a, s, cb);
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null).show();
    }

    private static void geocode(final Activity a, final String address, final Picked cb) {
        Toast.makeText(a, "\u05DE\u05D7\u05E4\u05E9 \u05D0\u05EA \u05D4\u05DB\u05EA\u05D5\u05D1\u05EA\u2026", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Address found = null;
            String err = null;
            try {
                List<Address> r = new Geocoder(a, new Locale("he", "IL")).getFromLocationName(address, 1);
                if (r != null && !r.isEmpty()) found = r.get(0);
            } catch (Exception e) {
                err = e.getMessage();
            }
            final Address f = found;
            final String er = err;
            a.runOnUiThread(() -> {
                if (f == null) {
                    Toast.makeText(a, "\u05D4\u05DB\u05EA\u05D5\u05D1\u05EA \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4" + (er == null ? "" : ": " + er), Toast.LENGTH_LONG).show();
                    return;
                }
                nameAndRadius(a, address, f.getLatitude(), f.getLongitude(), cb);
            });
        }).start();
    }

    private static void nameAndRadius(final Activity a, final String name, final double lat, final double lng, final Picked cb) {
        String[] r = new String[RADII.length];
        for (int i = 0; i < RADII.length; i++) r[i] = "\u05D1\u05E8\u05D3\u05D9\u05D5\u05E1 " + (RADII[i] >= 1000 ? RADII[i] / 1000 + " \u05E7\"\u05DE" : RADII[i] + " \u05DE\u05D8\u05E8");
        new AlertDialog.Builder(a).setTitle("\uD83D\uDCCD " + name).setItems(r, (d, w) -> {
            cb.on(name, lat, lng, RADII[w]);
            ensureBackground(a);
        }).show();
    }

    /** Android needs "Allow all the time" for reminders while the app is closed. */
    static void ensureBackground(final Activity a) {
        if (backgroundGranted(a)) return;
        new AlertDialog.Builder(a)
                .setTitle("\u05D4\u05E8\u05E9\u05D0\u05EA \u05DE\u05D9\u05E7\u05D5\u05DD \u05D1\u05E8\u05E7\u05E2")
                .setMessage("\u05DB\u05D3\u05D9 \u05E9\u05D4\u05EA\u05D6\u05DB\u05D5\u05E8\u05EA \u05EA\u05E2\u05D1\u05D5\u05D3 \u05D2\u05DD \u05DB\u05E9\u05D4\u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D4 \u05E1\u05D2\u05D5\u05E8\u05D4, \u05D1\u05D7\u05E8 \u05D1\u05DE\u05E1\u05DA \u05D4\u05D1\u05D0 \"\u05D0\u05E4\u05E9\u05E8 \u05DB\u05DC \u05D4\u05D6\u05DE\u05DF\" (Allow all the time).")
                .setPositiveButton("\u05D4\u05DE\u05E9\u05DA", (d, w) -> a.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQ_BACKGROUND))
                .setNegativeButton("\u05DC\u05D0 \u05E2\u05DB\u05E9\u05D9\u05D5", null)
                .show();
    }

    static String clientAddress(Context c, String client) {
        try {
            ClientStore s = Data.get(c);
            for (int r = 0; r < s.rows.size(); r++) if (s.name(r).equalsIgnoreCase(client)) return Nav.address(c, s, r);
        } catch (Exception ignored) { }
        return "";
    }
}
