package rssminer.classfier;

import static rssminer.classfier.NaiveBayes.classify;
import static rssminer.classfier.NaiveBayes.train;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSysVote {

    static Logger logger = LoggerFactory.getLogger(UserSysVote.class);

    private long sinceTime;
    private int userID;
    private Connection con;
    private DataSource ds;

    public UserSysVote(int userID, long sinceTime, DataSource ds)
            throws SQLException {
        this.userID = userID;
        this.sinceTime = sinceTime;
        this.ds = ds;

    }

    private List<Integer> fetchUpIDs(Statement stat) throws SQLException {
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND vote_user = 1 order by read_date desc limit 100",
                        userID);
        List<Integer> ups = new ArrayList<Integer>();

        ResultSet rs = stat.executeQuery(sql);
        while (rs.next()) {
            ups.add(rs.getInt(1));
        }
        rs.close();
        return ups;
    }

    private List<Integer> fetchDownIDs(Statement stat) throws SQLException {
        List<Integer> downs = new ArrayList<Integer>();
        String sql = String
                .format("SELECT feed_id FROM user_feed WHERE user_id = %d AND vote_user = -1 order by read_date desc limit 100",
                        userID);
        ResultSet rs = stat.executeQuery(sql);
        while (rs.next()) {
            downs.add(rs.getInt(1));
        }
        rs.close();
        return downs;
    }

    private List<Integer> getUnvotedIDs(Statement stat) throws SQLException {
        String sql = String.format("call get_unvoted_feedids(%d, %d)",
                userID, sinceTime);
        List<Integer> unVoted = new ArrayList<Integer>();
        ResultSet rs = stat.executeQuery(sql);
        while (rs.next()) {
            unVoted.add(rs.getInt(1));
        }
        rs.close();
        return unVoted;
    }

    private double[] pick(double[] prefs, double likeRatio,
            double dislikeRatio) {
        int likeIndex = prefs.length - (int) (prefs.length * likeRatio);
        int disLikeIndex = (int) (prefs.length * dislikeRatio);
        likeIndex = likeIndex == prefs.length ? prefs.length - 1 : likeIndex;
        Arrays.sort(prefs);
        return new double[] { prefs[likeIndex], prefs[disLikeIndex] };
    }

    public double[] reCompute() throws SQLException, CorruptIndexException,
            IOException {
        double[] ret = null;
        con = ds.getConnection();
        con.setAutoCommit(false);
        Statement stat = con.createStatement();
        List<Integer> downs = fetchDownIDs(stat);
        List<Integer> ups = fetchUpIDs(stat);
        if (ups.size() > 0 && downs.size() > 0) {
            long start = System.currentTimeMillis();
            List<Integer> unVoted = getUnvotedIDs(stat);
            Map<String, Map<String, Double>> model = train(ups, downs);
            double[] results = classify(model, unVoted);
            PreparedStatement ps = con
                    .prepareStatement("insert into user_feed (user_id, feed_id, vote_sys) values (?, ?, ?) on duplicate key update vote_sys = ?;");
            for (int i = 0; i < results.length; i++) {
                int feedID = unVoted.get(i);
                double score = results[i];
                ps.setInt(1, userID);
                ps.setInt(2, feedID);
                ps.setDouble(3, score);
                ps.setDouble(4, score);
                ps.addBatch();
            }
            ps.executeBatch(); // ignore result
            con.commit();
            // 30% => like, 20% dislike, 50% neutual
            ret = pick(results, 0.3, 0.2);
            long time = System.currentTimeMillis() - start;
            logger.info(
                    "for user {}, downs: {}, ups: {}, unVoted: {}, take time: {}ms",
                    new Object[] { userID, downs.size(), ups.size(),
                            unVoted.size(), time });
        }
        stat.close();
        con.close();
        return ret;
    }
}
