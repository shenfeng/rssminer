/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db;

import clojure.lang.Keyword;
import me.shenfeng.http.HttpUtils;
import redis.clients.jedis.*;
import rssminer.Utils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.*;
import java.util.*;

import static rssminer.Utils.K_DATA_SOURCE;
import static rssminer.Utils.K_REDIS_SERVER;

public class MinerDAO {
    static final int COMBINED_KEY_EXPIRE = 1800; // cache half an hour

    private final JedisPool jedis;
    private final DataSource ds;

    // sub newest, oldest
    static final String FEED_FIELD = "SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
            + "f.published_ts,uf.read_date,uf.vote_user, uf.vote_date FROM feeds "
            + "f LEFT JOIN user_feed uf ON uf.feed_id = f.id and uf.user_id =";

    static final String FETCH_FEED = "SELECT f.id,f.rss_link_id,f.title,f.author,f.link,tags,"
            + "f.published_ts,uf.read_date,uf.vote_user,uf.vote_date,d.summary FROM feeds "
            + "f LEFT JOIN user_feed uf ON uf.feed_id = f.id and uf.user_id = ";

    // + "join feed_data d on d.id = f.id and f.id ";

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
                Double d = scores.get(i).get();
                if (d != null) {
                    feeds.get(i).setScore(d);
                }
            }
        } finally {
            jedis.returnResource(redis);
        }
    }

    private void appendIn(StringBuilder sb, List<Integer> ids) {
        sb.append('(');
        for (int id : ids) {
            sb.append(id).append(',');
        }
        sb.setLength(sb.length() - 1); // remove last ','
        sb.append(')');
    }

    private void appendLimitOffset(StringBuilder sb, int limit, int offset) {
        sb.append("limit ").append(offset).append(',').append(limit);
    }

    private StringBuilder createBuilder(List<Integer> ids) {
        if (ids == null) {
            return new StringBuilder(245);
        } else {
            return new StringBuilder(245 + ids.size() * 5);
        }
    }

    private List<Feed> fetchFeeds(int userID, List<Integer> feedids)
            throws SQLException {
        StringBuilder sb = createBuilder(feedids);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where id in");
        appendIn(sb, feedids);
        return fetchFeeds(sb.toString());
    }

    private Feed createFeed(ResultSet rs, boolean withSummary)
            throws SQLException {
        Feed f = new Feed();
        f.setId(rs.getInt(1));
        f.setRssid(rs.getInt(2));
        f.setTitle(rs.getString(3));
        f.setAuthor(rs.getString(4));
        f.setLink(rs.getString(5));
        f.setTags(rs.getString(6));
        f.setPublishedts(rs.getInt(7));
        f.setReadts(rs.getInt(8));
        f.setVote(rs.getInt(9));
        f.setVotets(rs.getInt(10));
        if (withSummary) {
            f.setSummary(rs.getString(11));
        }
        return f;
    }

    private List<Feed> fetchFeeds(String sql) throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            List<Feed> feeds = new ArrayList<Feed>(20);
            ResultSet rs = stat.executeQuery(sql);
            boolean withsummary = rs.getMetaData().getColumnCount() == 11;
            while (rs.next()) {
                feeds.add(createFeed(rs, withsummary));
            }
            Utils.closeQuietly(rs);
            Utils.closeQuietly(stat);
            return feeds;
        } finally {
            Utils.closeQuietly(con);
        }
    }

    public List<Feed> sortbyOrder(List<Integer> feedids, List<Feed> feeds) {
        List<Feed> result = new ArrayList<Feed>(feeds.size());
        for (Integer id : feedids) {
            for (Feed f : feeds) {
                if (id.equals(f.getId())) {
                    result.add(f);
                    break;
                }
            }
        }
        return result;
    }

    public List<Feed> fetchFeedsWithScore(int userID, List<Integer> feedids)
            throws SQLException {
        List<Feed> feeds = sortbyOrder(feedids, fetchFeeds(userID, feedids));
        addScore(userID, feeds);
        return feeds;
    }

    private List<Feed> fetchFeedsWithScore(int userID, Set<Tuple> scores)
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
            List<Feed> feeds = fetchFeeds(userID,
                    new ArrayList<Integer>(map.keySet()));
            for (Feed feed : feeds) {
                feed.setScore(map.get(feed.getId()));
            }
            Collections.sort(feeds); // sort by score
            return feeds;
        }
    }

    public static List<Feed> removeDuplicate(List<Feed> feeds) {
        List<Feed> results = new ArrayList<Feed>(feeds.size());
        for (Feed f : feeds) {
            boolean c = false;
            for (Feed e : results) {
                if (e.getTitle().equals(f.getTitle())
                        || e.getLink().equals(f.getLink())) {
                    c = true;
                    break;
                }
            }
            if (!c) {
                results.add(f);
            }
        }
        return results;
    }

    private List<Feed> fetchFeedsWithScore(int userID, String sql)
            throws SQLException {
        List<Feed> feeds = fetchFeeds(sql);
        addScore(userID, feeds);
        return feeds;
    }

    // for click on folder
    public List<Feed> fetchFolderLikest(int userID, List<Integer> rssIDs,
                                        int limit, int offset) throws SQLException {
        Jedis redis = jedis.getResource();
        rssIDs = new ArrayList<Integer>(rssIDs);
        byte[] key = Utils.genKey(userID, rssIDs);
        try {
            if (!redis.exists(key)) {
                int count = rssIDs.size();
                byte[][] keys = new byte[count][];
                for (int i = 0; i < count; i++) {
                    keys[i] = Utils.genKey(userID, rssIDs.get(i));
                }
                redis.zunionstore(key, keys);
                redis.expire(key, COMBINED_KEY_EXPIRE);
            }
            Set<Tuple> scores = redis.zrevrangeWithScores(key, offset, offset
                    + limit - 1);
            return fetchFeedsWithScore(userID, scores);
        } finally {
            jedis.returnResource(redis);
        }
    }

    public List<Feed> fetchFolderNewest(int userID, List<Integer> rssIDs,
                                        int limit, int offset) throws SQLException {
        StringBuilder sb = createBuilder(rssIDs);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" WHERE f.rss_link_id in ");
        appendIn(sb, rssIDs);
        sb.append(" order by published_ts desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    // for folder
    public List<Feed> fetchFolderOldest(int userID, List<Integer> rssIDs,
                                        int limit, int offset) throws SQLException {
        StringBuilder sb = createBuilder(rssIDs);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" WHERE f.rss_link_id in ");
        appendIn(sb, rssIDs);
        sb.append(" order by published_ts ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchFolderRead(int userID, List<Integer> rssIDs,
                                      int limit, int offset) throws SQLException {
        StringBuilder sb = createBuilder(rssIDs);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.read_date > 0 and uf.rss_link_id in ");
        appendIn(sb, rssIDs);
        sb.append(" order by uf.read_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    public List<Feed> fetchFolderVote(int userID, List<Integer> rssIDs,
                                      int limit, int offset) throws SQLException {
        StringBuilder sb = createBuilder(rssIDs);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.vote_date > 0 and uf.vote_user != 0 and uf.rss_link_id in ");
        appendIn(sb, rssIDs);
        sb.append(" order by uf.vote_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    // global
    public List<Feed> fetchGLikest(int userID, int limit, int offset)
            throws SQLException {
        byte[] key = Utils.genKey(userID);
        Jedis redis = jedis.getResource();
        try {
            List<Integer> subIDS = DBHelper.getUserSubIDS(ds, userID);
            int count = subIDS.size();
            if (count < 1) {
                return new ArrayList<Feed>(0);
            }
            byte[][] keys = new byte[count][];
            for (int i = 0; i < count; i++) {
                keys[i] = Utils.genKey(userID, subIDS.get(i));
            }
            redis.zunionstore(key, keys);
            Set<Tuple> scores = redis.zrevrangeWithScores(key, offset, offset
                    + limit - 1);
            redis.del(key);
            return fetchFeedsWithScore(userID, scores);
        } finally {
            jedis.returnResource(redis);
        }
    }

    // global
    public List<Feed> fetchGNewest(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" JOIN user_subscription us ON f.rss_link_id = us.rss_link_id");
        sb.append(" where us.user_id = ");
        sb.append(userID).append(" and f.published_ts >");
        // only recent 60 days
        sb.append((int) (System.currentTimeMillis() / 1000) - 3600 * 24 * 60);
        sb.append(" order by f.published_ts desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchGRead(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.read_date > 0 order by uf.read_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    public List<Feed> fetchGVote(int userID, int limit, int offset)
            throws SQLException {
        StringBuilder sb = new StringBuilder(240);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.vote_date > 0 and uf.vote_user != 0 order by uf.vote_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    public List<Feed> fetchSubLikest(int userID, int subID, int limit,
                                     int offset) throws SQLException {
        byte[] key = Utils.genKey(userID, subID);
        Jedis redis = jedis.getResource();
        try {
            Set<Tuple> scores = redis.zrevrangeWithScores(key, offset, offset
                    + limit - 1);
            return fetchFeedsWithScore(userID, scores);
        } finally {
            jedis.returnResource(redis);
        }
    }

    public List<Feed> fetchSubNewest(int userID, int subID, int limit,
                                     int offset) throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" WHERE f.rss_link_id = ").append(subID);
        sb.append(" order by published_ts desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchSubOldest(int userID, int subID, int limit,
                                     int offset) throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" WHERE f.rss_link_id = ").append(subID);
        sb.append(" order by published_ts ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeedsWithScore(userID, sb.toString());
    }

    public List<Feed> fetchSubRead(int userID, int subID, int limit,
                                   int offset) throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.read_date > 0 and uf.rss_link_id = ");
        sb.append(subID);
        sb.append(" order by uf.read_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    private List<Subscription> fetchSubs(String select, int userID)
            throws SQLException {
        Connection con = ds.getConnection();
        try {
            Statement stat = con.createStatement();
            ResultSet rs = stat.executeQuery(select);
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
            Utils.closeQuietly(rs);
            String sql = "select like_score,neutral_score from users where id = "
                    + userID;
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

    public List<Feed> fetchFeedsWithSummary(int userID, List<Integer> feedIDs)
            throws SQLException {
        StringBuilder sb = new StringBuilder(FETCH_FEED.length() + 80);
        sb.append(FETCH_FEED);
        sb.append(userID);
        sb.append(" join feed_data d on d.id = f.id and f.id in ");
        appendIn(sb, feedIDs);

        return sortbyOrder(feedIDs,
                fetchFeedsWithScore(userID, sb.toString()));
    }

    public List<Feed> fetchSubVote(int userID, int subID, int limit,
                                   int offset) throws SQLException {
        StringBuilder sb = createBuilder(null);
        sb.append(FEED_FIELD).append(userID);
        sb.append(" where uf.vote_date > 0 and uf.vote_user != 0 and uf.rss_link_id = ");
        sb.append(subID);
        sb.append(" order by uf.vote_date desc ");
        appendLimitOffset(sb, limit, offset);
        return fetchFeeds(sb.toString());
    }

    static final String SUB_FIELDS = "SELECT rss_link_id AS id, l.title, group_name, sort_index,"
            + "COALESCE(l.alternate, l.url) as url, l.total_feeds FROM user_subscription u "
            + "join rss_links l ON l.id = u.rss_link_id WHERE";

    public Subscription fetchUserSub(int userid, int rssid)
            throws SQLException {
        StringBuilder sb = new StringBuilder(SUB_FIELDS.length() + 20);
        sb.append(SUB_FIELDS);
        sb.append(" u.rss_link_id = ").append(rssid);
        List<Subscription> subs = fetchSubs(sb.toString(), userid);
        if (subs.isEmpty()) {
            return null;
        }
        return subs.get(0);
    }

    public List<Subscription> fetchUserSubs(int userID) throws SQLException {
        StringBuilder sb = new StringBuilder(SUB_FIELDS.length() + 20);
        sb.append(SUB_FIELDS);
        sb.append(" u.user_id = ").append(userID);
        return fetchSubs(sb.toString(), userID);
    }
}
