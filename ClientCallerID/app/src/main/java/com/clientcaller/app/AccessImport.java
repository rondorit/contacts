package com.clientcaller.app;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads a Microsoft Access (.accdb / .mdb) table into rows. Plain Java (no Android). */
public final class AccessImport {
    private AccessImport() { }

    /** True if the bytes are an Access database file. */
    public static boolean isAccess(byte[] d) {
        if (d.length < 20) return false;
        String sig = new String(d, 4, 15, StandardCharsets.ISO_8859_1);
        return sig.startsWith("Standard Jet DB") || sig.startsWith("Standard ACE DB");
    }

    /** "mdb" for Access 2000-2003 files, "accdb" for Access 2007 and newer. */
    public static String extension(File f) {
        try (Database db = new DatabaseBuilder(f).setReadOnly(true).open()) {
            String n = db.getFileFormat().name();
            return n.startsWith("V2000") || n.startsWith("V2003") || n.startsWith("V1997") || n.contains("JET") ? "mdb" : "accdb";
        } catch (Exception e) {
            return "accdb";
        }
    }

    /** Opens the file and reads every row of the table; throws if the file is damaged. */
    public static int verify(File f, String table) throws IOException {
        try (Database db = new DatabaseBuilder(f).setReadOnly(true).open()) {
            com.healthmarketscience.jackcess.Table t = db.getTable(table);
            if (t == null) throw new IOException("table missing");
            int n = 0;
            for (Row r : t) { r.size(); n++; }
            return n;
        }
    }

    public static List<String> tables(File f) throws IOException {
        try (Database db = new DatabaseBuilder(f).setReadOnly(true).open()) {
            return new ArrayList<>(db.getTableNames());
        }
    }

    /** First row = column names, then one row per record. */
    public static List<List<String>> read(File f, String tableName) throws IOException {
        List<List<String>> out = new ArrayList<>();
        try (Database db = new DatabaseBuilder(f).setReadOnly(true).open()) {
            Table t = db.getTable(tableName);
            if (t == null) throw new IOException("\u05D4\u05D8\u05D1\u05DC\u05D4 \u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0\u05D4: " + tableName);
            List<String> names = new ArrayList<>();
            for (Column c : t.getColumns()) names.add(c.getName());
            out.add(names);
            for (Row r : t) {
                List<String> row = new ArrayList<>();
                for (String n : names) row.add(str(r.get(n)));
                out.add(row);
            }
        }
        return out;
    }

    static String str(Object v) {
        if (v == null) return "";
        if (v instanceof Double || v instanceof Float || v instanceof BigDecimal) {
            try {
                return new BigDecimal(v.toString()).stripTrailingZeros().toPlainString();
            } catch (Exception e) {
                return v.toString();
            }
        }
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return ((Boolean) v) ? "\u05DB\u05DF" : "\u05DC\u05D0";
        if (v instanceof java.util.Date) {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.US).format((java.util.Date) v);
        }
        if (v instanceof java.time.LocalDateTime) {
            java.time.LocalDateTime d = (java.time.LocalDateTime) v;
            return String.format(Locale.US, "%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
        }
        if (v instanceof byte[]) return "";
        return v.toString().trim();
    }
}
