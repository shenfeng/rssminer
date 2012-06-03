package rssminer.classfier;

import static rssminer.Utils.K_DATA_SOURCE;
import static rssminer.Utils.K_EVENTS_THRESHOLD;
import static rssminer.Utils.K_REDIS_SERVER;
import static rssminer.classfier.NaiveBayes.classify;
import static rssminer.classfier.NaiveBayes.train;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sql.DataSource;

import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import rssminer.Utils;
import rssminer.db.DBHelper;
import clojure.lang.Keyword;

public class SysVoteDaemon implements Runnable {

    public static final int expireSeconds = 3600 * 24 * 10; // 10 days
    public static final double likeRatio = 0.2;
    public static final double dislikeRatio = 0.3;
    private static final Logger logger = LoggerFactory
            .getLogger(SysVoteDaemon.class);

    public final int eventsThreashold;
    private final DataSource ds;
    private JedisPool jedis;
    private volatile boolean running = false;
    private LinkedBlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();
    private Thread deamonThread;

    static final Map<String, Map<String, Double>> noModel = new TreeMap<String, Map<String, Double>>();
    private Map<Integer, Integer> combineEvents = new TreeMap<Integer, Integer>();
    private Map<Integer, Map<String, Map<String, Double>>> modelCache = new TreeMap<Integer, Map<String, Map<String, Double>>>();

    public SysVoteDaemon(Map<Keyword, Object> config) {
        this.jedis = (JedisPool) config.get(K_REDIS_SERVER);
        this.ds = (DataSource) config.get(K_DATA_SOURCE);
        this.eventsThreashold = (Integer) config.get(K_EVENTS_THRESHOLD);
        if (this.jedis == null || this.ds == null) {
            throw new NullPointerException("jedis and ds can not be null");
        }
    }

    private void computeAddSaveScore(int userID) throws SQLException,
            CorruptIndexException, IOException {
        long start = System.currentTimeMillis();
        Map<String, Map<String, Double>> model = trainModel(userID);
        if (model != null) {
            List<FeedScore> unVoted = DBHelper.getUnvotedFeeds(ds, userID);
            if (!unVoted.isEmpty()) {
                List<Integer> unVotedIDs = new ArrayList<Integer>(
                        unVoted.size());
                for (FeedScore feed : unVoted) {
                    unVotedIDs.add(feed.feedID);
                }
                double[] results = classify(model, unVotedIDs);
                for (int i = 0; i < results.length; ++i) {
                    unVoted.get(i).setScore(results[i]); // add score to it
                }
                saveScoresToRedis(userID, unVoted);
                saveScoresToMysql(userID, results);
            }
        }
        logger.info("compute and save score for user {}, takes {}ms", userID,
                System.currentTimeMillis() - start);
    }

    private Map<String, Map<String, Double>> getModel(int userID)
            throws SQLException, CorruptIndexException, IOException {
        Map<String, Map<String, Double>> cache = modelCache.get(userID);
        if (cache != null) {
            return cache;
        } else {
            Map<String, Map<String, Double>> model = trainModel(userID);
            if (model != null) {
                modelCache.put(userID, model);
            }
            return model;
        }
    }

    public void handlerFetcherEvent(FetcherEvent e) throws SQLException,
            CorruptIndexException, IOException {
        List<Integer> userIDs = DBHelper.fetchUserIDsBySubID(ds, e.subid);
        Jedis redis = jedis.getResource();
        try {
            Pipeline pipeline = redis.pipelined();
            for (int userID : userIDs) {
                Map<String, Map<String, Double>> model = getModel(userID);
                if (model != null) {
                    byte[] key = Utils.genKey(userID, e.subid);
                    List<Integer> feedids = e.feedids;
                    for (Integer feedid : feedids) {
                        double score = NaiveBayes.classify(model, feedid);
                        pipeline.zadd(key, score, feedid.toString()
                                .getBytes());
                    }
                }
            }
            pipeline.sync();
        } finally {
            jedis.returnResource(redis);
        }
    }

