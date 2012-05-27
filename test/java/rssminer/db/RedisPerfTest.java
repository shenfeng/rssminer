package rssminer.db;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * Simple Redis performance Test
 *
 * LOCAL: redis server and client are on the same machine(M1).
 * REMOTE: redis server(M2) and client(M1) are on different machine.
 *
 * M1: CPU Intel i7 2600 @ 3.4G, MEM 16G
 * M2: CPU Intel T7200 @ 2.0G, MEM 3G
 *
 */

public class RedisPerfTest {

    Jedis jedis;
    static final int COUNT = 1000000;
    static final int MAX_CACHE_LENGTH = 2048; // 2k
    final String redisHost = "192.168.1.10";

    Random r = new Random();

    public String getUserKey(int id) {
        return "user:" + id;
    }

    public String genRandString() {
        int length = r.nextInt(MAX_CACHE_LENGTH);
        byte data[] = new byte[length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        return new String(data);
    }

    @Before
    public void setup() {
        JedisPool pool = new JedisPool(new JedisPoolConfig(), redisHost);
        jedis = pool.getResource();
    }

    // LOCAL 202s, used_memory_human:152.94M
    // REMOTE 1086s, used_memory_human:171.64M
    @Test
    public void testLPush1() {
        for (int i = 0; i < COUNT; i++) {
            String user_id = getUserKey(i);
            int count = r.nextInt(20);
            for (int j = 0; j < count; ++j) {
                int activityId = r.nextInt();
                jedis.lpush(user_id, activityId + "");
            }
        }

    }

    // LOCAL 19s, used_memory_human:165.88M
    // REMOTE 116s
    @Test
    public void testLpush2() {
        // do it after testLpush1
        for (int i = 0; i < COUNT; ++i) {
            String user_id = getUserKey(i);
            jedis.lpush(user_id, r.nextInt() + "");
        }
    }

    // LOCAL 60s, used_memory_human:2.06G (after flushall)
    // REMOTE 227s, used_memory_human:1.96G (after flushall)
    @Test
    public void testSetupCache() {
        for (int i = 0; i < COUNT; ++i) {
            jedis.set(getUserKey(i), genRandString());
        }
    }

    // LOCAL 0.588s
    // REMOTE 2.928s
    @Test
    public void testGet() {
        int LOOP = 10000;
        for (int i = 0; i < LOOP; ++i) {
            String s = jedis.get(getUserKey(r.nextInt(COUNT)));
            if (s == null) {
                throw new RuntimeException("get failed");
            }
        }
    }
}
