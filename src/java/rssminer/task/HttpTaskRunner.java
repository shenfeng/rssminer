package rssminer.task;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.http.AsyncHanlder;
import me.shenfeng.http.HttpClient;
import me.shenfeng.http.ResponseFuture;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTaskRunner {

    static class Entry {
        IHttpTask task;
        ResponseFuture future;

        public Entry(IHttpTask task, ResponseFuture future) {
            this.task = task;
            this.future = future;
        }
    }

    private static final AsyncHanlder NOP = new AsyncHanlder() {
        public void onCompleted(HttpResponse response) {
        }
    };

    private static Logger logger = LoggerFactory
            .getLogger(HttpTaskRunner.class);

    private final HttpClient mClient;
    private final IHttpTaskProvder mSource;
    private final String mPrefix;
    private final int mQueueSize;
    private final int mWorkerCount;
    private long startTime;
    private AtomicInteger mCounter = new AtomicInteger(0);
    private volatile boolean mRunning;
    private Thread[] mWorkerThreads;
    private Thread mConsummerThread;
    private final Map<Integer, Integer> mStat = new TreeMap<Integer, Integer>();
    private final BlockingQueue<Entry> mQueue;

    private Runnable mWorker = new Runnable() {
        public void run() {
            IHttpTask task = mSource.nextTask();
            try {
                while (mRunning && task != null) {
                    ResponseFuture f = mClient.execGet(task.getUri(),
                            task.getHeaders(), NOP);
                    mQueue.put(new Entry(task, f));
                    task = mSource.nextTask();
                }
            } catch (InterruptedException e) {
                // ignored
            }
            logger.info("{} producer is stopped", mPrefix);
            mRunning = false;
        }
    };

    private Runnable mConsummer = new Runnable() {
        private void recordStat(HttpResponse resp) {
            Integer code = resp.getStatus().getCode();
            Integer c = mStat.get(code);
            if (c == null)
                c = 0;
            mStat.put(code, ++c);
        }

        public void run() {
            try {
                while (mRunning || !mQueue.isEmpty()) {
                    Entry entry = mQueue.take();
                    IHttpTask task = entry.task;
                    try {
                        // take care of timeout
                        HttpResponse resp = entry.future.get();
                        recordStat(resp);
                        logger.trace("{} {}", resp.getStatus(), task.getUri());
                        task.doTask(entry.future.get());
                    } catch (Exception e) {
                        logger.error(task.getUri().toString(), e);
                    } finally {
                        mCounter.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            }
            logger.info("{} consummer is stopped", mPrefix);
            mRunning = false;
        }
    };

    public HttpTaskRunner(IHttpTaskProvder source, HttpClient client,
            int queueSize, int worker, String prefix) {
        mSource = source;
        mClient = client;
        mQueueSize = queueSize;
        mWorkerCount = worker;
        mQueue = new ArrayBlockingQueue<Entry>(queueSize);
        mPrefix = prefix;
    }

    public String getRate() {
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        return format("%.2f", mCounter.get() / m);
    }

    public Map<Integer, Integer> getStat() {
        mStat.put(1000, mCounter.get());
        mStat.put(1200, mQueueSize);
        mStat.put(1201, mQueue.size());
        mStat.put(1210, mWorkerCount);
        mStat.put(1300, (int) (startTime / 100));
        return mStat;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void start() {
        mRunning = true;
        mWorkerThreads = new Thread[mWorkerCount];
        for (int i = 0; i < mWorkerCount; ++i) {
            mWorkerThreads[i] = new Thread(mWorker, mPrefix + " Worker#" + i);
            mWorkerThreads[i].start();
        }

        mConsummerThread = new Thread(mConsummer, mPrefix + " Consumer");
        mConsummerThread.start();

        startTime = currentTimeMillis();
        mCounter.set(0);
        logger.info("starting {}", mPrefix);
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;
            for (Thread t : mWorkerThreads) {
                t.interrupt();
            }
            mConsummerThread.interrupt();
            logger.info("{}, {} req/min, stoping", mPrefix, getRate());
        }
    }

    public String toString() {
        return format("%s, %d req, %s req/min, %d(%d)\n%s", mPrefix,
                mCounter.get(), getRate(), mQueueSize, mQueue.size(), mStat);
    }
}
