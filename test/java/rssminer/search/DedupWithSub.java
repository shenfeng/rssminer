package rssminer.search;

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

//class HashID {
//    public final int id;
//    public final int 
//}

// 5996: 	[697203, 665429, 665430, 665441, 716582, 708895, 750705, 750704, 787612, 687214]
// 45:  [153621, 145187, 147411, 150362]

public class DedupWithSub {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    final static int STEP = 1000;
    final static int REPORT = STEP * 10;

    public static void main(String[] args) throws SQLException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");

        PreparedStatement ps = con
                .prepareStatement("select id, simhash from feeds where rss_link_id = ?");

        int count = 0;

        for (int i = 1; i < 6558; i++) {
            ps.setInt(1, i);
            ResultSet rs = ps.executeQuery();
            Map<Long, List<Integer>> map = new TreeMap<Long, List<Integer>>();

            while (rs.next()) {
                int id = rs.getInt(1);
                long hash = rs.getLong(2);
                if (hash != -1) {
                    List<Integer> ids = map.get(hash);
                    if (ids == null) {
                        ids = new ArrayList<Integer>(1);
                        map.put(hash, ids);
                    }
                    ids.add(id);
                }
            }

            for (Entry<Long, List<Integer>> e : map.entrySet()) {
                if (e.getValue().size() > 1) {
                    System.out.println(i + ": " + "\t" + e.getValue());
                    count += e.getValue().size() - 1;
                }
            }

        }

        System.out.println(count);

    }
}
