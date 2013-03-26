package rssminer.db;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.util.zip.Deflater;

public class CompressionTest {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";

    // @Test
    public void testDeflater() throws IOException {

        // String str = IOUtils.toString(new
        // FileInputStream("/Users/feng/workspace/rssminer/test/java/rssminer/db/CompressionTest.java"));
        String str = "-----";
        byte[] input = str.getBytes("UTF-8");

        for (int i = Deflater.BEST_SPEED; i < Deflater.BEST_COMPRESSION; i++) {
            int compressedSize = getCompressedSize(i, input);
            System.out.println(i + "\t" + input.length + "\t" + compressedSize);
        }
    }

    private static int getCompressedSize(int level, byte[] input) {
        Deflater deflater = new Deflater(level);
        deflater.setInput(input);
        deflater.finish();

        byte[] buffer = new byte[4096];

        int count = 0;
        while (!deflater.finished()) {
            count += deflater.deflate(buffer);
        }
        return count;
    }

    private static long byteCount = 0, compressedSize = 0, combinedSize = 0, feedCount = 0;

    public static void main(String[] args) throws SQLException, UnsupportedEncodingException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement stat = con.createStatement();

        ResultSet max = stat.executeQuery("select max(id) from feed_data");
        max.next();
        int maxId = max.getInt(1);
        PreparedStatement ps = con
                .prepareStatement("select summary from feed_data where id > ? and id <= ?");
        System.out.println("maxid = " + maxId);

        StringBuilder sb = new StringBuilder();
        final int LEVEL = 6;

        for (int i = 0; i < maxId;) {
            ps.setInt(1, i);
            i += 10;
            ps.setInt(2, i);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                feedCount += 1;
                String summary = rs.getString(1);
                sb.append(summary);

                byte[] bytes = summary.getBytes("utf8");
                byteCount += bytes.length;
                compressedSize += getCompressedSize(LEVEL, bytes);
            }

            combinedSize += getCompressedSize(LEVEL, sb.toString().getBytes("utf8"));
            sb.setLength(0);
            if (i % 40000 == 0) {
                printMessage(i);
            }
        }
        printMessage(maxId);
    }

    private static void printMessage(int i) {
        System.out
                .printf("%s, index %d, %d feed, %.2fm, avg: %d, compress %.2fm, avg %d, combined %.2fm\n",
                        new java.util.Date(), i, feedCount, (double) byteCount / 1024 / 1024,
                        byteCount / feedCount, (double) compressedSize / 1024 / 1024,
                        compressedSize / feedCount, (double) combinedSize / 1024 / 1024);
    }

}
