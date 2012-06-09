package rssminer.db;

import static rssminer.Utils.K_DATA_SOURCE;
import static rssminer.Utils.K_REDIS_SERVER;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import me.shenfeng.http.HttpUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import rssminer.Utils;
import clojure.lang.Keyword;

public class MinerDAO {
    private JedisPool jedis;
    private DataSource ds;

    static final String SELECT_FIELDS = "SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
            + "f.published_ts FROM feeds f";

    // sub newest, oldest
    static final String WITH_SCORE = "SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
            + "f.published_ts,uf.read_date,uf.vote_user FROM feeds "
            + "f LEFT JOIN user_feed uf ON uf.feed_id = f.id and uf.user_id =";

    public MinerDAO(Map<Keyword, Object> config) {
        this.jedis = (JedisPool) config.get(K_REDIS_SERVER);
        this.ds = (DataSource) config.get(K_DATA_SOURCE);
        if (this.jedis == null || this.ds == null) {
            throw new NullPointerException("jedis and ds can not be null");
        }
    }

    private void addScore(int userID, List<Feed> feeds) {
        Jedis redis = jedis.getResource();
        try {
            Pipeline pipeline = redis.pipelined();
            List<Response<Double>> scores = new ArrayList<Response<Double>>(
                    feeds.size());
            for (Feed f : feeds) {
                byte[] member = Integer.toString(f.getId()).getBytes(
                        HttpUtils.UTF_8);
                Response<Double> score = pipeline.zscore(
                        Utils.genKey(userID, f.getRssid()), member);
                scores.add(score);
            }
            pipeline.sync();
            for (int i = 0; i < feeds.size(); i++) {
                try {
                    feeds.get(i).setScore(scores.get(i).get());
                } catch (Exception ignore) {
                    // TODO, should return nil when no key instead of EXCEPTION
                }
            }
        } finally {
            jedis.returnResource(redis);
        }
    }

    public List<Feed> fetchFeeds(List<Integer> feedids) throws SQLException {
        StringBuilder sb = new StringBuilder(SELECT_FIELDS.length()
                + feedids.size() * 8);
        sb.append(SELECT_FIELDS);
        sb.append(" where id in(");
        for (int id : feedids) {
            sb.append(id).append(',');
        }
        sb.setLength(sb.length() - 1); // remove last ,
        sb.append(')');
        return fetchFeeds(sb.toString());
    }

    private List<Feed> fetchFeeds(String sql) throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            List<Feed> feeds = new ArrayList<Feed>(30);
            ResultSet rs = stat.executeQuery(sql);
            int count = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Feed f = new Feed();
                f.setId(rs.getInt(1));
                f.setRssid(rs.getInt(2));
                f.setTitle(rs.getString(3));
                f.setAuthor(rs.getString(4));
                f.setLink(rs.getString(5));
                f.setTags(rs.getString(6));
                f.setPublishedts(rs.getInt(7));
                if (count > 7) { // with read ts and vote_user
                    f.setReadts(rs.getInt(8));
                    f.setVote(rs.getInt(9));
                }
                feeds.add(f);
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return feeds;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public List<Feed> fetchFeedsWithScore(int userID, List<Integer> feedids)
            throws SQLException {
        List<Feed> feeds = fetchFeeds(feedids);
        // sort by search result by lucene score
        List<Feed> result = new ArrayList<Feed>(feeds.size());
        for (Integer id : feedids) {
            for (Feed f : feeds) {
                if (id.equals(f.getId())) {
                    result.add(f);
                    break;
                }
            }
        }
        addScore(userID, result);
        return result;
    }

    private List<Feed> fetchFeedsWithScore(int userID, String sql)
            throws SQLException {
        List<Feed> feeds = fetchFeeds(sql);
        addScore(userID, feeds);
        return feeds;
    }

    private List<Feed> fetchFeedsWithScore(Set<Tuple> scores)
            throws SQLException {
        Map<Integer, Double> map = new HashMap<Integer, Double>(
                (int) (scores.size() * 1.5));
        for (Tuple tuple : scores) {
            int id = Integer.valueOf(new String(tuple.getBinaryElement(),
                    HttpUtils.UTF_8));
            map.put(id, tuple.getScore());
        }
        if (map.isEmpty()) {
            return new ArrayList<Feed>(0);
        } else {
            List<Feed> feeds = fetchFeeds(new ArrayList<Integer>(map.keySet()));
            for (Feed feed : feeds) {
                feed.setScore(map.get(feed.getId()));
            }
            Collections.sort(feeds); // sort by score
            return feeds;
        }
    }

    // global
    public List<Feed> fetchGLikest(int userID, int limit, int offset)
            throws SQLException {
        byte[] key = Utils.genKey(userID);
        Jedis redis = jedis.getResource();
        try {
            if (!redis.exists(key)) {
                List<Integer> subIDS = DBHelper.getUserSubIDS(ds, userID);
                int count = subIDS.size();
                byte[][] keys = new byte[count][];
                for (int i = 0; i < count; i++) {
                    keys[i] = Utils.genKey(userID, subIDS.get(i));
                }
                redis.zunionstore(key, keys);
                redis.expire(key, 3600);
            }
            Set<Tuple> scores = redis.zrevrangeWithScores(key, offset, offset
                    + limit - 1);
            return fetchFeedsWithScore(scores);
        } finally {
            jedis.returnResource(redis);
        }
    }

