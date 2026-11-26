package com.clientcaller.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The client table: reads Excel (.xlsx) or CSV, finds the phone and name columns,
 * and looks clients up by phone number. Plain Java, no Android dependencies.
 */
public class ClientStore {

    public final List<String> headers = new ArrayList<>();
    public final List<List<String>> rows = new ArrayList<>();
    public final List<Integer> phoneCols = new ArrayList<>();
    public int nameCol = 0;
    private final Map<String, Integer> byPhone = new HashMap<>();

    private static final String[] PHONE_WORDS = {
            "phone", "tel", "mobile", "cell",
            "\u05D8\u05DC\u05E4\u05D5\u05DF", "\u05D8\u05DC.", "\u05D8\u05DC'", "\u05E0\u05D9\u05D9\u05D3", "\u05E4\u05DC\u05D0\u05E4\u05D5\u05DF", "\u05E1\u05DC\u05D5\u05DC\u05E8"
    };
    private static final String[] NAME_WORDS = {
            "name", "\u05E9\u05DD", "client", "customer", "\u05DC\u05E7\u05D5\u05D7", "company", "\u05D7\u05D1\u05E8\u05D4", "\u05E2\u05E1\u05E7"
    };
    private static final String REL_NS =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    // ------------------------------------------------------------------ loading

    /** Builds the table from raw rows; the first non-empty row is the header row. */
    public void load(List<List<String>> table) {
        headers.clear();
        rows.clear();
        int start = 0;
        while (start < table.size() && isBlank(table.get(start))) start++;
        if (start < table.size()) {
            int width = 0;
            for (List<String> r : table) width = Math.max(width, r.size());
            List<String> h = table.get(start);
            for (int c = 0; c < width; c++) {
                String v = c < h.size() ? h.get(c).trim() : "";
                headers.add(v.isEmpty() ? "\u05E2\u05DE\u05D5\u05D3\u05D4 " + (c + 1) : v);
            }
            for (int i = start + 1; i < table.size(); i++) {
                List<String> r = table.get(i);
                if (isBlank(r)) continue;
                List<String> row = new ArrayList<>();
                for (int c = 0; c < width; c++) row.add(c < r.size() ? r.get(c).trim() : "");
                rows.add(row);
            }
        }
        detectColumns();
        index();
    }

    private void detectColumns() {
        phoneCols.clear();
        for (int c = 0; c < headers.size(); c++) {
            String h = headers.get(c).toLowerCase(Locale.ROOT).trim();
            boolean match = h.equals("\u05D8\u05DC");
            for (String w : PHONE_WORDS) if (h.contains(w)) match = true;
            if (match) phoneCols.add(c);
        }
        if (phoneCols.isEmpty()) {
            // No header says "phone": pick columns whose values mostly look like phone numbers.
            for (int c = 0; c < headers.size(); c++) {
                int filled = 0, phoney = 0;
                for (List<String> r : rows) {
                    String v = r.get(c);
                    if (v.isEmpty()) continue;
                    filled++;
                    if (looksLikePhone(v)) phoney++;
                }
                if (filled > 0 && phoney * 10 >= filled * 6) phoneCols.add(c);
            }
        }
        nameCol = -1;
        outer:
        for (String w : NAME_WORDS) {
            for (int c = 0; c < headers.size(); c++) {
                if (phoneCols.contains(c)) continue;
                if (headers.get(c).toLowerCase(Locale.ROOT).contains(w)) {
                    nameCol = c;
                    break outer;
                }
            }
        }
        if (nameCol < 0) {
            for (int c = 0; c < headers.size(); c++) {
                if (!phoneCols.contains(c)) { nameCol = c; break; }
            }
        }
        if (nameCol < 0) nameCol = 0;
    }

    private void index() {
        byPhone.clear();
        for (int r = 0; r < rows.size(); r++) {
            for (int c : phoneCols) {
                for (String part : phoneParts(rows.get(r).get(c))) {
                    String k = key(part);
                    if (!byPhone.containsKey(k)) byPhone.put(k, r);
                }
            }
        }
    }

    // ------------------------------------------------------------------ phone numbers

