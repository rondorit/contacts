package com.clientcaller.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

/** Looks up a caller in the phone's own contacts list. */
final class PhoneContacts {
    static final String KEY_ENABLED = "use_contacts";

    private PhoneContacts() { }

    static boolean enabled(Context c) {
        return CallLogger.prefs(c).getBoolean(KEY_ENABLED, true)
                && c.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    /** Contact name for this number, or null. */
    static String name(Context c, String number) {
        if (number == null || number.trim().isEmpty() || !enabled(c)) return null;
        Uri u = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        try (Cursor cur = c.getContentResolver().query(u,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                String n = cur.getString(0);
                return n == null || n.trim().isEmpty() ? null : n.trim();
            }
        } catch (Exception ignored) { }
        return null;
    }
}
