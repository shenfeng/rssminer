package rssminer.db;

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
    public static List<Integer> fetchRecentRead(Statement stat, int userID)
            throws SQLException {
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND read_date > 0 order by read_date desc limit 100",
                        userID);
        return getIDS(stat, sql);
    }

    public static List<Integer> fetchUserIDsBySubID(DataSource ds, int subid)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            String sql = "select user_id, vote from user_subscription where rss_link_id = "
                    + subid;
            Statement stat = con.createStatement();
            List<Integer> ids = getIDS(stat, sql);
            Utils.closeQuietly(stat);
            return ids;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Integer>[] fetchVotedIds(DataSource ds, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            String sql = "select feed_id, vote_user from user_feed where vote_user != 0 and user_id = "
                    + userID + " order by vote_date desc limit 100";
            ResultSet rs = stat.executeQuery(sql);
            ArrayList<Integer> ups = new ArrayList<Integer>();
            ArrayList<Integer> downs = new ArrayList<Integer>();
            while (rs.next()) {
                if (rs.getInt(2) > 0) {
                    ups.add(rs.getInt(1));
                } else {
                    downs.add(rs.getInt(1));
                }
            }
            return new ArrayList[] { ups, downs };
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
            Statement stat = con.createStatement();
            String sql = "select f.id, f.rss_link_id from feeds f join"
                    + " user_subscription us on f.rss_link_id = us.rss_link_id"
                    + " and us.user_id =" + userID
                    + " order by published_ts desc limit 4000";
            List<FeedScore> unVoted = new ArrayList<FeedScore>(1024);
            ResultSet rs = stat.executeQuery(sql);
            while (rs.next()) {
                unVoted.add(new FeedScore(rs.getInt(1), rs.getInt(2)));
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
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
