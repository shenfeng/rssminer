package rssminer;

import clojure.lang.Keyword;
import me.shenfeng.dbcp.PerThreadDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.search.Searcher;
import rssminer.tools.Utils;

import java.io.IOException;
import java.util.*;

public class NearDuplicate implements Runnable {

    static Logger logger = LoggerFactory.getLogger(NearDuplicate.class);

    private static volatile int[] feedhashes;

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
        int md = feedhashes[feedid];
        for (int i = 0; i < feedhashes.length; i++) {
            if (i != feedid) {
                int d = rssminer.Utils.hammingDistance(md, feedhashes[i]);
                if (d < distance) {
                    result.add(i);
                    logger.info("{}:{} {}:{}, distance: {}", new Object[]{
                            feedid, md, i, feedhashes[i], d
                    });
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource("jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        Searcher.initGlobalSearcher("/var/rssminer/index", config);

        NearDuplicate duplicate = new NearDuplicate();
        duplicate.run();
    }

    public void run() {
        try {
            logger.info("init NearDuplicate");
            int max = Utils.getMaxID();
            int[] hashes = new int[max + 1];
            for (int i = 0; i <= max; i++) {
                if(i % 60000 == 0) {
                    logger.info("handing {}, max {}", i, max);
                }
                int hash = rssminer.Utils.simHash(i);
                hashes[i] = hash;
            }
            feedhashes = hashes;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
