/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db;

import rssminer.Utils;
import rssminer.classfier.FeedScore;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBHelper {
    public static List<Integer> fetchUserIDsBySubID(DataSource ds, int subid)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            String sql = "select user_id from user_subscription where rss_link_id = "
                    + subid;
            Statement stat = con.createStatement();
            List<Integer> ids = getIDS(stat, sql);
            Utils.closeQuietly(stat);
            return ids;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public static List<Vote> fetchVotedIds(DataSource ds, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            CallableStatement stat = con.prepareCall("call get_voted(?)");
            stat.setInt(1, userID);
            ResultSet rs = stat.executeQuery();
            List<Vote> votes = new ArrayList<Vote>(100);
            while (rs.next()) {
                votes.add(new Vote(rs.getInt(1), rs.getInt(2)));
            }
            Utils.closeQuietly(stat);
            Utils.closeQuietly(rs);
            return votes;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    private static List<Integer> getIDS(Statement stat, String sql)
            throws SQLException {
        ResultSet rs = stat.executeQuery(sql);
        List<Integer> ids = new ArrayList<Integer>();
        while (rs.next()) {
            ids.add(rs.getInt(1));
        }
        Utils.closeQuietly(rs);
        return ids;
    }

    public static List<FeedScore> getUnvotedFeeds(DataSource ds, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            CallableStatement call = con.prepareCall("call get_unvoted(?)");
            call.setInt(1, userID);
            List<FeedScore> unVoted = new ArrayList<FeedScore>(2500);
            ResultSet rs = call.executeQuery();
            while (rs.next()) {
                unVoted.add(new FeedScore(rs.getInt(1), rs.getInt(2), rs
                        .getInt(3)));
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(call);
            return unVoted;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public static List<Integer> getUserSubIDS(DataSource ds, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            String sql = "select rss_link_id from user_subscription where user_id = "
                    + userID;
            List<Integer> ids = getIDS(stat, sql);
            Utils.closeQuietly(stat);
            return ids;
        } finally {
            Utils.closeQuietly(con);
        }
    }
}
