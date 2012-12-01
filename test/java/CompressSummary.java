import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.DeflaterOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompressSummary {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    final static int STEP = 1000;
    final static int REPORT = STEP * 10;

    private static Logger logger = LoggerFactory.getLogger(CompressSummary.class);

    public static void main(String[] args) throws SQLException, IOException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select * from feed_data limit 400000");

        int uncompressed = 0, compressed = 0;

        int count = 0;

        while (rs.next()) {
            count++;
            if (count % 5000 == 0) {
                logger.info("handle {}, {}, {}",
                        new Object[] { count, uncompressed, compressed });
            }
            String summary = rs.getString(2);
            byte[] bytes = summary.getBytes();

            uncompressed += bytes.length;

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            DeflaterOutputStream dos = new DeflaterOutputStream(bos);
            dos.write(bytes);
            dos.close();

            compressed += bos.toByteArray().length;

        }
    }
}
