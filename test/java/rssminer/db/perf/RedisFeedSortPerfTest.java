package rssminer.db.perf;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Tuple;

public class RedisFeedSortPerfTest extends AbstractPerfTest {

    JedisPool pool;

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

    // feeds: 1223852 => 9.874s

    @Test
    public void insertTestData() {
        Jedis jedis = pool.getResource();

        long totalFeedCount = 0;

        for (int userid = USER_ID_START; userid < USER_ID_END; userid++) {
            int subcount = getSubsPerUser();
            int start = getFeeIDStart();
            for (int subid = SUB_ID_START; subid < subcount + SUB_ID_START; subid++) {
                int feedCount = getPerSubFeedCount();
                totalFeedCount += feedCount;
                // pipline 2.557s vs 6.442s
                byte[] key = gen_key(userid, subid);
                Pipeline p = jedis.pipelined();
                for (int feedid = start; feedid < feedCount + start; feedid++) {
                    p.zadd(key, getScore(), Integer.toString(feedid)
                            .getBytes());
                }
                start += feedCount;
                p.sync();
            }
        }

        System.out.println(totalFeedCount);
        pool.returnResource(jedis);
    }

    // 25s
    @Test
    public void testTotalPerf() {
        Jedis jedis = pool.getResource();
        for (int userid = USER_ID_START; userid < USER_ID_START + NUM_TEST; userid++) {
            int subcount = getSubsPerUser();
            byte[][] keys = new byte[subcount][];
            for (int i = 0; i < subcount; i++) {
                keys[i] = gen_key(userid, i + SUB_ID_START);
            }
            jedis.zunionstore(gen_key(userid), keys);
            Set<Tuple> result = jedis.zrevrangeWithScores(gen_key(userid), 0,
                    29);
//            System.out.println(result.size());
            jedis.del(gen_key(userid));
        }

        pool.returnResource(jedis);
    }

    // 1.854
    @Test
    public void testFewSubsPerf() throws Exception {
        Jedis jedis = pool.getResource();

        String test_tmp_key = "sdfsdfs";

        for (int userid = USER_ID_START; userid < USER_ID_END + NUM_TEST; userid++) {
            int[] ids = randSubIds();
            byte[][] keys = new byte[ids.length][];
            for (int i = 0; i < ids.length; i++) {
                keys[i] = gen_key(userid, ids[i]);
            }
            jedis.zunionstore(test_tmp_key.getBytes(), keys);
            Set<Tuple> result = jedis.zrevrangeWithScores(
                    test_tmp_key.getBytes(), 0, 29);
            jedis.del(test_tmp_key.getBytes());
//            System.out.println(result.size());
        }
    }

    // 0.337s
    @Test
    public void testPerSubPerf() throws Exception {
        Jedis jedis = pool.getResource();
        for (int userid = USER_ID_START; userid < USER_ID_END + NUM_TEST; userid++) {
            int rssLinkID = random.nextInt(NUM_SUBS_PER_USER) + SUB_ID_START;

            Set<Tuple> a = jedis.zrevrangeWithScores(
                    gen_key(userid, rssLinkID), 0, 19);
            System.out.println(a.size());
            for (Tuple tuple : a) {
                String e = tuple.getElement();
                double s = tuple.getScore();
//                System.out.println(e + "\t" + s);
            }
        }
        pool.returnResource(jedis);
    }
}
