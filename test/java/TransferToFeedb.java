import org.httpkit.*;
import org.httpkit.client.*;

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.zip.Deflater;

/**
 * Created with IntelliJ IDEA.
 * User: feng
 * Date: 5/12/13
 * Time: 7:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransferToFeedb {

    static String JDBC_URL = "jdbc:mysql://192.168.1.101/rssminer?cachePrepStmts=true&useServerPrepStmts=true";

    private static DynamicBytes getCompressedSize(int level, byte[] input) {
        Deflater deflater = new Deflater(level);
        deflater.setInput(input);
        deflater.finish();

        DynamicBytes bytes = new DynamicBytes(1024);

        byte[] buffer = new byte[4096];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            bytes.append(buffer, count);
        }
        return bytes;
    }

    private static long startTime = System.currentTimeMillis();

    public static void main(String[] args) throws SQLException, IOException, InterruptedException {
        HttpClient client = new HttpClient();
        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement stat = con.createStatement();

        ResultSet max = stat.executeQuery("select max(id) from feed_data");
        max.next();
        int maxId = max.getInt(1);
        PreparedStatement ps = con
                .prepareStatement("select summary, id from feed_data where id > ? and id <= ?");
        System.out.println("maxid = " + maxId);


        final int LEVEL = 6;

        final Semaphore semaphore = new Semaphore(10);

        for (int i = 0; i < maxId; ) {
            ps.setInt(1, i);
            i += 50;
            ps.setInt(2, i);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String summary = rs.getString(1);
                DynamicBytes bytes = getCompressedSize(LEVEL, summary.getBytes("utf8"));
                RequestConfig cfg = new RequestConfig(HttpMethod.POST, new HashMap<String, Object>(), new BytesInputStream(bytes.get(), bytes.length()),
                        400000, 10000);
                String url = "http://127.0.0.1:7167/d/feeds?id=" + rs.getInt(2) + "&len=" + bytes.length();
                semaphore.acquire();
                final long time = System.currentTimeMillis();
                client.exec(url, cfg, null, new IRespListener() {
                    public void onBodyReceived(byte[] bytes, int i) throws AbortException {
                    }

                    public void onCompleted() {
                    }

                    public void onHeadersReceived(Map<String, String> stringStringMap) throws AbortException {
                    }

                    public void onInitialLineReceived(HttpVersion httpVersion, HttpStatus httpStatus) throws AbortException {
                        semaphore.release();
                        long t = System.currentTimeMillis() - time;
                        if (t > 2000) {
                            System.out.printf("time:%dms\n", t);
                        }
                        if (httpStatus.getCode() != 200) {
                            System.out.println("-------------");
                        }
                    }

                    public void onThrowable(Throwable throwable) {
                        semaphore.release();
                        throwable.printStackTrace();
                    }
                });
            }

            if (i % 10000 == 0) {
                System.out.println(new Date() + "\t" + i);
            }
        }
    }
}
