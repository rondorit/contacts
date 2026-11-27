package com.clientcaller.app;

import android.app.Activity;
import android.content.Context;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.Toast;

/** Fingerprint (or phone lock code) confirmation before deleting data. */
final class Auth {
    static final String KEY_ENABLED = "auth_delete";

    private Auth() { }

    static boolean enabled(Context c) {
        return CallLogger.prefs(c).getBoolean(KEY_ENABLED, true);
    }

    /** Runs action only after the user confirms with fingerprint / face / phone lock. */
    static void confirm(final Activity a, String what, final Runnable action) {
        if (!enabled(a) || Build.VERSION.SDK_INT < 28) {
            action.run();
            return;
        }
        try {
            BiometricPrompt.Builder b = new BiometricPrompt.Builder(a)
                    .setTitle("\u05D0\u05D9\u05E9\u05D5\u05E8 \u05DE\u05D7\u05D9\u05E7\u05D4")
                    .setSubtitle(what);
            if (Build.VERSION.SDK_INT >= 30) {
                b.setAllowedAuthenticators(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            } else if (Build.VERSION.SDK_INT >= 29) {
                b.setDeviceCredentialAllowed(true);
            } else {
                b.setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", a.getMainExecutor(), (d, w) -> { });
            }
            b.build().authenticate(new CancellationSignal(), a.getMainExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            action.run();
                        }

                        @Override
                        public void onAuthenticationError(int code, CharSequence msg) {
                            if (code == BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT
                                    || code == BiometricPrompt.BIOMETRIC_ERROR_HW_UNAVAILABLE
                                    || code == BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS
                                    || code == BiometricPrompt.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL) {
                                // No fingerprint / lock set up on this phone: the confirm dialog is enough.
                                action.run();
                            } else if (code != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
                                    && code != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                                Toast.makeText(a, "\u05D4\u05D0\u05D9\u05DE\u05D5\u05EA \u05E0\u05DB\u05E9\u05DC - \u05DC\u05D0 \u05E0\u05DE\u05D7\u05E7", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } catch (Exception e) {
            action.run();
        }
    }
}
