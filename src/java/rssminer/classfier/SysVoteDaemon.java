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
import rssminer.db.Vote;
import clojure.lang.Keyword;

public class SysVoteDaemon implements Runnable {

    public static final int EXPIRE_SECONDS = 3600 * 24 * 10; // 10 days
    public static final double LIKE_RATIO = 0.2;
    public static final double DISLIKE_RATIO = 0.5;
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
            IOException {
        Map<String, Map<String, Double>> model = trainModel(userID);
        if (model != null) {
            Watch w = new Watch().start();
            List<FeedScore> unVoted = DBHelper.getUnvotedFeeds(ds, userID);
            if (!unVoted.isEmpty()) {
                List<Integer> unVotedIDs = new ArrayList<Integer>(
                        unVoted.size());
                for (FeedScore feed : unVoted) {
                    unVotedIDs.add(feed.feedID);
                }
                double[] results = classify(model, unVotedIDs);
                int now = (int) (System.currentTimeMillis() / 1000);
                for (int i = 0; i < results.length; ++i) {
                    FeedScore feed = unVoted.get(i);
                    if (results[i] > 0) {
                        // kind of news site, the newer, the better.
                        // per 4 hour, +2 for always positive
                        double t = Math.log((double) (now - feed.publishTs)
                                / (3600 * 4) + 2);
                        results[i] /= t;
                    }
                    feed.setScore(results[i]); // add score to it
                }
                saveScoresToRedis(userID, unVoted);
                saveScoresToMysql(userID, results);
            }
            logger.info(
                    "compute and save score for user {}, {} feeds, takes {}ms",
                    new Object[] { userID, unVoted.size(), w.time() });
        }
    }

    private Map<String, Map<String, Double>> getModel(int userID)
            throws SQLException, IOException {
        Map<String, Map<String, Double>> cache = modelCache.get(userID);
        if (cache != null) {
            if (cache == noModel) {
                return null;
            } else {
                return cache;
            }
        } else {
            return trainModel(userID);
        }
    }

    public void handlerFetcherEvent(FetcherEvent e) throws SQLException,
            IOException {
        Watch w = new Watch().start();
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
        logger.info("rss:{}, feed cnt:{}, {} users, take {}ms", new Object[] {
                e.subid, e.feedids.size(), userIDs.size(), w.time() });
    }

    public void handlerUserEvent(UserEvent e) throws
            SQLException, IOException {
        Integer c = combineEvents.get(e.userID);
        if (c == null) {
            c = 0;
        }
        c += 1;
        if (c <= eventsThreashold && e.feedID != -1) { // -1 => compute now
            // buffer
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
        Event event;
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
        double neutral = results[(int) (results.length * DISLIKE_RATIO)];
        double like = results[(int) (results.length * (1 - LIKE_RATIO))];

        Connection con = ds.getConnection();
        try {
            PreparedStatement ps = con
                    .prepareStatement("update users set like_score = ?, neutral_score =? where id = ?");
            ps.setDouble(1, like);
            ps.setDouble(2, neutral);
            ps.setInt(3, usreID);
            ps.executeUpdate();
            if (!con.getAutoCommit()) {
                con.commit();
            }
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
            pipelined.del(Utils.genKey(userID));
            for (FeedScore fs : scores) {
                if (fs.subid != lastSubID) {
                    if (lastKey != null) {
                        pipelined.expire(lastKey, EXPIRE_SECONDS);
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
        logger.info("score daemon started, eventsThreashold: "
                + eventsThreashold);
    }

    public void stop() {
        running = false;
        deamonThread.interrupt();
    }

    private Map<String, Map<String, Double>> trainModel(int userID)
            throws SQLException, IOException {
        Watch w = new Watch().start();
        List<Vote> votes = DBHelper.fetchVotedIds(ds, userID);
        Map<String, Map<String, Double>> model = null;
        if (votes.size() > 0) {
            model = train(votes);
            modelCache.put(userID, model);
        } else {
            // TODO strategy to expire cache
            modelCache.put(userID, noModel);
        }
        // System.out.println(model);
        logger.info("train model for user {} with {} feeds takes {}ms",
                new Object[] { userID, votes.size(), w.time() });
        return model;
    }
}
