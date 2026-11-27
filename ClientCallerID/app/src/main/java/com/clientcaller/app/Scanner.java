package com.clientcaller.app;

import android.app.Activity;
import android.widget.Toast;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

/** Barcode / QR scanning with Google's code scanner (camera UI is provided by Google Play services). */
final class Scanner {
    interface Result { void on(String code); }

    private Scanner() { }

    static void scan(final Activity a, final Result cb) {
        try {
            GmsBarcodeScannerOptions o = new GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .enableAutoZoom()
                    .build();
            GmsBarcodeScanner s = GmsBarcodeScanning.getClient(a, o);
            s.startScan()
                    .addOnSuccessListener(b -> {
                        String v = b.getRawValue();
                        if (v != null) cb.on(v.trim());
                    })
                    .addOnFailureListener(e -> Toast.makeText(a, "הסריקה נכשלה: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } catch (Throwable e) {
            Toast.makeText(a, "סריקה לא זמינה: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
