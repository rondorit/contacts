package com.clientcaller.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** Work report PDF: ticket details, work & materials, photos and the client's signature. */
final class WorkReport {
    private static final int W = 595, H = 842, M = 36;
    private static final int BLUE = 0xFF1565C0, LIGHT = 0xFFE3EDFB, GRAY = 0xFF6B7280, TEXT = 0xFF1F2937;
    private PdfDocument doc;
    private PdfDocument.Page page;
    private Canvas cv;
    private float y;
    private int pageNo;

    static File build(Context c, TicketDb.Ticket t) throws IOException {
        return new WorkReport().make(c, t);
    }

    private File make(Context c, TicketDb.Ticket t) throws IOException {
        doc = new PdfDocument();
        newPage();
        Paint band = new Paint();
        band.setColor(BLUE);
        cv.drawRect(0, 0, W, 80, band);
        text("\u05D3\u05D5\u05D7 \u05E2\u05D1\u05D5\u05D3\u05D4", M, 18, W - 2 * M, 24, true, Color.WHITE);
        text("\u05E7\u05E8\u05D9\u05D0\u05D4 #" + t.id + "  \u2022  " + TicketDb.fmt(System.currentTimeMillis()).substring(0, 10), M, 52, W - 2 * M, 12, false, 0xDDFFFFFF);
        y = 96;

        Issuer.Profile iss = Issuer.byId(c, "");
        float colW = (W - 2 * M - 20) / 2f;
        float h1 = text("\u05DE\u05D1\u05E6\u05E2:\n" + Issuer.block(iss), W - M - colW, y, colW, 11, false, TEXT);
        StringBuilder to = new StringBuilder("\u05DC\u05E7\u05D5\u05D7:\n").append(t.clientName.isEmpty() ? "-" : t.clientName);
        if (!t.clientPhone.isEmpty()) to.append("\n\u05D8\u05DC': ").append(t.clientPhone);
        float h2 = text(to.toString(), M, y, colW, 11, false, TEXT);
        y += Math.max(h1, h2) + 14;

        section("\u05E4\u05E8\u05D8\u05D9 \u05D4\u05E7\u05E8\u05D9\u05D0\u05D4");
        para("\u05E0\u05D5\u05E9\u05D0: " + t.title);
        if (!t.description.isEmpty()) para("\u05EA\u05D9\u05D0\u05D5\u05E8: " + t.description);
        if (!t.equipment.isEmpty()) para("\u05E6\u05D9\u05D5\u05D3: " + t.equipment + (t.mac.isEmpty() ? "" : "   MAC: " + t.mac));
        para("\u05E0\u05E4\u05EA\u05D7\u05D4: " + TicketDb.fmt(t.created) + "   \u2022   \u05E1\u05D8\u05D8\u05D5\u05E1: " + He.status(t.status));

        List<TicketDb.Item> items = TicketDb.get(c).items(t.id);
        if (!items.isEmpty()) {
            section("\u05E2\u05D1\u05D5\u05D3\u05D4 \u05D5\u05D7\u05D5\u05DE\u05E8\u05D9\u05DD");
            double sum = 0;
            for (TicketDb.Item it : items) {
                need(18);
                String left = QuoteDb.money(it.total());
                String right = ("time".equals(it.kind) ? "\u23F1\uFE0F " : "\uD83D\uDD29 ") + it.name + "  \u00D7" + QuoteDb.qty(it.qty)
                        + (it.price > 0 ? "  @ " + QuoteDb.money(it.price) : "");
                text(right, W / 2f - 20, y, W / 2f - M + 20, 11, false, TEXT);
                textAlign(left, M, y, W / 2f - M - 30, 11, false, TEXT, Layout.Alignment.ALIGN_OPPOSITE);
                y += 17;
                sum += it.total();
            }
            need(22);
            Paint bg = new Paint();
            bg.setColor(LIGHT);
            cv.drawRect(M, y, W - M, y + 20, bg);
            text("\u05E1\u05D4\"\u05DB (\u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE)", W / 2f, y + 3, W / 2f - M - 6, 12, true, TEXT);
            textAlign(QuoteDb.money(sum), M + 6, y + 3, W / 2f - M, 12, true, TEXT, Layout.Alignment.ALIGN_OPPOSITE);
            y += 28;
        }

        List<TicketDb.Note> notes = TicketDb.get(c).notes(t.id);
        if (!notes.isEmpty()) {
            section("\u05DE\u05D4 \u05D1\u05D5\u05E6\u05E2");
            int shown = 0;
            for (int k = notes.size() - 1; k >= 0 && shown < 12; k--, shown++) {
                TicketDb.Note n = notes.get(k);
                if (n.text.startsWith("\u05E1\u05D8\u05D8\u05D5\u05E1:") || n.text.startsWith("\uD83D\uDCF7")) continue;
                para(TicketDb.fmt(n.time) + "  " + n.text);
            }
        }

        List<File> photos = Media.photos(c, t.id);
        if (!photos.isEmpty()) {
            section("\u05EA\u05DE\u05D5\u05E0\u05D5\u05EA");
            float pw = (W - 2 * M - 20) / 3f, ph = pw * 0.75f;
            int col = 0;
            for (File f : photos) {
                if (col == 0) need(ph + 8);
                Bitmap b = Media.thumb(f, 600);
                if (b == null) continue;
                float x = W - M - (col + 1) * pw - col * 10;
                drawFit(b, new RectF(x, y, x + pw, y + ph));
                b.recycle();
                col++;
                if (col == 3) { col = 0; y += ph + 8; }
            }
            if (col != 0) y += ph + 8;
        }

        File sig = Media.signature(c, t.id);
        if (sig.exists()) {
            section("\u05D0\u05D9\u05E9\u05D5\u05E8 \u05D4\u05DC\u05E7\u05D5\u05D7");
            need(120);
            String signer = CallLogger.prefs(c).getString("signer_t" + t.id, t.clientName);
            long when = CallLogger.prefs(c).getLong("signed_t" + t.id, sig.lastModified());
            para("\u05D4\u05E2\u05D1\u05D5\u05D3\u05D4 \u05D1\u05D5\u05E6\u05E2\u05D4 \u05DC\u05E9\u05D1\u05D9\u05E2\u05D5\u05EA \u05E8\u05E6\u05D5\u05E0\u05D9.  " + (signer.isEmpty() ? "" : signer + "  \u2022  ") + TicketDb.fmt(when));
            Bitmap b = android.graphics.BitmapFactory.decodeFile(sig.getPath());
            if (b != null) {
                drawFit(b, new RectF(W - M - 220, y, W - M, y + 90));
                Paint line = new Paint();
                line.setColor(GRAY);
                cv.drawLine(W - M - 220, y + 92, W - M, y + 92, line);
                b.recycle();
                y += 100;
            }
        }

        doc.finishPage(page);
        File dir = new File(c.getCacheDir(), "share");
        dir.mkdirs();
        File out = new File(dir, "work-report-" + t.id + ".pdf");
        try (FileOutputStream fo = new FileOutputStream(out)) {
            doc.writeTo(fo);
        }
        doc.close();
        return out;
    }

