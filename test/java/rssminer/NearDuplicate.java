package rssminer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.shenfeng.dbcp.PerThreadDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.search.Searcher;
import rssminer.tools.Utils;
import clojure.lang.Keyword;

public class NearDuplicate implements Runnable {

    static Logger logger = LoggerFactory.getLogger(NearDuplicate.class);

    private static volatile long[] feedhashes;

    private NearDuplicate() {
    }

    public static void init() {
        Thread thread = new Thread(new NearDuplicate(), "duplicate");
        thread.setDaemon(true);
        thread.start();
    }

    public static List<Integer> similar(int feedid, int distance) {
        if (feedhashes == null) {
            return new ArrayList<Integer>(0);
        }
        ArrayList<Integer> result = new ArrayList<Integer>();
        long md = feedhashes[feedid];
        for (int i = 0; i < feedhashes.length; i++) {
            if (i != feedid) {
                int d = SimHash.hammingDistance(md, feedhashes[i]);
                if (d < distance) {
                    result.add(i);
                    logger.info("{}:{} {}:{}, distance: {}", new Object[] {
                            feedid, md, i, feedhashes[i], d });
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        Searcher.initGlobalSearcher("/var/rssminer/index", config);

        NearDuplicate duplicate = new NearDuplicate();
        duplicate.run();
        logger.info("all computed");
        int counter[] = new int[65];
        for (int i = 0; i < feedhashes.length; i++) {
            long me = feedhashes[i];
            if (me != -1) {
                long innerCounter[] = new long[65];
                for (int j = i + 1; j < feedhashes.length; j++) {
                    long other = feedhashes[j];
                    if (other != -1) {
                        int d = SimHash.hammingDistance(me, other);
                        // if (d == 0) {
                        // logger.info("{}:{}, {}:{}", new Object[] { i, me,
                        // j, other });
                        // }
                        innerCounter[d] += 1;
                    }
                }
                for (int j = 0; j < innerCounter.length; j++) {
                    counter[j] += innerCounter[j];
                }
            }
            if (i % 5000 == 0) {
                logger.info(i + ": " + Arrays.toString(counter));
            }
        }
    }

    public void run() {
        try {
            logger.info("init NearDuplicate");
            int max = Utils.getMaxID();
            // max = 150000;
            long[] hashes = new long[max + 1];

            for (int i = 0; i <= max; i++) {
                if (i % 60000 == 0) {
                    logger.info("handing {}, max {}", i, max);
                }
                long hash = SimHash.simHash(i);
                hashes[i] = hash;
            }
            feedhashes = hashes;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
