import com.healthmarketscience.jackcess.*;
import com.clientcaller.app.AccessImport;
import com.clientcaller.app.ClientStore;
import java.io.File;
import java.util.List;

/** Creates an Access file with Hebrew columns and checks the app reads it. Run by GitHub Actions. */
public class AccessTest {
    public static void main(String[] a) throws Exception {
        File f = new File("test.accdb");
        try (Database db = DatabaseBuilder.create(Database.FileFormat.V2010, f)) {
            Table t = new TableBuilder("לקוחות")
                    .addColumn(new ColumnBuilder("sp", DataType.TEXT))
                    .addColumn(new ColumnBuilder("סלולאר", DataType.TEXT))
                    .addColumn(new ColumnBuilder("אתר", DataType.LONG))
                    .addColumn(new ColumnBuilder("זהות", DataType.TEXT))
                    .toTable(db);
            t.addRow("רון כהן", "052-1234567", 101, "123");
            t.addRow("Dana Levi", "0549876543", 102, "456");
            new TableBuilder("other").addColumn(new ColumnBuilder("x", DataType.TEXT)).toTable(db);
        }
        byte[] b = java.nio.file.Files.readAllBytes(f.toPath());
        List<String> tables = AccessImport.tables(f);
        ClientStore s = new ClientStore();
        s.load(AccessImport.read(f, "לקוחות"));
        int r = s.find("+972521234567");
        int r2 = s.find("054-987-6543");
        String res = "isAccess=" + AccessImport.isAccess(b) + " tables=" + tables.size() + " rows=" + s.rows.size()
                + " nameColIsSp=" + "sp".equals(s.headers.get(s.nameCol)) + " phoneCols=" + s.phoneCols
                + " found1=" + (r >= 0) + " found2=" + (r2 >= 0 ? s.name(r2) : "NO")
                + " site=" + (r >= 0 ? s.rows.get(r).get(2) : "");
        System.out.println("::notice title=ACCESS_TEST::" + res);
        if (r < 0 || r2 < 0 || !AccessImport.isAccess(b)) System.exit(1);
    }
}
