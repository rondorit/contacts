package com.clientcaller.app;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.DateTimeType;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Writes clients changed in the app back into an Access table:
 * updates matching records, adds new ones, deletes removed ones. Other records are left alone.
 * Plain Java (no Android).
 */
public final class AccessExport {
    static {
        // Android has no javax.script: never evaluate Access default values / validation rules.
        System.setProperty("com.healthmarketscience.jackcess.enableExpressionEvaluation", "false");
    }

    private AccessExport() { }

    public static final class Result {
        public int updated, added, deleted, notFound;
        /** For added rows: index in the "changed" list -> value the database generated for the key column. */
        public final Map<Integer, String> newKeys = new HashMap<>();
        public final List<String> warnings = new ArrayList<>();
    }

    /**
     * @param keyCol  column that identifies a record (e.g. "\u05DE\u05D6\u05D4\u05D4"); if null/missing, nameCol is used
     * @param changed rows (values in the order of headers) to update or add
     * @param deleted key values of records to delete
     */
    public static Result apply(File f, String tableName, List<String> headers, List<List<String>> changed,
                               List<String> deleted, String keyCol, String nameCol) throws IOException {
        Result res = new Result();
        try (Database db = new DatabaseBuilder(f).setAutoSync(false).setEvaluateExpressions(false).open()) {
            db.setDateTimeType(DateTimeType.DATE);
            Table t = db.getTable(tableName);
            if (t == null) throw new IOException("\u05D4\u05D8\u05D1\u05DC\u05D4 \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4: " + tableName);
            String key = keyCol != null && hasColumn(t, keyCol) && headers.contains(keyCol) ? keyCol : nameCol;
            if (key == null || !hasColumn(t, key)) throw new IOException("\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4 \u05E2\u05DE\u05D5\u05D3\u05D4 \u05DC\u05D6\u05D9\u05D4\u05D5\u05D9 \u05E8\u05E9\u05D5\u05DE\u05D5\u05EA");
            int keyIdx = headers.indexOf(key);

            // Index existing records by key.
            Map<String, Row> byKey = new LinkedHashMap<>();
            for (Row r : t) {
                String k = norm(AccessImport.str(r.get(key)));
                if (!k.isEmpty() && !byKey.containsKey(k)) byKey.put(k, r);
            }

            for (int i = 0; i < changed.size(); i++) {
                List<String> vals = changed.get(i);
                String k = keyIdx >= 0 && keyIdx < vals.size() ? norm(vals.get(keyIdx)) : "";
                Row existing = k.isEmpty() ? null : byKey.get(k);
                if (existing != null) {
                    boolean diff = false;
                    for (Column c : t.getColumns()) {
                        int hi = headers.indexOf(c.getName());
                        if (hi < 0 || c.isAutoNumber() || c.getName().equals(key) && !k.isEmpty()) continue;
                        String nv = hi < vals.size() ? vals.get(hi) : "";
                        if (norm(AccessImport.str(existing.get(c.getName()))).equals(norm(nv))) continue;
                        try {
                            existing.put(c.getName(), convert(c, nv));
                            diff = true;
                        } catch (Exception e) {
                            res.warnings.add(c.getName() + ": \"" + nv + "\" \u05DC\u05D0 \u05E0\u05E9\u05DE\u05E8 (\u05E1\u05D5\u05D2 \u05E9\u05D3\u05D4 \u05DC\u05D0 \u05DE\u05EA\u05D0\u05D9\u05DD)");
                        }
                    }
                    if (diff) {
                        t.updateRow(existing);
                        res.updated++;
                    }
                } else {
                    Map<String, Object> m = new HashMap<>();
                    for (Column c : t.getColumns()) {
                        if (c.isAutoNumber()) continue;
                        int hi = headers.indexOf(c.getName());
                        if (hi < 0) continue;
                        String nv = hi < vals.size() ? vals.get(hi) : "";
                        try {
                            Object o = convert(c, nv);
                            if (o != null) m.put(c.getName(), o);
                        } catch (Exception e) {
                            res.warnings.add(c.getName() + ": \"" + nv + "\" \u05DC\u05D0 \u05E0\u05E9\u05DE\u05E8 (\u05E1\u05D5\u05D2 \u05E9\u05D3\u05D4 \u05DC\u05D0 \u05DE\u05EA\u05D0\u05D9\u05DD)");
                        }
                    }
                    t.addRowFromMap(m);
                    Object gen = m.get(key);
                    if (gen != null) res.newKeys.put(i, AccessImport.str(gen));
                    res.added++;
                }
            }

            for (String d : deleted) {
                Row r = byKey.get(norm(d));
                if (r == null) {
                    res.notFound++;
                    continue;
                }
                t.deleteRow(r);
                byKey.remove(norm(d));
                res.deleted++;
            }
            db.flush();
        }
        return res;
    }

    private static boolean hasColumn(Table t, String name) {
        for (Column c : t.getColumns()) if (c.getName().equals(name)) return true;
        return false;
    }

    static String norm(String v) {
        if (v == null) return "";
        v = v.trim();
        if (v.matches("-?\\d+\\.0+")) v = v.substring(0, v.indexOf('.'));
        return v;
    }

    /** Text from the app -> the column's Access type. Empty -> null. */
    static Object convert(Column c, String v) throws Exception {
        v = v == null ? "" : v.trim();
        DataType dt = c.getType();
        if (v.isEmpty()) return dt == DataType.BOOLEAN ? Boolean.FALSE : null;
        switch (dt) {
            case BYTE:
                return Byte.parseByte(norm(v));
            case INT:
                return Short.parseShort(norm(v));
            case LONG:
                return Integer.parseInt(norm(v));
            case BIG_INT:
                return Long.parseLong(norm(v));
            case FLOAT:
                return Float.parseFloat(v);
            case DOUBLE:
                return Double.parseDouble(v);
            case MONEY:
            case NUMERIC:
                return new BigDecimal(v);
            case BOOLEAN:
                return v.equals("\u05DB\u05DF") || v.equalsIgnoreCase("true") || v.equals("1") || v.equalsIgnoreCase("yes");
            case SHORT_DATE_TIME:
                for (String p : new String[]{"dd/MM/yyyy HH:mm", "dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd"}) {
                    try {
                        SimpleDateFormat f = new SimpleDateFormat(p, Locale.US);
                        f.setLenient(false);
                        return f.parse(v);
                    } catch (Exception ignored) { }
                }
                throw new IllegalArgumentException("date");
            default:
                return v;
        }
    }
}
