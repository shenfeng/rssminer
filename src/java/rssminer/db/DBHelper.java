package rssminer.db;

import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import rssminer.Utils;
import rssminer.classfier.FeedScore;

public class DBHelper {

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

    public static List<Integer> fetchDownIDs(Statement stat, int userID)
            throws SQLException {
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND vote_user = -1 order by vote_date desc limit 100",
                        userID);
        return getIDS(stat, sql);
    }

    public static List<Integer> fetchRecentRead(Statement stat, int userID)
            throws SQLException {
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND read_date > 0 order by read_date desc limit 100",
                        userID);
        return getIDS(stat, sql);
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

    public static List<Integer> fetchUpIDs(Statement stat, int userID)
            throws SQLException {
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND vote_user = 1 order by vote_date desc limit 100",
                        userID);
        return getIDS(stat, sql);
    }

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

    public static List<FeedScore> getUnvotedFeeds(DataSource ds, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            long start = currentTimeMillis();
            int ts = (int) (start / 1000) - 3600 * 24 * 30;
            Statement stat = con.createStatement();
            String sql = String
                    .format("call get_unvoted(%d, %d)", userID, ts);
            List<FeedScore> unVoted = new ArrayList<FeedScore>();
            ResultSet rs = stat.executeQuery(sql);
            while (rs.next()) {
                unVoted.add(new FeedScore(rs.getInt("id"), rs
                        .getInt("rss_link_id")));
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return unVoted;
        } finally {
            Utils.closeQuietly(con);
        }
    }
}
