import com.healthmarketscience.jackcess.*;
import com.clientcaller.app.AccessExport;
import com.clientcaller.app.AccessImport;
import com.clientcaller.app.ClientStore;
import java.io.File;
import java.util.*;

/** Creates an Access file with Hebrew columns and checks the app reads and updates it. Run by GitHub Actions. */
public class AccessTest {
    static final String T = "לקוחות";   // לקוחות
    static final String ID = "מזהה";             // מזהה
    static final String CELL = "סלולאר";  // סלולאר
    static final String SITE = "אתר";                 // אתר

    public static void main(String[] a) throws Exception {
        File f = new File("test.accdb");
        try (Database db = DatabaseBuilder.create(Database.FileFormat.V2010, f)) {
            Table t = new TableBuilder(T)
                    .addColumn(new ColumnBuilder(ID, DataType.LONG).setAutoNumber(true))
                    .addColumn(new ColumnBuilder("sp", DataType.TEXT))
                    .addColumn(new ColumnBuilder(CELL, DataType.TEXT))
                    .addColumn(new ColumnBuilder(SITE, DataType.LONG))
                    .toTable(db);
            t.addRow(Column.AUTO_NUMBER, "רון כהן", "052-1234567", 101);
            t.addRow(Column.AUTO_NUMBER, "Dana Levi", "0549876543", 102);
            t.addRow(Column.AUTO_NUMBER, "Keep Me", "0501111111", 103);
            new TableBuilder("other").addColumn(new ColumnBuilder("x", DataType.TEXT)).toTable(db);
        }
        byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
        List<String> tables = AccessImport.tables(f);
        ClientStore s = new ClientStore();
        s.load(AccessImport.read(f, T));
        int r = s.find("+972521234567");
        int r2 = s.find("054-987-6543");
        String res = "isAccess=" + AccessImport.isAccess(b) + " tables=" + tables.size() + " rows=" + s.rows.size()
                + " nameColIsSp=" + "sp".equals(s.headers.get(s.nameCol)) + " phoneCols=" + s.phoneCols
                + " found1=" + (r >= 0) + " found2=" + (r2 >= 0) + " site=" + (r >= 0 ? s.rows.get(r).get(3) : "");
        System.out.println("::notice title=ACCESS_READ::" + res);
        if (r < 0 || r2 < 0 || !AccessImport.isAccess(b)) System.exit(1);

        // ---- write back: edit row 1 (new name + site), add a new client, delete Dana
        List<String> edited = new ArrayList<>(s.rows.get(r));
        edited.set(1, "Ron Cohen EDITED");
        edited.set(3, "555");
        List<String> added = Arrays.asList("", "New Client", "053-2222222", "104");
        List<List<String>> changed = new ArrayList<>();
        changed.add(edited);
        changed.add(added);
        String danaId = s.rows.get(r2).get(0);
        AccessExport.Result x = AccessExport.apply(f, T, s.headers, changed, Collections.singletonList(danaId), ID, "sp");
        ClientStore s2 = new ClientStore();
        s2.load(AccessImport.read(f, T));
        boolean okEdit = s2.find("0521234567") >= 0 && s2.name(s2.find("0521234567")).equals("Ron Cohen EDITED")
                && s2.rows.get(s2.find("0521234567")).get(3).equals("555");
        boolean okAdd = s2.find("0532222222") >= 0;
        boolean okDel = s2.find("0549876543") < 0;
        boolean okKeep = s2.find("0501111111") >= 0;
        String res2 = "updated=" + x.updated + " added=" + x.added + " deleted=" + x.deleted + " newKeys=" + x.newKeys
                + " warnings=" + x.warnings + " rows=" + s2.rows.size()
                + " okEdit=" + okEdit + " okAdd=" + okAdd + " okDel=" + okDel + " okKeep=" + okKeep;
        System.out.println("::notice title=ACCESS_WRITE::" + res2);
        if (!(okEdit && okAdd && okDel && okKeep && x.newKeys.containsKey(1))) System.exit(1);
    }
}
