package com.clientcaller.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The price list: items/services with a price before VAT. Add, edit, delete, import from Excel. */
public class CatalogActivity extends Activity {
    private static final int REQ_IMPORT = 1;
    private LinearLayout list;
    private EditText search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF");
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 60));
        page.addView(Theme.header(this, "\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF"));

        List<View> tools = new ArrayList<>();
        tools.add(Ui.tile(this, "\u2795", "\u05E4\u05E8\u05D9\u05D8 \u05D7\u05D3\u05E9", "", 0, v -> edit(new QuoteDb.Item())));
        tools.add(Ui.tile(this, "\uD83D\uDCE5", "\u05D9\u05D9\u05D1\u05D5\u05D0 \u05DE\u05D0\u05E7\u05E1\u05DC", "\u05E9\u05DD + \u05DE\u05D7\u05D9\u05E8", Ui.MUTED, v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            startActivityForResult(i, REQ_IMPORT);
        }));
        Ui.grid(this, page, tools, 2);
        page.addView(Ui.text(this, "  \u05D4\u05DE\u05D7\u05D9\u05E8\u05D9\u05DD \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE. \u05DE\u05E2\"\u05DE 18% \u05DE\u05EA\u05D5\u05D5\u05E1\u05E3 \u05D1\u05D4\u05E6\u05E2\u05D4.", 12, Ui.MUTED, false));

        search = new EditText(this);
        search.setHint("\uD83D\uDD0D  \u05D7\u05D9\u05E4\u05D5\u05E9 \u05D1\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF\u2026");
        search.setSingleLine(true);
        search.setBackground(Theme.fieldBg(this));
        search.setTextColor(Theme.TEXT_PRIMARY);
        search.setHintTextColor(Theme.TEXT_TERTIARY);
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { refresh(); }
        });
        page.addView(search, Ui.cardParams(this));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        page.addView(list);

        ScrollView sv = new ScrollView(this);
        sv.addView(page);
        setContentView(sv);
        He.rtl(this);
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        String f = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int n = 0;
        for (final QuoteDb.Item it : QuoteDb.get(this).catalog()) {
            if (!f.isEmpty() && !(it.name + " " + it.category).toLowerCase(Locale.ROOT).contains(f)) continue;
            n++;
            String sub = QuoteDb.money(it.price) + (it.unit.isEmpty() ? "" : " / " + it.unit)
                    + (it.category.isEmpty() ? "" : "  \u2022  " + it.category);
            list.addView(Ui.rowCard(this, it.name, sub, "", 0, v -> edit(it)), Ui.cardParams(this));
        }
        if (n == 0) list.addView(Ui.text(this, "   \u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF \u05E8\u05D9\u05E7 - \u05D4\u05D5\u05E1\u05E3 \u05E4\u05E8\u05D9\u05D8 \u05D0\u05D5 \u05D9\u05D9\u05D1\u05D0 \u05DE\u05D0\u05E7\u05E1\u05DC", 14, Ui.MUTED, false));
    }

    private void edit(final QuoteDb.Item it) {
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        lay.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        final EditText name = field(lay, "\u05E9\u05DD \u05D4\u05E4\u05E8\u05D9\u05D8 / \u05D4\u05E9\u05D9\u05E8\u05D5\u05EA", it.name, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        final EditText price = field(lay, "\u05DE\u05D7\u05D9\u05E8 \u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE (\u20AA)", it.price == 0 ? "" : QuoteDb.qty(it.price),
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText unit = field(lay, "\u05D9\u05D7\u05D9\u05D3\u05D4 (\u05E9\u05E2\u05D4, \u05D9\u05D7', \u05D7\u05D5\u05D3\u05E9\u2026)", it.unit, InputType.TYPE_CLASS_TEXT);
        final EditText cat = field(lay, "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4", it.category, InputType.TYPE_CLASS_TEXT);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(it.id == 0 ? "\u05E4\u05E8\u05D9\u05D8 \u05D7\u05D3\u05E9" : "\u05E2\u05E8\u05D9\u05DB\u05EA \u05E4\u05E8\u05D9\u05D8")
                .setView(lay)
                .setPositiveButton("\u05E9\u05DE\u05D5\u05E8", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) {
                        Toast.makeText(this, "\u05D7\u05E1\u05E8 \u05E9\u05DD", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    it.name = n;
                    it.price = QuoteActivity.parse(price.getText().toString(), 0);
                    it.unit = unit.getText().toString().trim();
                    it.category = cat.getText().toString().trim();
                    QuoteDb.get(this).saveItem(it);
                    refresh();
                })
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null);
        if (it.id > 0) b.setNeutralButton("\u05DE\u05D7\u05E7", (d, w) -> Auth.confirm(this, "\u05DE\u05D7\u05D9\u05E7\u05EA " + it.name, () -> {
            QuoteDb.get(this).deleteItem(it.id);
            refresh();
        }));
        b.show();
    }

    private EditText field(LinearLayout lay, String label, String value, int type) {
        lay.addView(Ui.text(this, label, 12, Ui.MUTED, false));
        EditText e = new EditText(this);
        e.setInputType(type);
        e.setText(value);
        lay.addView(e);
        return e;
    }

    // ------------------------------------------------------------------ Excel import

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = getContentResolver().openInputStream(data.getData())) {
            if (in == null) throw new java.io.IOException("\u05DC\u05D0 \u05E0\u05D9\u05EA\u05DF \u05DC\u05E4\u05EA\u05D5\u05D7");
            List<List<String>> table = ClientStore.readTable(ClientStore.readAll(in));
            int start = 0;
            while (start < table.size() && rowBlank(table.get(start))) start++;
            if (start >= table.size()) throw new java.io.IOException("\u05D4\u05E7\u05D5\u05D1\u05E5 \u05E8\u05D9\u05E7");
            List<String> headers = new ArrayList<>(table.get(start));
            final List<List<String>> rows = new ArrayList<>(table.subList(start + 1, table.size()));
            int nameCol = find(headers, "\u05E9\u05DD", "\u05E4\u05E8\u05D9\u05D8", "\u05EA\u05D9\u05D0\u05D5\u05E8", "\u05E9\u05D9\u05E8\u05D5\u05EA", "name", "item", "description");
            int priceCol = find(headers, "\u05DE\u05D7\u05D9\u05E8", "\u05E2\u05DC\u05D5\u05EA", "price", "cost");
            final int unitCol = find(headers, "\u05D9\u05D7\u05D9\u05D3\u05D4", "unit");
            final int catCol = find(headers, "\u05E7\u05D8\u05D2\u05D5\u05E8\u05D9\u05D4", "\u05E1\u05D5\u05D2", "category");
            final String[] hs = headers.toArray(new String[0]);
            if (nameCol < 0 || priceCol < 0 || nameCol == priceCol) {
                chooseCol(hs, "\u05D1\u05D0\u05D9\u05D6\u05D5 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05E9\u05DD \u05D4\u05E4\u05E8\u05D9\u05D8?", nc -> chooseCol(hs, "\u05D1\u05D0\u05D9\u05D6\u05D5 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05D4\u05DE\u05D7\u05D9\u05E8 (\u05DC\u05E4\u05E0\u05D9 \u05DE\u05E2\"\u05DE)?",
                        pc -> askMode(rows, nc, pc, unitCol, catCol)));
            } else {
                askMode(rows, nameCol, priceCol, unitCol, catCol);
            }
        } catch (Exception e) {
            Toast.makeText(this, "\u05E9\u05D2\u05D9\u05D0\u05D4 \u05D1\u05E7\u05E8\u05D9\u05D0\u05EA \u05D4\u05E7\u05D5\u05D1\u05E5: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private interface IntCb {
        void on(int v);
    }

    private void chooseCol(String[] headers, String title, IntCb cb) {
        new AlertDialog.Builder(this).setTitle(title).setItems(headers, (d, w) -> cb.on(w))
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null).show();
    }

    private static int find(List<String> headers, String... words) {
        for (String w : words) {
            for (int i = 0; i < headers.size(); i++) {
                if (headers.get(i).toLowerCase(Locale.ROOT).contains(w)) return i;
            }
        }
        return -1;
    }

    private static boolean rowBlank(List<String> r) {
        for (String v : r) if (v != null && !v.trim().isEmpty()) return false;
        return true;
    }

    private static String cell(List<String> r, int c) {
        return c >= 0 && c < r.size() && r.get(c) != null ? r.get(c).trim() : "";
    }

    private void askMode(final List<List<String>> rows, final int nameCol, final int priceCol, final int unitCol, final int catCol) {
        int count = 0;
        for (List<String> r : rows) if (!cell(r, nameCol).isEmpty()) count++;
        final int total = count;
        new AlertDialog.Builder(this)
                .setTitle("\u05D9\u05D9\u05D1\u05D5\u05D0 " + total + " \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD")
                .setMessage("\u05DC\u05D4\u05D7\u05DC\u05D9\u05E3 \u05D0\u05EA \u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF \u05D4\u05E7\u05D9\u05D9\u05DD, \u05D0\u05D5 \u05DC\u05D4\u05D5\u05E1\u05D9\u05E3 \u05D0\u05DC\u05D9\u05D5?")
                .setPositiveButton("\u05DC\u05D4\u05D7\u05DC\u05D9\u05E3", (d, w) -> Auth.confirm(this, "\u05D4\u05D7\u05DC\u05E4\u05EA \u05D4\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", () ->
                        doImport(rows, nameCol, priceCol, unitCol, catCol, true)))
                .setNeutralButton("\u05DC\u05D4\u05D5\u05E1\u05D9\u05E3", (d, w) -> doImport(rows, nameCol, priceCol, unitCol, catCol, false))
                .setNegativeButton("\u05D1\u05D9\u05D8\u05D5\u05DC", null)
                .show();
    }

    private void doImport(List<List<String>> rows, int nameCol, int priceCol, int unitCol, int catCol, boolean replace) {
        QuoteDb db = QuoteDb.get(this);
        if (replace) db.clearCatalog();
        int n = 0;
        for (List<String> r : rows) {
            String name = cell(r, nameCol);
            if (name.isEmpty()) continue;
            QuoteDb.Item it = new QuoteDb.Item();
            it.name = name;
            it.price = QuoteActivity.parse(cell(r, priceCol), 0);
            it.unit = cell(r, unitCol);
            it.category = cell(r, catCol);
            db.saveItem(it);
            n++;
        }
        Toast.makeText(this, "\u05D9\u05D5\u05D1\u05D0\u05D5 " + n + " \u05E4\u05E8\u05D9\u05D8\u05D9\u05DD \u05DC\u05DE\u05D7\u05D9\u05E8\u05D5\u05DF", Toast.LENGTH_LONG).show();
        refresh();
    }
}
