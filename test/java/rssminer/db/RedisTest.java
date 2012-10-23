/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RedisTest {

    JedisPool pool;
    private static String KEY = "sdfsdf";

    private static byte[] STATUS = "s".getBytes();
    private static byte[] BODY = "b".getBytes();

    @Before
    public void setup() {
        pool = new JedisPool("127.0.0.1", 6379);
    }

    private void getFromRedis() {
        Jedis redis = pool.getResource();
        Map<byte[], byte[]> m = redis.hgetAll("sdfsf".getBytes());
        System.err.println(m.isEmpty());
        Iterator<Entry<byte[], byte[]>> ok = m.entrySet().iterator();
        while (ok.hasNext()) {
            Entry<byte[], byte[]> n = ok.next();
            String k = new String(n.getKey());
            System.out.println(k);
        }
        pool.returnResource(redis);

    }


    private void putToRedis(int status, String type) {
        Jedis redis = pool.getResource();
        Map<byte[], byte[]> m = new HashMap<byte[], byte[]>();
        m.put(STATUS, (status + "|" + type).getBytes());
        m.put(BODY, "body".getBytes());
        m.put("t".getBytes(), "".getBytes());
        m.put("tt".getBytes(), null); // NPE
        redis.hmset(KEY.getBytes(), m);
        // redis.hmset(key, hash)
        pool.returnResource(redis);
    }

    @Test
    public void testMap() throws InterruptedException {
        Jedis redis = pool.getResource();
        putToRedis(1212, null);
        getFromRedis();
    }
}
