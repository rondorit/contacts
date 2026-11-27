package com.clientcaller.app;

import android.content.Context;
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

/** Builds a right-to-left A4 PDF of a price quote. */
final class QuotePdf {
    private static final int W = 595, H = 842, M = 36;
    private static final int BLUE = 0xFF1565C0, DARK = 0xFF0D47A1, LIGHT = 0xFFE3EDFB, GRAY = 0xFF6B7280, TEXT = 0xFF1F2937;

    private PdfDocument doc;
    private PdfDocument.Page page;
    private Canvas cv;
    private int pageNo = 0;
    private float y;

    private QuotePdf() { }

    static File build(Context c, QuoteDb.Quote q) throws IOException {
        return new QuotePdf().make(c, q);
    }

    private File make(Context c, QuoteDb.Quote q) throws IOException {
        doc = new PdfDocument();
        newPage();

        // Header band
        Paint band = new Paint();
        band.setColor(BLUE);
        cv.drawRect(0, 0, W, 92, band);
        text("\u05D4\u05E6\u05E2\u05EA \u05DE\u05D7\u05D9\u05E8", M, 22, W - 2 * M, 26, true, Color.WHITE, Layout.Alignment.ALIGN_NORMAL);
        text("\u05DE\u05E1' " + q.id + (q.title.isEmpty() ? "" : "  \u2022  " + q.title), M, 58, W - 2 * M, 13, false, 0xDDFFFFFF,
                Layout.Alignment.ALIGN_NORMAL);
        y = 108;

        // Issuer (right) and recipient (left) blocks
        String iss = Issuer.block(Issuer.forQuote(c, q));
        float colW = (W - 2 * M - 20) / 2f;
        float h1 = text("\u05DE\u05D0\u05EA:\n" + iss, W - M - colW, y, colW, 11, false, TEXT, Layout.Alignment.ALIGN_NORMAL);
        StringBuilder to = new StringBuilder("\u05DC\u05DB\u05D1\u05D5\u05D3:\n").append(q.clientName.isEmpty() ? "-" : q.clientName);
        if (!q.clientPhone.isEmpty()) to.append("\n\u05D8\u05DC': ").append(q.clientPhone);
        if (!q.clientEmail.isEmpty()) to.append("\n").append(q.clientEmail);
        float h2 = text(to.toString(), M, y, colW, 11, false, TEXT, Layout.Alignment.ALIGN_NORMAL);
        y += Math.max(h1, h2) + 8;
        long until = q.created + q.validDays * 24L * 3600 * 1000;
        text("\u05EA\u05D0\u05E8\u05D9\u05DA: " + TicketDb.fmt(q.created).substring(0, 10) + "     \u05D1\u05EA\u05D5\u05E7\u05E3 \u05E2\u05D3: " + TicketDb.fmt(until).substring(0, 10),
                M, y, W - 2 * M, 11, false, GRAY, Layout.Alignment.ALIGN_NORMAL);
        y += 24;

        // Table: columns from right: # | description | qty | unit price | total
        float[] cw = {28, 255, 50, 95, 95};   // widths, right to left
        float tableW = 0;
        for (float w : cw) tableW += w;
        String[] head = {"#", "\u05EA\u05D9\u05D0\u05D5\u05E8", "\u05DB\u05DE\u05D5\u05EA", "\u05DE\u05D7\u05D9\u05E8 \u05DC\u05D9\u05D7'", "\u05E1\u05D4\"\u05DB"};
        rowBox(cw, head, true, tableW);
        int n = 1;
        for (QuoteDb.Line l : q.lines) {
            String[] cells = {String.valueOf(n++), l.name, QuoteDb.qty(l.qty), QuoteDb.money(l.price), QuoteDb.money(l.total())};
            rowBox(cw, cells, false, tableW);
        }
        y += 10;

        // Totals (left side)
        float tw = 230;
        float tx = M;
        ensure(110);
        totalLine("\u05E1\u05DB\u05D5\u05DD \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE", QuoteDb.money(q.subtotal()), tx, tw, false);
        if (q.discountPct > 0) totalLine("\u05D4\u05E0\u05D7\u05D4 " + QuoteDb.qty(q.discountPct) + "%", "-" + QuoteDb.money(q.discount()), tx, tw, false);
        totalLine("\u05DE\u05E2\"\u05DE 18%", QuoteDb.money(q.vat()), tx, tw, false);
        totalLine("\u05E1\u05D4\"\u05DB \u05DC\u05EA\u05E9\u05DC\u05D5\u05DD", QuoteDb.money(q.total()), tx, tw, true);
        y += 16;

        if (!q.notes.isEmpty()) {
            ensure(60);
            y += text("\u05D4\u05E2\u05E8\u05D5\u05EA:\n" + q.notes, M, y, W - 2 * M, 11, false, TEXT, Layout.Alignment.ALIGN_NORMAL) + 12;
        }
        ensure(40);
        text("\u05D4\u05D4\u05E6\u05E2\u05D4 \u05D1\u05EA\u05D5\u05E7\u05E3 \u05DC-" + q.validDays + " \u05D9\u05D5\u05DD. \u05D4\u05DE\u05D7\u05D9\u05E8\u05D9\u05DD \u05D1\u05E9\u05E7\u05DC\u05D9\u05DD. \u05DC\u05D0\u05D9\u05E9\u05D5\u05E8 \u05D4\u05D4\u05E6\u05E2\u05D4 \u05D0\u05E4\u05E9\u05E8 \u05DC\u05D4\u05E9\u05D9\u05D1 \"\u05DE\u05D0\u05E9\u05E8/\u05EA\" \u05D1\u05D5\u05D5\u05D0\u05D8\u05E1\u05D0\u05E4 \u05D0\u05D5 \u05D1\u05DE\u05D9\u05D9\u05DC.", M, y, W - 2 * M, 10, false, GRAY,
                Layout.Alignment.ALIGN_NORMAL);

        doc.finishPage(page);
        File dir = new File(c.getCacheDir(), "share");
        dir.mkdirs();
        File f = new File(dir, "quote-" + q.id + ".pdf");
        try (FileOutputStream o = new FileOutputStream(f)) {
            doc.writeTo(o);
        }
        doc.close();
        return f;
    }

