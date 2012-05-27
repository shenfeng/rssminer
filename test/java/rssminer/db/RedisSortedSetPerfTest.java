package rssminer.db;

import java.util.Random;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Tuple;

public class RedisSortedSetPerfTest {

    JedisPool pool;
    Random random = new Random();

    static final int NUM_USER = 40000;
    static int NUM_SUBS_PER_USER = 60;

    static int NUM_FEEDS_PER_SUB = 10;
    static int NUM_FEEDS_MAX = 100;

    static final int USER_OFFSET = 10000;
    static final int SUB_OFFSET = 100;

    @Before
    public void setup() {
        pool = new JedisPool("127.0.0.1", 6379);
    }

    public byte[] gen_key(int userid, int subid) {
        String key = "us:u_" + userid + "_s_" + subid;
        return key.getBytes();
    }

    public byte[] gen_key(int userid) {
        String key = "us:u_" + userid;
        return key.getBytes();
    }

    @Test
    public void insertTestData() {
        Jedis jedis = pool.getResource();

        long totalFeedCount = 0;

        for (int userid = 0 + USER_OFFSET; userid < NUM_USER + USER_OFFSET; userid++) {
            int subcount = random.nextInt(NUM_SUBS_PER_USER);
            for (int subid = 0 + SUB_OFFSET; subid < subcount + SUB_OFFSET; subid++) {
                int start = random.nextInt(1000000);
                int feedCount = random.nextInt(NUM_FEEDS_MAX)
                        + NUM_FEEDS_PER_SUB;
                totalFeedCount += feedCount;
                // pipline 2.557s vs 6.442s
                //
                byte[] key = gen_key(userid, subid);

                Pipeline p = jedis.pipelined();

                for (int feedid = start; feedid < feedCount + start; feedid++) {
                    // jedis.zadd(key, random.nextDouble() * 100,
                    // Integer.toString(feedid).getBytes());
                    p.zadd(key, random.nextDouble() * 100,
                            Integer.toString(feedid).getBytes());

                }
                p.sync();
                // jedis.z
            }
        }

        System.out.println(totalFeedCount);
        pool.returnResource(jedis);
    }

    @Test
    public void testRevrange() {
        Jedis jedis = pool.getResource();
        for (int userid = 0 + USER_OFFSET; userid < USER_OFFSET + 1000; userid++) {
            Set<Tuple> a = jedis.zrevrangeWithScores(
                    gen_key(userid, 10 + SUB_OFFSET), 0, 30);
            for (Tuple tuple : a) {
                String e = tuple.getElement();
                double s = tuple.getScore();
                // System.out.println(e + "\t" + s);
            }
            // System.out.println("---------");

            // Set<String> keys = jedis.keys(gen_key(userid));
            // System.out.println(keys);

        }
        pool.returnResource(jedis);
    }

    @Test
    public void testUnionRevrange() {
        Jedis jedis = pool.getResource();
        for (int userid = 0 + USER_OFFSET; userid < USER_OFFSET + 1000; userid++) {
            int subcount = random.nextInt(NUM_SUBS_PER_USER) + 3;
            byte[][] keys = new byte[subcount][];
            for (int i = 0; i < subcount; i++) {
                keys[i] = gen_key(userid, i + SUB_OFFSET);
            }
            jedis.zunionstore(gen_key(userid), keys);
            Set<Tuple> result = jedis.zrevrangeWithScores(gen_key(userid), 30, 60);
            System.out.println(result.size());
            jedis.del(gen_key(userid));
        }

        pool.returnResource(jedis);
    }
}