    public void handlerUserEvent(UserEvent e) throws CorruptIndexException,
            SQLException, IOException {

        Integer c = combineEvents.get(e.userID);
        if (c == null) {
            c = 0;
        }
        c += 1;
        if (c <= eventsThreashold) {
            // pending
            combineEvents.put(e.userID, c);
        } else {
            // clear counter
            combineEvents.remove(e.userID);
            computeAddSaveScore(e.userID);
        }
    }

    public void onFecherEvent(int subid, List<Integer> feedids) {
        queue.offer(new FetcherEvent(subid, feedids));
    }

    public void onFeedEvent(int userID, int feedID) {
        queue.offer(new UserEvent(userID, feedID));
    }

    public void run() {
        Event event = null;
        while (running) {
            try {
                event = queue.take();
                if (event instanceof FetcherEvent) {
                    handlerFetcherEvent((FetcherEvent) event);
                } else if (event instanceof UserEvent) {
                    handlerUserEvent((UserEvent) event);
                }
            } catch (InterruptedException ignore) {
            } catch (Exception e) {
                // event is dropped
                logger.error(e.getMessage(), e);
            }
        }
        logger.debug("user vote daemon stopped");
    }

    private void saveScoresToMysql(int usreID, double[] results)
            throws SQLException {
        Arrays.sort(results);
        double neutral = results[(int) (results.length * dislikeRatio)];
        double like = results[(int) (results.length * (1 - likeRatio))];

        Connection con = ds.getConnection();
        try {
            PreparedStatement ps = con
                    .prepareStatement("update users set like_score = ?, neutral_score =? where id = ?");
            ps.setDouble(1, like);
            ps.setDouble(2, neutral);
            ps.setInt(3, usreID);
            ps.executeUpdate();
            Utils.closeQuietly(ps);
        } finally {
            Utils.closeQuietly(con);
        }
    }

    private void saveScoresToRedis(int userID, List<FeedScore> unVoted) {
        Jedis redis = jedis.getResource();
        try {
            Pipeline pipelined = redis.pipelined();
            FeedScore scores[] = new FeedScore[unVoted.size()];
            scores = unVoted.toArray(scores);
            Arrays.sort(scores);
            byte[] lastKey = null;
            int lastSubID = -1;
            for (FeedScore fs : scores) {
                if (fs.subid != lastSubID) {
                    if (lastKey != null) {
                        pipelined.expire(lastKey, expireSeconds);
                    }
                    lastKey = Utils.genKey(userID, fs.subid);
                    pipelined.del(lastKey); // delete it
                }
                lastSubID = fs.subid;
                byte[] member = Integer.toString(fs.feedID).getBytes();
                pipelined.zadd(lastKey, fs.score, member);
            }
            pipelined.sync();
        } finally {
            jedis.returnResource(redis);
        }
    }

    public void start() {
        running = true;
        deamonThread = new Thread(this);
        deamonThread.setName("score-daemon");
        deamonThread.setDaemon(true);
        deamonThread.start();
        logger.debug("score daemon started");
    }

    public void stop() {
        running = false;
        deamonThread.interrupt();
    }

    private Map<String, Map<String, Double>> trainModel(int userID)
            throws SQLException, CorruptIndexException, IOException {
        Connection con = ds.getConnection();
        Map<String, Map<String, Double>> model = null;
        try {
            Statement stat = con.createStatement();
            List<Integer> downs = DBHelper.fetchDownIDs(stat, userID);
            List<Integer> ups = DBHelper.fetchUpIDs(stat, userID);
            // List<Integer> reads = DataHelper.fetchRecentRead(stat,
            // userID);
            if (ups.size() > 0 && downs.size() > 0) {
                model = train(ups, downs);
            }
            stat.close();
        } finally {
            Utils.closeQuietly(con);
        }
        return model;
    }
}
