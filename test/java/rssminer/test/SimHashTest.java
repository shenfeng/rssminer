/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.test;

import clojure.lang.Keyword;
import junit.framework.Assert;
import me.shenfeng.dbcp.PerThreadDataSource;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.SimHash;
import rssminer.search.Searcher;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SimHashTest {
    private static final Logger logger = LoggerFactory
            .getLogger(SimHashTest.class);

    @Test
    public void testHammingDistance() {
        int diff = 1;
        String strx = "0";
        String stry = "1";
        for (int i = 0; i < 63; i++) {
            long x = Long.parseLong(strx, 2);
            long y = Long.parseLong(stry, 2);
            Assert.assertSame(SimHash.hammingDistance(x, y), diff);
            double r = Math.random();
            if (r > 0.7) {
                diff += 1;
                strx += "1";
                stry += "0";
            } else if (r > 0.4) {
                diff += 1;
                strx += "0";
                stry += "1";
            } else if (r > 0.2) {
                strx += "1";
                stry += "1";
            } else {
                strx += "0";
                stry += "0";
            }
        }
    }

    private static Connection con;
    private static final int TOTAL = 1000000;

    static {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        try {
            Searcher.initGlobalSearcher("/var/rssminer/index", config);
            con = db.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithLuence() throws SQLException {
        PreparedStatement ps = con
                .prepareStatement("select summary from feed_data where id = ?");
        for (int i = 1; i < TOTAL; i++) {
            ps.setInt(1, i);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String summary = rs.getString(1);
                SimHash.simHash(summary);
            }
            if (i % 5000 == 0) {
                logger.info("handle " + i);
            }
            rs.close();
        }
    }

    @Test
    public void testWithOutLuence() throws IOException {
        for (int i = 1; i < TOTAL; i++) {
            SimHash.simHash(i);
        }
    }

    @Test
    public void testSimHash() {
        long h1 = SimHash.simHash("the cat sat on a mat");
        long h2 = SimHash.simHash("the cat sat on the mat");
        long h3 = SimHash.simHash("we all scream for ice cream");
        System.out.println(SimHash.hammingDistance(h1, h2));
        System.out.println(SimHash.hammingDistance(h1, h3));
        System.out.println(SimHash.hammingDistance(h2, h3));
    }
}
