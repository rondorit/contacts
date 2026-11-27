package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Ticket photos (camera / gallery) and the client's signature. Files live in filesDir/photos. */
final class Media {
    static final int REQ_CAMERA = 81, REQ_GALLERY = 82;
    private static final String KEY_PENDING = "cam_pending";
    private static final int MAX = 1600;

    private Media() { }

    static File dir(Context c) {
        File d = new File(c.getFilesDir(), "photos");
        d.mkdirs();
        return d;
    }

    /** Photos of a ticket, oldest first. */
    static List<File> photos(Context c, long ticketId) {
        List<File> out = new ArrayList<>();
        File[] all = dir(c).listFiles();
        if (all == null) return out;
        String pre = "t" + ticketId + "_";
        for (File f : all) if (f.getName().startsWith(pre)) out.add(f);
        java.util.Collections.sort(out, (a, b) -> a.getName().compareTo(b.getName()));
        return out;
    }

    static File signature(Context c, long ticketId) {
        return new File(dir(c), "sig_t" + ticketId + ".png");
    }

    static void takePhoto(Activity a, long ticketId) {
        File share = new File(a.getCacheDir(), "share");
        share.mkdirs();
        File target = new File(share, "cam_" + System.currentTimeMillis() + ".jpg");
        CallLogger.prefs(a).edit().putString(KEY_PENDING, ticketId + "|" + target.getName()).apply();
        Uri u = FilesProvider.uriFor(target);
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, u)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.setClipData(ClipData.newRawUri("photo", u));
        try {
            a.startActivityForResult(i, REQ_CAMERA);
        } catch (Exception e) {
            Toast.makeText(a, "\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05D0\u05E4\u05DC\u05D9\u05E7\u05E6\u05D9\u05D9\u05EA \u05DE\u05E6\u05DC\u05DE\u05D4", Toast.LENGTH_SHORT).show();
        }
    }

    static void pickPhotos(Activity a, long ticketId) {
        CallLogger.prefs(a).edit().putString(KEY_PENDING, ticketId + "|").apply();
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        a.startActivityForResult(Intent.createChooser(i, "\u05D1\u05D7\u05E8 \u05EA\u05DE\u05D5\u05E0\u05D5\u05EA"), REQ_GALLERY);
    }

    /** Returns true if this result was a photo (the screen should refresh its photos). */
    static boolean onResult(Activity a, int req, int res, Intent data) {
        if (req != REQ_CAMERA && req != REQ_GALLERY) return false;
        String pending = CallLogger.prefs(a).getString(KEY_PENDING, "");
        CallLogger.prefs(a).edit().remove(KEY_PENDING).apply();
        if (res != Activity.RESULT_OK || pending.isEmpty()) return true;
        long tid;
        try {
            tid = Long.parseLong(pending.substring(0, pending.indexOf('|')));
        } catch (Exception e) {
            return true;
        }
        int added = 0;
        try {
            if (req == REQ_CAMERA) {
                File cam = new File(new File(a.getCacheDir(), "share"), pending.substring(pending.indexOf('|') + 1));
                if (cam.length() > 0 && store(a, Uri.fromFile(cam), cam, tid)) added++;
                cam.delete();
            } else if (data != null) {
                List<Uri> uris = new ArrayList<>();
                if (data.getClipData() != null) {
                    for (int k = 0; k < data.getClipData().getItemCount(); k++) uris.add(data.getClipData().getItemAt(k).getUri());
                } else if (data.getData() != null) {
                    uris.add(data.getData());
                }
                for (Uri u : uris) if (store(a, u, null, tid)) added++;
            }
        } catch (Exception e) {
            Toast.makeText(a, "\u05E9\u05DE\u05D9\u05E8\u05EA \u05D4\u05EA\u05DE\u05D5\u05E0\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (added > 0) TicketDb.get(a).addNote(tid, "\uD83D\uDCF7 \u05E0\u05D5\u05E1\u05E4\u05D5 " + added + " \u05EA\u05DE\u05D5\u05E0\u05D5\u05EA");
        return true;
    }

    /** Scales the picture down to MAX px, fixes the rotation and saves it as JPEG. */
    private static boolean store(Context c, Uri u, File raw, long tid) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = open(c, u, raw)) { BitmapFactory.decodeStream(in, null, o); }
        if (o.outWidth <= 0) return false;
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= MAX) sample *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        Bitmap b;
        try (InputStream in = open(c, u, raw)) { b = BitmapFactory.decodeStream(in, null, o2); }
        if (b == null) return false;
        int rot = 0;
        try (InputStream in = open(c, u, raw)) {
            int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            rot = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90 : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                    : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
        } catch (Exception ignored) { }
        float scale = Math.min(1f, MAX / (float) Math.max(b.getWidth(), b.getHeight()));
        if (scale < 1f || rot != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rot);
            Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (r != b) b.recycle();
            b = r;
        }
        File out = new File(dir(c), "t" + tid + "_" + System.currentTimeMillis() + ".jpg");
        try (OutputStream os = new FileOutputStream(out)) { b.compress(Bitmap.CompressFormat.JPEG, 82, os); }
        b.recycle();
        return true;
    }

    private static InputStream open(Context c, Uri u, File raw) throws Exception {
        return raw != null ? new java.io.FileInputStream(raw) : c.getContentResolver().openInputStream(u);
    }

    static Bitmap thumb(File f, int px) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= px) sample *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), o2);
    }

    /** Opens a photo in the phone's viewer. */
    static void view(Activity a, File f) {
        try {
            File share = new File(a.getCacheDir(), "share");
            share.mkdirs();
            File copy = new File(share, f.getName());
            try (InputStream in = new java.io.FileInputStream(f); OutputStream out = new FileOutputStream(copy)) {
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            a.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(FilesProvider.uriFor(copy),
                    f.getName().endsWith(".png") ? "image/png" : "image/jpeg").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) {
            Toast.makeText(a, "\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E4\u05EA\u05D5\u05D7: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------------ signature

    /** Finger-drawn signature on a white pad. */
    static final class SignatureView extends View {
        private final Path path = new Path();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean empty = true;

        SignatureView(Context c) {
            super(c);
            paint.setColor(0xFF0D1B3E);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4 * c.getResources().getDisplayMetrics().density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            setBackgroundColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas cv) {
            super.onDraw(cv);
            cv.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            getParent().requestDisallowInterceptTouchEvent(true);
            float x = e.getX(), y = e.getY();
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                path.moveTo(x, y);
                path.lineTo(x + 0.1f, y);
                empty = false;
            } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                for (int h = 0; h < e.getHistorySize(); h++) path.lineTo(e.getHistoricalX(h), e.getHistoricalY(h));
                path.lineTo(x, y);
            }
            invalidate();
            return true;
        }

        void clear() {
            path.reset();
            empty = true;
            invalidate();
        }

        boolean isEmpty() {
            return empty;
        }

        Bitmap bitmap() {
            Bitmap b = Bitmap.createBitmap(Math.max(1, getWidth()), Math.max(1, getHeight()), Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            cv.drawColor(Color.WHITE);
            cv.drawPath(path, paint);
            return b;
        }
    }

    interface Signed { void on(String signer); }

    /** Shows the signing pad; saves sig_t&lt;id&gt;.png and returns the signer's name. */
    static void sign(final Activity a, final TicketDb.Ticket t, final Signed cb) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(a, 12);
        box.setPadding(p, p, p, 0);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final EditText name = new EditText(a);
        name.setHint("\u05E9\u05DD \u05D4\u05D7\u05D5\u05EA\u05DD");
        name.setText(t.clientName);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        box.addView(name);
        box.addView(Ui.text(a, "\u05D7\u05EA\u05D5\u05DD \u05DB\u05D0\u05DF \u05D1\u05D0\u05E6\u05D1\u05E2:", 13, Ui.MUTED, false));
        final SignatureView pad = new SignatureView(a);
        box.addView(pad, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 220)));
        Button clear = new Button(a);
        clear.setText("\uD83E\uDDFD \u05E0\u05E7\u05D4");
        clear.setAllCaps(false);
        clear.setOnClickListener(v -> pad.clear());
        box.addView(clear);
        final AlertDialog d = new AlertDialog.Builder(a)
                .setTitle("\u270D\uFE0F \u05D7\u05EA\u05D9\u05DE\u05EA \u05DC\u05E7\u05D5\u05D7 \u2013 \u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id)
                .setView(box)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8 \u05D7\u05EA\u05D9\u05DE\u05D4", null)
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .create();
        d.setOnShowListener(x -> d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (pad.isEmpty()) {
                Toast.makeText(a, "\u05D7\u05E1\u05E8\u05D4 \u05D7\u05EA\u05D9\u05DE\u05D4", Toast.LENGTH_SHORT).show();
                return;
            }
            try (OutputStream os = new FileOutputStream(signature(a, t.id))) {
                pad.bitmap().compress(Bitmap.CompressFormat.PNG, 100, os);
            } catch (Exception e) {
                Toast.makeText(a, "\u05E9\u05DE\u05D9\u05E8\u05EA \u05D4\u05D7\u05EA\u05D9\u05DE\u05D4 \u05E0\u05DB\u05E9\u05DC\u05D4: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            String signer = name.getText().toString().trim();
            CallLogger.prefs(a).edit().putString("signer_t" + t.id, signer)
                    .putLong("signed_t" + t.id, System.currentTimeMillis()).apply();
            TicketDb.get(a).addNote(t.id, "\u270D\uFE0F \u05E0\u05D7\u05EA\u05DD \u05E2\"\u05D9 " + (signer.isEmpty() ? "\u05D4\u05DC\u05E7\u05D5\u05D7" : signer));
            d.dismiss();
            cb.on(signer);
        }));
        d.show();
    }
}