    private void newPage() {
        if (page != null) doc.finishPage(page);
        page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, ++pageNo).create());
        cv = page.getCanvas();
        y = M;
    }

    private void need(float h) {
        if (y + h > H - M) newPage();
    }

    private void section(String title) {
        need(40);
        y += 6;
        text(title, M, y, W - 2 * M, 14, true, BLUE);
        y += 20;
        Paint p = new Paint();
        p.setColor(LIGHT);
        cv.drawRect(M, y, W - M, y + 1.5f, p);
        y += 8;
    }

    private void para(String s) {
        StaticLayout l = layout(s, W - 2 * M, 11, false, TEXT, Layout.Alignment.ALIGN_NORMAL);
        need(l.getHeight() + 4);
        draw(l, M, y);
        y += l.getHeight() + 4;
    }

    private void drawFit(Bitmap b, RectF box) {
        float s = Math.min(box.width() / b.getWidth(), box.height() / b.getHeight());
        float w = b.getWidth() * s, h = b.getHeight() * s;
        float x = box.right - w, top = box.top + (box.height() - h) / 2f;
        cv.drawBitmap(b, null, new RectF(x, top, x + w, top + h), new Paint(Paint.FILTER_BITMAP_FLAG));
    }

    private float text(String s, float x, float top, float width, float size, boolean bold, int color) {
        return textAlign(s, x, top, width, size, bold, color, Layout.Alignment.ALIGN_NORMAL);
    }

    private float textAlign(String s, float x, float top, float width, float size, boolean bold, int color, Layout.Alignment a) {
        StaticLayout l = layout(s, width, size, bold, color, a);
        draw(l, x, top);
        return l.getHeight();
    }

    private void draw(StaticLayout l, float x, float top) {
        cv.save();
        cv.translate(x, top);
        l.draw(cv);
        cv.restore();
    }

    private static StaticLayout layout(String s, float width, float size, boolean bold, int color, Layout.Alignment align) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(size);
        tp.setColor(color);
        if (bold) tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return StaticLayout.Builder.obtain(s, 0, s.length(), tp, (int) Math.max(10, width))
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .build();
    }
}
