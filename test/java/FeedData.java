/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

public class FeedData {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    final static int STEP = 1000;
    final static int REPORT = STEP * 10;

    public static void main(String[] args) throws SQLException {
        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");

        Statement stat = con.createStatement();

        ResultSet rs = stat.executeQuery("select id, rss_link_id from feeds order by id");

        ArrayList<Integer> data = new ArrayList<Integer>(1000000);

        while (rs.next()) {
            data.add(rs.getInt(1));
        }
        ResultSet ids = stat.executeQuery("select id from feed_data order by id");

        // ArrayList<Integer> feeds = new ArrayList<Integer>(1000000);

        while (ids.next()) {
            int id = ids.getInt(1);

            int idx = Collections.binarySearch(data, id);
            if (idx < 0) {
                System.out.println(id);
                // System.out.println(id + "\t" + ids.getInt(2));
            }

        }

    }
}
