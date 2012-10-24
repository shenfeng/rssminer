/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.Utils;

import java.io.IOException;
import java.sql.*;

public class SimHashUpdater {

    private static final Logger logger = LoggerFactory
            .getLogger(SimHashUpdater.class);

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";

    public static void main(String[] args) throws IOException, SQLException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement stat = con.createStatement();

        ResultSet rs = stat.executeQuery("select max(id) from feed_data");
        rs.next();
        int max = rs.getInt(1);

        PreparedStatement ps = con
                .prepareStatement("select d.id, d.summary, f.title from feed_data d join feeds f on f.id = d.id where f.id >= ? and f.id <= ?");

        PreparedStatement update = con
                .prepareStatement("update feeds set simhash = ? where id = ?");

        final int step = 5000;

        for (int i = 1; i <= max; ++i) {
            ps.setInt(1, i);
            ps.setInt(2, i + step);
            rs = ps.executeQuery();
            while (rs.next()) {
                i = rs.getInt(1); // update id;
                String summary = rs.getString(2);
                long h2 = Utils.simHash(summary, rs.getString(3));
                if (h2 != -1) {
                    update.setLong(1, h2);
                    update.setInt(2, i);
                    update.addBatch();
                }
                if (i % 5000 == 0) {
                    logger.info("handle " + i);
                }
            }
            update.executeBatch();
        }
    }
}
