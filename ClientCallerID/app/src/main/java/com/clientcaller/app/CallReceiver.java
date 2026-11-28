package com.clientcaller.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;

/** Fires when the phone starts ringing, and when a call ends. */
public class CallReceiver extends BroadcastReceiver {
    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context context, Intent intent) {
        if (!TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) return;
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            if (number != null && !number.isEmpty()) CallPopup.onIncoming(context, number);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            CallPopup.hide();
            // Give the phone a few seconds to write the call log, then add it to the calendar.
            final PendingResult pending = goAsync();
            final Context app = context.getApplicationContext();
            new Handler(Looper.getMainLooper()).postDelayed(() -> new Thread(() -> {
                try {
                    try { MissedCalls.check(app); } catch (Throwable ignored) { }
                    CallLogger.logLatestCall(app);
                } finally {
                    pending.finish();
                }
            }).start(), 3000);
        }
    }
}
