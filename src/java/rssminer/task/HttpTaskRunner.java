package rssminer.task;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import me.shenfeng.BlockingTransferQueue;
import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTaskRunner {

    private static Logger logger = LoggerFactory
            .getLogger(HttpTaskRunner.class);

    private final HttpClient mHttp;
    private final IHttpTaskProvder mProvider;
    private final String mPrefix;
    private long startTime;
    private AtomicInteger mCounter = new AtomicInteger(0);
    private volatile boolean mRunning;
    private Thread mWorkerThread;
    private Thread mConsummerThread;
    private final BlockingTransferQueue<HttpResponseFuture> mQueue;
    private final Map<Integer, Integer> mStat = new TreeMap<Integer, Integer>();

    private Runnable mWorker = new Runnable() {
        public void run() {
            IHttpTask task = mProvider.nextTask();
            try {
                while (mRunning && task != null) {
                    final HttpResponseFuture future = mHttp.execGet(
                            task.getUri(), task.getHeaders());
                    future.setAttachment(task);
                    future.addListener(new Runnable() {
                        public void run() {
                            mQueue.done(future);
                        }
                    });
                    mQueue.put(future);
                    task = mProvider.nextTask();
                }
            } catch (InterruptedException e) {
                // ignore
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
                while (mRunning) {
                    HttpResponseFuture future = mQueue.take();
                    IHttpTask task = (IHttpTask) future.getAttachment();
                    try {
                        HttpResponse resp = future.get();
                        recordStat(resp);
                        logger.trace("{} {}", resp.getStatus(), task.getUri());
                        task.doTask(resp);
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
            int queueSize, String prefix) {
        mProvider = source;
        mHttp = client;
        mQueue = new BlockingTransferQueue<HttpResponseFuture>(queueSize);
        mStat.put(1200, queueSize);
        mStat.put(1300, (int) (startTime / 1000));
        mPrefix = prefix;
    }

    public String getRate() {
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        return format("%.2f", mCounter.get() / m);
    }

    public Map<Integer, Integer> getStat() {
        mStat.put(1000, mCounter.get());
        mStat.put(1201, mQueue.pendingSize());
        return mStat;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void start() {
        mRunning = true;
        mWorkerThread = new Thread(mWorker, mPrefix + " Workder");
        mWorkerThread.start();

        mConsummerThread = new Thread(mConsummer, mPrefix + " Consumer");
        mConsummerThread.start();

        startTime = currentTimeMillis();
        logger.info("starting {}", mPrefix);
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;
            mWorkerThread.interrupt();
            mConsummerThread.interrupt();
            logger.info("{}, {} req/min, stoping", mPrefix, getRate());
        }
    }

    public String toString() {
        return format("%s, %d req, %s req/min, %d\n%s", mPrefix,
                mCounter.get(), getRate(), mQueue.pendingSize(), mStat);
    }
}
