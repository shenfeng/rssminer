/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db.perf;

import org.junit.Before;
import org.junit.Test;

import java.sql.*;

public class MysqlFeedSortTest extends AbstractPerfTest {

    Connection con;

    @Before
    public void setup() throws SQLException {
        con = DriverManager.getConnection("jdbc:mysql://localhost/rssminer",
                "feng", "");
    }

    // used_memory_human:241.37M => 8021364

    // ///////////////

    // used_memory_human:486.77M, 411916
    // insert 1245991 => 59s
    @Test
    public void insertTestData() throws SQLException {
        long totalFeedCount = 0;
        PreparedStatement ps = con
                .prepareStatement("insert into user_feed (user_id, feed_id, rss_link_id, vote_sys) values (?, ?, ?, ?)");
        con.setAutoCommit(false);
        for (int userid = USER_ID_START; userid < USER_ID_END; userid++) {
            int subcount = getSubsPerUser();
            int start = getFeeIDStart();
            for (int subid = SUB_ID_START; subid < subcount + SUB_ID_START; subid++) {
                int feedCount = getPerSubFeedCount();
                totalFeedCount += feedCount;
                for (int feedid = start; feedid < feedCount + start; feedid++) {
                    ps.setInt(1, userid);
                    ps.setInt(2, feedid);
                    ps.setInt(3, subid);
                    ps.setDouble(4, getScore());
                    ps.addBatch();
                }
                start += feedCount;
                ps.executeBatch();
                con.commit();
            }
        }

        System.out.println(totalFeedCount);
    }

    // 2.250s
    @Test
    public void testTotalPerf() throws SQLException {
        PreparedStatement ps = con
                .prepareStatement("select feed_id, vote_sys from user_feed where user_id = ? order by vote_sys desc limit ? offset ?");
        for (int userid = USER_ID_START; userid < USER_ID_START + NUM_TEST; userid++) {
            ps.setInt(1, userid);
            ps.setInt(2, 30);
            ps.setInt(3, 0);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                count++;
            }
            // System.out.println(count);
            rs.close();
        }
    }

    // 1.34s
    @Test
    public void testFewSubsPerf() throws Exception {
        Statement statement = con.createStatement();
        for (int userid = USER_ID_START; userid < USER_ID_END + NUM_TEST; userid++) {
            int[] ids = randSubIds();
            StringBuilder sb = new StringBuilder(
                    "select feed_id, vote_sys from user_feed where user_id = "
                            + userid + " and rss_link_id in (");
            for (int id : ids) {
                sb.append(id);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append(") order by vote_sys desc limit 20");
            ResultSet rs = statement.executeQuery(sb.toString());
            int count = 0;
            while (rs.next()) {
                count++;
            }
            // System.out.println(count);
            rs.close();
        }
    }

    // 0.64s
    @Test
    public void testPerSubPerf() throws Exception {
        PreparedStatement ps = con
                .prepareStatement("select feed_id, vote_sys from user_feed where user_id = ? and rss_link_id = ? order by vote_sys desc limit ? offset ?");

        for (int userid = USER_ID_START; userid < USER_ID_START + NUM_TEST; userid++) {
            int rssLinkID = random.nextInt(NUM_SUBS_PER_USER) + SUB_ID_START;
            ps.setInt(1, userid);
            ps.setInt(2, rssLinkID);
            ps.setInt(3, 20);
            ps.setInt(4, 0);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                int feedid = rs.getInt("feed_id");
                double vote = rs.getDouble("vote_sys");
                // System.out.println(feedid + "\t" + vote);
                count++;
            }
            // System.out.println(count);
            rs.close();
        }
    }

    @Override
    public void testCountPerf() throws Exception {
        // TODO Auto-generated method stub

    }
}
