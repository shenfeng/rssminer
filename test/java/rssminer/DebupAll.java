package rssminer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.tools.Utils;

public class DebupAll {

    static Logger logger = LoggerFactory.getLogger(DebupAll.class);
    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";

    public static void main(String[] args) throws SQLException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");

        PreparedStatement ps = con.prepareStatement("select id, simhash from feeds");
        Map<Long, List<Integer>> map = new TreeMap<Long, List<Integer>>();
        ResultSet rs = ps.executeQuery();

        int max = Utils.getMaxID();
        long[] hashes = new long[max + 1];
        for (int i = 0; i < hashes.length; i++) {
            hashes[i] = -1;
        }

        while (rs.next()) {
            int id = rs.getInt(1);
            long hash = rs.getLong(2);
            hashes[id] = hash;

            if (hash != -1) {
                List<Integer> ids = map.get(hash);
                if (ids == null) {
                    ids = new ArrayList<Integer>(1);
                    map.put(hash, ids);
                }
                ids.add(id);
            }

        }

        int count = 0;
        for (Entry<Long, List<Integer>> e : map.entrySet()) {
            List<Integer> feeds = e.getValue();
            if (feeds.size() > 1) {
                count += e.getValue().size() - 1;
            }
        }
        System.out.println(count);

        count = 0;
        for (int i = 0; i < hashes.length; i++) {
            if (i % 10000 == 0) {
                logger.info("handing {}, count {}, total " + hashes.length, i, count);
            }
            long hash = hashes[i];
            if (hash == -1) {
                continue;
            }
            for (int j = i + 1; j < hashes.length; j++) {
                long h = hashes[j];
                if (h == -1) {
                    continue;
                }
                if (rssminer.Utils.hammingDistance(hash, h) < 2) {
                    count += 1;
                    hashes[j] = -1;
                }
            }
        }
        System.out.println(count);
    }

}