    // global
    public List<Feed> fetchGNewest(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append(SELECT_FIELDS);
        sb.append(" JOIN user_subscription us ON f.rss_link_id = us.rss_link_id");
        sb.append(" where us.user_id = ");
        sb.append(userID);
        sb.append(" order by f.published_ts desc ");
        sb.append("limit ").append(limit);
        sb.append(" offset ");
        sb.append(offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchGRead(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append("SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
                + "uf.read_date FROM feeds f");
        sb.append(" join user_feed uf on uf.feed_id = f.id");
        sb.append(" where uf.user_id = ").append(userID);
        sb.append(" and uf.read_date > 0 order by uf.read_date desc");
        sb.append(" limit ").append(limit);
        sb.append(" offset ").append(offset);
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            ResultSet rs = stat.executeQuery(sb.toString());
            List<Feed> feeds = new ArrayList<Feed>(limit);
            while (rs.next()) {
                Feed f = new Feed();
                f.setId(rs.getInt(1));
                f.setRssid(rs.getInt(2));
                f.setTitle(rs.getString(3));
                f.setAuthor(rs.getString(4));
                f.setLink(rs.getString(5));
                f.setTags(rs.getString(6));
                f.setReadts(rs.getInt(7));
                feeds.add(f);
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return feeds;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public List<Feed> fetchGVote(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append("SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
                + "uf.vote_date,uf.vote_user FROM feeds f");
        sb.append(" join user_feed uf on uf.feed_id = f.id");
        sb.append(" where uf.user_id = ").append(userID);
        sb.append(" and uf.vote_date > 0 order by uf.vote_date desc");
        sb.append(" limit ").append(limit);
        sb.append(" offset ").append(offset);

        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            ResultSet rs = stat.executeQuery(sb.toString());
            List<Feed> feeds = new ArrayList<Feed>(limit);
            while (rs.next()) {
                Feed f = new Feed();
                f.setId(rs.getInt(1));
                f.setRssid(rs.getInt(2));
                f.setTitle(rs.getString(3));
                f.setAuthor(rs.getString(4));
                f.setLink(rs.getString(5));
                f.setTags(rs.getString(6));
                f.setVotets(rs.getInt(7));
                f.setVote(rs.getInt(8));
                feeds.add(f);
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return feeds;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public List<Feed> fetchSubLikest(int userID, int subID, int limit,
            int offset) throws SQLException {
        byte[] key = Utils.genKey(userID, subID);
        Jedis redis = jedis.getResource();
        try {
            Set<Tuple> scores = redis.zrevrangeWithScores(key, offset, offset
                    + limit - 1);
            return fetchFeedsWithScore(scores);
        } finally {
            jedis.returnResource(redis);
        }
    }

    public List<Feed> fetchSubNewest(int userID, int subID, int limit,
            int offset) throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append(WITH_SCORE).append(userID);
        sb.append(" WHERE f.rss_link_id = ").append(subID);
        sb.append(" order by published_ts desc ");
        sb.append(" limit ").append(limit);
        sb.append(" offset ");
        sb.append(offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchSubOldest(int userID, int subID, int limit,
            int offset) throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append(WITH_SCORE).append(userID);
        sb.append(" WHERE f.rss_link_id = ").append(subID);
        sb.append(" order by published_ts ");
        sb.append(" limit ").append(limit);
        sb.append(" offset ");
        sb.append(offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Subscription> fetchUserSubs(int userID) throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();

            ResultSet rs = stat
                    .executeQuery("SELECT rss_link_id AS id, l.title, group_name, sort_index,"
                            + "l.alternate as url, l.total_feeds FROM user_subscription u join rss_links l "
                            + "ON l.id = u.rss_link_id WHERE u.user_id = "
                            + userID);
            List<Subscription> subs = new ArrayList<Subscription>();
            while (rs.next()) {
                Subscription s = new Subscription();
                s.setId(rs.getInt(1));
                s.setTitle(rs.getString(2));
                s.setGroup(rs.getString(3));
                s.setIndex(rs.getInt(4));
                s.setUrl(rs.getString(5));
                s.setTotal(rs.getInt(6));
                subs.add(s);
            }
            String sql = "select like_score,neutral_score from users where id = "
                    + userID;
            Utils.closeQuietly(rs);
            rs = stat.executeQuery(sql);
            if (rs.next()) {
                double like = rs.getDouble(1);
                double neutral = rs.getDouble(2);
                Jedis redis = jedis.getResource();
                Pipeline pipeline = redis.pipelined();
                List<Response<Long>> scores = new ArrayList<Response<Long>>(
                        subs.size() * 2);
                try {
                    for (Subscription s : subs) {
                        byte[] key = Utils.genKey(userID, s.getId());
                        Response<Long> l = pipeline.zcount(key, like,
                                Double.MAX_VALUE);
                        Response<Long> n = pipeline
                                .zcount(key, neutral, like);
                        scores.add(l);
                        scores.add(n);
                    }
                    pipeline.sync();
                    int idx = 0;
                    for (Subscription s : subs) {
                        s.setLike(scores.get(idx++).get().intValue());
                        s.setNeutral(scores.get(idx++).get().intValue());
                    }
                } finally {
                    jedis.returnResource(redis);
                }
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return subs;
        } finally {
            Utils.closeQuietly(con);
        }
    }
}
