package rssminer.db.perf;

import java.util.Random;

public abstract class AbstractPerfTest {

    Random random = new Random();

    static final int USER_ID_START = 100000;
    static final int USER_ID_END = USER_ID_START + 10000;

    static final int SUB_ID_START = 10000;
    static int NUM_SUBS_PER_USER = 40;

    static int NUM_TEST = 1000;

    public double getScore() {
        return random.nextDouble() * 100;
    }

    public int getFeeIDStart() {
        return random.nextInt(10000 * 100);
    }

    public int getPerSubFeedCount() {
        return random.nextInt(60) + 10;
    }

    public int getSubsPerUser() {
        return random.nextInt(NUM_SUBS_PER_USER) + 1;
    }

    public int[] randSubIds() {
        int count = random.nextInt(15) + 1;
        int[] data = new int[count];
        for (int i = 0; i < count; ++i) {
            data[i] = i + SUB_ID_START;
        }
        return data;
    }

    public abstract void insertTestData() throws Exception;

    public abstract void testTotalPerf() throws Exception;

    public abstract void testPerSubPerf() throws Exception;

    public abstract void testFewSubsPerf() throws Exception;
}
