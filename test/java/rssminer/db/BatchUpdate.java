/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db;

import org.junit.Test;

import java.sql.*;
import java.util.*;

class UserFeed {
    public int userID;
    public int feedID;

    public UserFeed(int userID, int feedID) {
        this.userID = userID;
        this.feedID = feedID;
    }
}

public class BatchUpdate {

    @Test
    public void testBatch() throws SQLException {
        Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost/rssminer", "feng", "");

        con.setAutoCommit(false);
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery("select * from user_feed limit 1000");
        List<UserFeed> userfeeds = new ArrayList<UserFeed>();
        while (rs.next()) {
            int userID = rs.getInt("user_id");
            int feedID = rs.getInt("feed_id");
            userfeeds.add(new UserFeed(userID, feedID));
        }

        PreparedStatement ps = con
                .prepareStatement("insert into user_feed (user_id, feed_id, vote_sys) values (?, ?, ?) on duplicate key update vote_user = ?;");

        Random r = new Random();

        for (UserFeed uf : userfeeds) {
            ps.setInt(1, uf.userID);
            ps.setInt(2, uf.feedID);
            float f = r.nextFloat();
            ps.setFloat(3, f);
            ps.setFloat(4, f);
            ps.addBatch();
        }

        int[] results = ps.executeBatch();
        con.commit(); // 0.346s vs 32.324s(auto commit)

        System.out.println(results.length + "\t" + Arrays.toString(results));

    }
}