    private void newPage() {
        if (page != null) doc.finishPage(page);
        pageNo++;
        page = doc.startPage(new PdfDocument.PageInfo.Builder(W, H, pageNo).create());
        cv = page.getCanvas();
        y = M;
    }

    private void ensure(float need) {
        if (y + need > H - M) newPage();
    }

    private void rowBox(float[] cw, String[] cells, boolean header, float tableW) {
        // measure height
        float hMax = 0;
        for (int i = 0; i < cells.length; i++) {
            hMax = Math.max(hMax, layout(cells[i], cw[i] - 10, 10.5f, header, TEXT,
                    i == 1 ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_CENTER).getHeight());
        }
        float rowH = hMax + 12;
        ensure(rowH);
        float right = W - M;
        Paint bg = new Paint();
        bg.setColor(header ? DARK : Color.WHITE);
        cv.drawRect(right - tableW, y, right, y + rowH, bg);
        Paint line = new Paint();
        line.setColor(0xFFD1D5DB);
        line.setStrokeWidth(0.7f);
        cv.drawLine(right - tableW, y + rowH, right, y + rowH, line);
        float x = right;
        for (int i = 0; i < cells.length; i++) {
            x -= cw[i];
            text(cells[i], x + 5, y + 6, cw[i] - 10, 10.5f, header, header ? Color.WHITE : TEXT,
                    i == 1 ? Layout.Alignment.ALIGN_NORMAL : Layout.Alignment.ALIGN_CENTER);
        }
        y += rowH;
    }

    private void totalLine(String label, String value, float x, float w, boolean big) {
        float h = big ? 28 : 20;
        if (big) {
            Paint p = new Paint();
            p.setColor(LIGHT);
            cv.drawRoundRect(new RectF(x, y, x + w, y + h), 6, 6, p);
        }
        float size = big ? 13 : 11;
        text(label, x + w / 2, y + (big ? 7 : 3), w / 2 - 8, size, big, big ? DARK : TEXT, Layout.Alignment.ALIGN_NORMAL);
        text(value, x + 8, y + (big ? 7 : 3), w / 2 - 8, size, big, big ? DARK : TEXT, Layout.Alignment.ALIGN_OPPOSITE);
        y += h + 2;
    }

    private StaticLayout layout(String s, float width, float size, boolean bold, int color, Layout.Alignment align) {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTextSize(size);
        tp.setColor(color);
        if (bold) tp.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return StaticLayout.Builder.obtain(s, 0, s.length(), tp, (int) Math.max(10, width))
                .setAlignment(align)
                .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
                .build();
    }

    /** Draws text at (x, top y) within width; returns its height. */
    private float text(String s, float x, float top, float width, float size, boolean bold, int color, Layout.Alignment align) {
        StaticLayout l = layout(s, width, size, bold, color, align);
        cv.save();
        cv.translate(x, top);
        l.draw(cv);
        cv.restore();
        return l.getHeight();
    }
}
