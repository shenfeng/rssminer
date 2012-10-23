/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import clojure.lang.Keyword;
import me.shenfeng.dbcp.PerThreadDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.SimHash;
import rssminer.search.Searcher;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SimHashUpdater {

    private static final Logger logger = LoggerFactory
            .getLogger(SimHashUpdater.class);

    public static void main(String[] args) throws IOException, SQLException {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        Searcher.initGlobalSearcher("/var/rssminer/index", config);

        Connection con = db.getConnection();
        Statement stat = con.createStatement();

        ResultSet rs = stat.executeQuery("select max(id) from feed_data");
        rs.next();
        int max = rs.getInt(1);

        PreparedStatement ps = con
                .prepareStatement("select id, summary from feed_data where id >= ? and id < ?");

        PreparedStatement update = con
                .prepareStatement("update feeds set simhash = ? where id = ?");

        final int step = 5000;

        for (int i = 1; i <= max; i++) {
            ps.setInt(1, i);
            ps.setInt(2, i + step);
            rs = ps.executeQuery();
            while (rs.next()) {
                i = rs.getInt(1); // update id;
                String summary = rs.getString(2);
                long h2 = SimHash.simHash(summary);
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
