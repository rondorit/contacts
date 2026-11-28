package com.clientcaller.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Team map: the technician shares his location while this service runs (he turns it on himself;
 * a notification is shown the whole time). Only the latest location is kept:
 * orgs/{org}/locations/{uid}. Also records arrival at the address of an open ticket.
 */
public class LocationShare extends Service {

    static final String ACTION_STOP = "com.clientcaller.app.LOC_STOP";
    private static final int NID = 650001;
    private static final float ARRIVE_M = 150f;
    private FusedLocationProviderClient fused;
    private LocationCallback cb;

    static boolean running(Context c) {
        return CallLogger.prefs(c).getBoolean("loc_share", false);
    }

    static boolean canStart(Context c) {
        return c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || c.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    static void start(Context c) {
        CallLogger.prefs(c).edit().putBoolean("loc_share", true).apply();
        c.startForegroundService(new Intent(c, LocationShare.class));
    }

    static void stop(Context c) {
        CallLogger.prefs(c).edit().putBoolean("loc_share", false).apply();
        c.stopService(new Intent(c, LocationShare.class));
        try {
            if (Cloud.user() != null && !Cloud.org().isEmpty()) {
                Map<String, Object> m = new HashMap<>();
                m.put("active", false);
                m.put("at", System.currentTimeMillis());
                Cloud.col("locations").document(Cloud.user().getUid()).set(m, SetOptions.merge());
            }
        } catch (Throwable ignored) { }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stop(this);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!running(this) || !canStart(this) || Cloud.user() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("loc", "\u05E9\u05D9\u05EA\u05D5\u05E3 \u05DE\u05D9\u05E7\u05D5\u05DD \u05E2\u05DD \u05D4\u05E6\u05D5\u05D5\u05EA", NotificationManager.IMPORTANCE_LOW));
        int fl = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        Notification n = new Notification.Builder(this, "loc")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("\uD83D\uDCCD \u05D4\u05DE\u05D9\u05E7\u05D5\u05DD \u05E9\u05DC\u05DA \u05DE\u05E9\u05D5\u05EA\u05E3 \u05E2\u05DD \u05D4\u05DE\u05E0\u05D4\u05DC")
                .setContentText("\u05E8\u05E7 \u05D4\u05DE\u05D9\u05E7\u05D5\u05DD \u05D4\u05D0\u05D7\u05E8\u05D5\u05DF \u05E0\u05E9\u05DE\u05E8. \u05E0\u05DB\u05D1\u05D4 \u05D0\u05D5\u05D8\u05D5\u05DE\u05D8\u05D9\u05EA \u05D1\u05BE20:00.")
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this, NID, new Intent(this, CloudActivity.class), fl))
                .addAction(new Notification.Action.Builder(null, "\u23F9 \u05D4\u05E4\u05E1\u05E7 \u05E9\u05D9\u05EA\u05D5\u05E3",
                        PendingIntent.getService(this, NID + 1, new Intent(this, LocationShare.class).setAction(ACTION_STOP), fl)).build())
                .build();
        startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        if (fused == null) {
            fused = LocationServices.getFusedLocationProviderClient(this);
            cb = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult r) {
                    Location l = r.getLastLocation();
                    if (l != null) onLocation(l);
                }
            };
            LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3 * 60 * 1000L)
                    .setMinUpdateIntervalMillis(60 * 1000L).setMinUpdateDistanceMeters(50).build();
            try {
                fused.requestLocationUpdates(req, cb, Looper.getMainLooper());
            } catch (SecurityException e) {
                stopSelf();
            }
        }
        return START_STICKY;
    }

    private void onLocation(Location l) {
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) >= 20) {   // end of work day
            stop(this);
            stopSelf();
            return;
        }
        try {
            Map<String, Object> m = new HashMap<>();
            m.put("lat", l.getLatitude());
            m.put("lng", l.getLongitude());
            m.put("acc", (double) l.getAccuracy());
            m.put("at", System.currentTimeMillis());
            m.put("active", true);
            m.put("name", Cloud.user().getDisplayName() == null ? Cloud.email() : Cloud.user().getDisplayName());
            Cloud.col("locations").document(Cloud.user().getUid()).set(m, SetOptions.merge());
        } catch (Throwable ignored) { }
        final double lat = l.getLatitude(), lng = l.getLongitude();
        new Thread(() -> checkArrival(lat, lng)).start();
    }

    /** Arrival: within 150 m of the address of one of my open tickets -> note + cloud "arrivedAt". */
    private void checkArrival(double lat, double lng) {
        try {
            boolean admin = Sync.admin();
            String day = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
            for (TicketDb.Ticket t : TicketDb.get(this).all()) {
                if (!t.isOpen()) continue;
                if (admin && !t.assignee.isEmpty()) continue;   // the manager: only his own tickets
                String key = "arr_" + t.id + "_" + day;
                if (CallLogger.prefs(this).getBoolean(key, false)) continue;
                double[] p = place(t);
                if (p == null) continue;
                float[] d = new float[1];
                Location.distanceBetween(lat, lng, p[0], p[1], d);
                if (d[0] > ARRIVE_M) continue;
                CallLogger.prefs(this).edit().putBoolean(key, true).apply();
                String hm = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                TicketDb.get(this).addNote(t.id, "\uD83D\uDCCD \u05D4\u05D2\u05E2\u05D4 \u05DC\u05DC\u05E7\u05D5\u05D7 " + hm);
                Sync.arrived(t.id);
            }
        } catch (Throwable ignored) { }
    }

    private double[] place(TicketDb.Ticket t) {
        String k = "tgeo_" + t.id;
        String c = CallLogger.prefs(this).getString(k, null);
        if (c != null) {
            if (c.isEmpty()) return null;
            String[] a = c.split(",");
            return new double[]{Double.parseDouble(a[0]), Double.parseDouble(a[1])};
        }
        String addr = Sync.address(t);
        String v = "";
        if (!addr.isEmpty()) {
            try {
                List<Address> r = new Geocoder(this, new Locale("he", "IL")).getFromLocationName(addr, 1);
                if (r != null && !r.isEmpty()) v = r.get(0).getLatitude() + "," + r.get(0).getLongitude();
            } catch (Exception ignored) {
                return null;   // network problem: try again next time
            }
        }
        CallLogger.prefs(this).edit().putString(k, v).apply();
        return v.isEmpty() ? null : place(t);
    }

    @Override
    public void onDestroy() {
        if (fused != null && cb != null) fused.removeLocationUpdates(cb);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }
}