    /**
     * Comparison key for a phone number, so that "050-123-4567", "+972 50 1234567",
     * "00972501234567" and an Excel number cell 501234567 all match.
     */
    public static String key(String raw) {
        if (raw == null) return "";
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') d.append(ch);
        }
        String s = d.toString();
        if (s.startsWith("00")) s = s.substring(2);
        if (s.startsWith("972") && s.length() >= 11) s = s.substring(3);
        int i = 0;
        while (i < s.length() - 1 && s.charAt(i) == '0') i++;
        s = s.substring(i);
        if (s.length() > 9) s = s.substring(s.length() - 9);
        return s;
    }

    /** A cell may hold several numbers: "050-1234567 / 09-8765432". */
    public static List<String> phoneParts(String cell) {
        List<String> out = new ArrayList<>();
        if (cell == null) return out;
        for (String p : cell.split("[,;/|\\n]+")) {
            p = p.trim();
            if (key(p).length() >= 7) out.add(p);
        }
        return out;
    }

    private static boolean looksLikePhone(String v) {
        return v.matches("[+0-9()\\-\\s.]{7,}") && key(v).length() >= 8;
    }

    /** Row index of the client with this phone number, or -1. */
    public int find(String number) {
        String k = key(number);
        if (k.length() < 7) return -1;
        Integer r = byPhone.get(k);
        return r == null ? -1 : r;
    }

    // ------------------------------------------------------------------ display helpers

    public String name(int r) {
        String v = rows.get(r).get(nameCol);
        if (!v.isEmpty()) return v;
        String p = firstPhone(r);
        return p.isEmpty() ? "\u05DC\u05E7\u05D5\u05D7" : p;
    }

    /** Comparison keys of all phone numbers of a client. */
    public List<String> keysForRow(int r) {
        List<String> out = new ArrayList<>();
        for (int c : phoneCols) {
            for (String p : phoneParts(rows.get(r).get(c))) out.add(key(p));
        }
        return out;
    }

    public String firstPhone(int r) {
        for (int c : phoneCols) {
            List<String> parts = phoneParts(rows.get(r).get(c));
            if (!parts.isEmpty()) return parts.get(0);
        }
        return "";
    }

    /** All non-empty fields except the name, one "Header: value" per line. */
    public String details(int r) {
        StringBuilder sb = new StringBuilder();
        List<String> row = rows.get(r);
        for (int c = 0; c < headers.size(); c++) {
            if (c == nameCol || row.get(c).isEmpty()) continue;
            if (sb.length() > 0) sb.append('\n');
            sb.append(headers.get(c)).append(": ").append(row.get(c));
        }
        return sb.toString();
    }

    /** Rows matching a name/text or phone-digits search. Empty query returns everything. */
    public List<Integer> search(String query) {
        List<Integer> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String qd = digits(q);
        if (qd.startsWith("0")) qd = qd.substring(1);
        boolean byDigits = qd.length() >= 3 && qd.length() * 2 >= q.replace(" ", "").length();
        for (int r = 0; r < rows.size(); r++) {
            if (q.isEmpty()) { out.add(r); continue; }
            boolean hit = false;
            for (String v : rows.get(r)) {
                if (v.toLowerCase(Locale.ROOT).contains(q)) { hit = true; break; }
            }
            if (!hit && byDigits) {
                for (int c : phoneCols) {
                    if (digits(rows.get(r).get(c)).contains(qd)) { hit = true; break; }
                }
            }
            if (hit) out.add(r);
        }
        return out;
    }

    private static String digits(String s) {
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') d.append(ch);
        }
        return d.toString();
    }

    private static boolean isBlank(List<String> r) {
        for (String v : r) if (v != null && !v.trim().isEmpty()) return false;
        return true;
    }

    // ------------------------------------------------------------------ saving

    public void saveFile(File f) throws IOException {
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            writeLine(w, headers);
            for (List<String> r : rows) writeLine(w, r);
        }
    }

    public void loadFile(File f) throws IOException {
        String text;
        try (InputStream in = new FileInputStream(f)) {
            text = new String(readAll(in), StandardCharsets.UTF_8);
        }
        List<List<String>> table = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            List<String> row = new ArrayList<>();
            for (String cell : line.split("\t", -1)) row.add(unescape(cell));
            table.add(row);
        }
        load(table);
    }

    private static void writeLine(Writer w, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) w.write('\t');
            w.write(cells.get(i).replace("\\", "\\\\").replace("\t", "\\t")
                    .replace("\r", "").replace("\n", "\\n"));
        }
        w.write('\n');
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                sb.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ file reading

    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    /** Reads an .xlsx or .csv file into raw rows. */
    public static List<List<String>> readTable(byte[] data) throws Exception {
        if (data.length >= 2 && data[0] == 'P' && data[1] == 'K') {
            return readXlsx(new ByteArrayInputStream(data));
        }
        if (data.length >= 4 && (data[0] & 0xFF) == 0xD0 && (data[1] & 0xFF) == 0xCF) {
            throw new IOException("\u05D6\u05D4\u05D5 \u05E7\u05D5\u05D1\u05E5 xls \u05D9\u05E9\u05DF. \u05D1\u05D0\u05E7\u05E1\u05DC \u05D1\u05D7\u05E8 \u05E9\u05DE\u05D9\u05E8\u05D4 \u05D1\u05E9\u05DD > \u05D7\u05D5\u05D1\u05E8\u05EA \u05E2\u05D1\u05D5\u05D3\u05D4 \u05E9\u05DC Excel (.xlsx) \u05D5\u05D9\u05D9\u05D1\u05D0 \u05D0\u05D5\u05EA\u05D5.");
        }
        return readCsv(data);
    }

    public static List<List<String>> readCsv(byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        if (text.indexOf('\uFFFD') >= 0 && Charset.isSupported("windows-1255")) {
            text = new String(data, Charset.forName("windows-1255")); // Hebrew Excel CSV
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);

        int nl = text.indexOf('\n');
        String first = nl < 0 ? text : text.substring(0, nl);
        char delim = ',';
        int best = count(first, ',');
        if (count(first, ';') > best) { delim = ';'; best = count(first, ';'); }
        if (count(first, '\t') > best) delim = '\t';

        List<List<String>> out = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < len && text.charAt(i + 1) == '"') { f.append('"'); i++; }
                    else quoted = false;
                } else {
                    f.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == delim) {
                row.add(f.toString());
                f.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < len && text.charAt(i + 1) == '\n') i++;
                row.add(f.toString());
                f.setLength(0);
                out.add(row);
                row = new ArrayList<>();
            } else {
                f.append(ch);
            }
        }
        if (f.length() > 0 || !row.isEmpty()) {
            row.add(f.toString());
            out.add(row);
        }
        return out;
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    /** Reads the first worksheet of an .xlsx file. */
    public static List<List<String>> readXlsx(InputStream in) throws Exception {
        Map<String, byte[]> files = new HashMap<>();
        try (ZipInputStream z = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = z.getNextEntry()) != null) {
                String n = e.getName().replace('\\', '/');
                if (n.startsWith("/")) n = n.substring(1);
                if (n.startsWith("xl/") && (n.endsWith(".xml") || n.endsWith(".rels"))) {
                    files.put(n, readAll(z));
                }
            }
        }

        List<String> shared = new ArrayList<>();
        byte[] ss = files.get("xl/sharedStrings.xml");
        if (ss != null) {
            Element sst = parse(ss).getDocumentElement();
            for (Element si : children(sst, "si")) shared.add(text(si));
        }

        String path = firstSheetPath(files);
        byte[] sheet = path == null ? null : files.get(path);
        if (sheet == null) throw new IOException("\u05DC\u05D0 \u05E0\u05DE\u05E6\u05D0 \u05D2\u05D9\u05DC\u05D9\u05D5\u05DF \u05D1\u05E7\u05D5\u05D1\u05E5");

        List<List<String>> out = new ArrayList<>();
        NodeList sd = parse(sheet).getElementsByTagNameNS("*", "sheetData");
        if (sd.getLength() == 0) return out;
        for (Element rowEl : children((Element) sd.item(0), "row")) {
            List<String> row = new ArrayList<>();
            int next = 0;
            for (Element c : children(rowEl, "c")) {
                String ref = c.getAttribute("r");
                int col = ref.isEmpty() ? next : colIndex(ref);
                String t = c.getAttribute("t");
                String v = childText(c, "v");
                String val;
                if ("s".equals(t)) {
                    try { val = shared.get(Integer.parseInt(v.trim())); } catch (Exception ex) { val = ""; }
                } else if ("inlineStr".equals(t)) {
                    List<Element> is = children(c, "is");
                    val = is.isEmpty() ? "" : text(is.get(0));
                } else if ("b".equals(t)) {
                    val = "1".equals(v.trim()) ? "TRUE" : "FALSE";
                } else if ("str".equals(t) || "e".equals(t)) {
                    val = v;
                } else {
                    val = cleanNumber(v);
                }
                while (row.size() < col) row.add("");
                if (row.size() == col) row.add(val); else row.set(col, val);
                next = col + 1;
            }
            out.add(row);
        }
        return out;
    }

    private static String firstSheetPath(Map<String, byte[]> files) throws Exception {
        byte[] wb = files.get("xl/workbook.xml");
        byte[] rels = files.get("xl/_rels/workbook.xml.rels");
        if (wb != null && rels != null) {
            NodeList sheets = parse(wb).getElementsByTagNameNS("*", "sheet");
            if (sheets.getLength() > 0) {
                String rid = ((Element) sheets.item(0)).getAttributeNS(REL_NS, "id");
                NodeList rl = parse(rels).getElementsByTagNameNS("*", "Relationship");
                for (int i = 0; i < rl.getLength(); i++) {
                    Element el = (Element) rl.item(i);
                    if (rid.equals(el.getAttribute("Id"))) {
                        String target = el.getAttribute("Target");
                        String p = target.startsWith("/") ? target.substring(1) : "xl/" + target;
                        if (files.containsKey(p)) return p;
                    }
                }
            }
        }
        List<String> names = new ArrayList<>();
        for (String n : files.keySet()) {
            if (n.startsWith("xl/worksheets/") && n.endsWith(".xml") && !n.contains("_rels")) names.add(n);
        }
        Collections.sort(names);
        return names.isEmpty() ? null : names.get(0);
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        try { f.setExpandEntityReferences(false); } catch (Exception ignored) { }
        DocumentBuilder b = f.newDocumentBuilder();
        return b.parse(new ByteArrayInputStream(xml));
    }

    private static String localName(Node n) {
        String ln = n.getLocalName();
        if (ln == null) {
            ln = n.getNodeName();
            int i = ln.indexOf(':');
            if (i >= 0) ln = ln.substring(i + 1);
        }
        return ln;
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(localName(n))) out.add((Element) n);
        }
        return out;
    }

    private static String childText(Element parent, String name) {
        List<Element> c = children(parent, name);
        return c.isEmpty() ? "" : c.get(0).getTextContent();
    }

    /** Text of a shared/inline string: all <t> pieces, skipping phonetic hints. */
    private static String text(Element e) {
        StringBuilder sb = new StringBuilder();
        collectText(e, sb);
        return sb.toString();
    }

    private static void collectText(Element e, StringBuilder sb) {
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            String ln = localName(n);
            if ("t".equals(ln)) sb.append(n.getTextContent());
            else if (!"rPh".equals(ln)) collectText((Element) n, sb);
        }
    }

    private static int colIndex(String ref) {
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = Character.toUpperCase(ref.charAt(i));
            if (ch < 'A' || ch > 'Z') break;
            col = col * 26 + (ch - 'A' + 1);
        }
        return Math.max(0, col - 1);
    }

    private static String cleanNumber(String v) {
        v = v.trim();
        if (v.isEmpty()) return v;
        try {
            if (v.matches("-?[0-9.]+([eE][-+]?[0-9]+)?")) {
                String plain = new BigDecimal(v).stripTrailingZeros().toPlainString();
                return plain;
            }
        } catch (Exception ignored) { }
        return v;
    }
}
