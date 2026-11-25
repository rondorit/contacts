package com.clientcaller.app;

import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

/**
 * Android's official caller-ID hook (active once the app is set as the
 * "Caller ID & spam app"). It never blocks calls; it only reads the number.
 */
public class CallerScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details details) {
        try {
            boolean incoming = Build.VERSION.SDK_INT < 29
                    || details.getCallDirection() == Call.Details.DIRECTION_INCOMING;
            Uri handle = details.getHandle();
            if (incoming && handle != null) {
                CallPopup.onIncoming(this, handle.getSchemeSpecificPart());
            }
        } catch (Exception ignored) {
        } finally {
            respondToCall(details, new CallResponse.Builder().build());
        }
    }
}
