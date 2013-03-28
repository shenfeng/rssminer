/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.dbcp.PerThreadDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.search.Searcher;
import rssminer.tools.Utils;

class Result {
    final int id;
    final int[] counter;

    public Result(int id, int[] counter) {
        this.id = id;
        this.counter = counter;
    }
}

class Int {
    int i = 0;

    public String toString() {
        return Integer.toString(i);
    }

}

class Worker extends Thread {
    // private static final Logger logger =
    // LoggerFactory.getLogger(Worker.class);

    private long[] feedhashes;
    private AtomicInteger id;
    private BlockingQueue<Result> done;

    public Worker(long[] feedhashes, AtomicInteger id, BlockingQueue<Result> done) {
        this.feedhashes = feedhashes;
        this.id = id;
        this.done = done;
    }

    public void run() {
        int i = 0;
        while ((i = id.incrementAndGet()) < feedhashes.length) {
            long me = feedhashes[i];
            // logger.info("handle {}, me: {}", i, me);
            if (me != -1) {
                int innerCounter[] = new int[65];
                for (int j = i + 1; j < feedhashes.length; j++) {
                    long other = feedhashes[j];
                    if (other != -1) {
                        int d = rssminer.Utils.hammingDistance(me, other);
                        innerCounter[d] += 1;
                    }
                }
                try {
                    done.put(new Result(i, innerCounter));
                } catch (InterruptedException e) {
                }
            }
        }
    }
}

public class NearDuplicate {

    static Logger logger = LoggerFactory.getLogger(NearDuplicate.class);
    private static final TreeMap<Integer, Int> distCounter = new TreeMap<Integer, Int>();

    private static volatile long[] feedhashes;

    private NearDuplicate() {
    }

    public static void init() {
        if (feedhashes == null) {
            try {
                logger.info("init NearDuplicate");
                int max = Utils.getMaxID();
                long[] hashes = new long[max + 1];
                Connection db = Utils.getRssminerDB();
                Statement stat = db.createStatement();

                ResultSet rs = stat.executeQuery("select id, simhash from feeds");
                for (int i = 0; i < hashes.length; i++) {
                    hashes[i] = -1;
                }
                while (rs.next()) {
                    hashes[rs.getInt(1)] = rs.getLong(2);
                }
                feedhashes = hashes;
                logger.info("init ok");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static List<Integer> similar(int feedid, int distance) {
        if (feedhashes == null || feedhashes[feedid] == -1) {
            return new ArrayList<Integer>(0);
        }
        ArrayList<Integer> result = new ArrayList<Integer>();
        long hash = feedhashes[feedid];
        for (int idx = 0; idx < feedhashes.length; idx++) {
            if (idx != feedid && feedhashes[idx] != -1) {
                int d = rssminer.Utils.hammingDistance(hash, feedhashes[idx]);
                if (d < distance) {
                    result.add(idx);
                    logger.info("{}:{} {}:{}, distance: {}", new Object[] { feedid, hash, idx,
                            feedhashes[idx], d });
                }
            }
        }
        return result;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        PerThreadDataSource db = new PerThreadDataSource("jdbc:mysql://localhost/rssminer",
                "feng", "");
        Searcher.initGlobalSearcher("/var/rssminer/index", db);

        init();

        AtomicInteger id = new AtomicInteger(0);
        BlockingQueue<Result> done = new ArrayBlockingQueue<Result>(100);

        int processors = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i <= processors; i++) {
            Worker w = new Worker(feedhashes, id, done);
            w.setName("worker-" + i);
            w.setDaemon(true);
            w.start();
        }
        int counter[] = new int[65];
        while (id.get() < feedhashes.length) {
            Result r = done.take();
            int innerCounter[] = r.counter;

            for (int i = 0; i < 4; ++i) {
                int d = innerCounter[i];
                if (d > 0) {
                    Int c = distCounter.get(d);
                    if (c == null) {
                        c = new Int();
                        distCounter.put(d, c);
                    }
                    c.i += 1;
                }
            }

            for (int j = 0; j < innerCounter.length; j++) {
                counter[j] += innerCounter[j];
            }
            if (r.id % 5000 == 0) {
                logger.info(r.id + ": " + Arrays.toString(counter));
                // logger.info("{}: {}", r.id, distCounter.toString());
            }
        }
        FileOutputStream fso = new FileOutputStream("/tmp/result");
        fso.write(Arrays.toString(counter).getBytes());
        fso.write("\n\n".getBytes());
        fso.write(distCounter.toString().getBytes());
        // logger.info();
        // logger.info();
    }

}
