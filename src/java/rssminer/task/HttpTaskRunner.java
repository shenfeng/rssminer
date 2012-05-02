package rssminer.task;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.HOURS;
import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;

import java.net.URI;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTaskRunner {

    class TextHandler implements ITextHandler {

        private IHttpTask task;

        public TextHandler(IHttpTask task) {
            this.task = task;
        }

        // run in the http client loop thread
        public void onSuccess(int status, Map<String, String> headers,
                String body) {
            ++mCounter;
            mConcurrent.release();
            recordStat(status);
            if (status == 301) {
                String l = headers.get(LOCATION);
                if (l == null) {
                    task.onThrowable(new Exception(
                            "301: but has no location header"));
                    return;
                } else {
                    l = task.getUri().resolve(l).toString();
                    headers.put(LOCATION, l);
                    task.doTask(status, headers, body);
                }
            } else if (status == 302) {
                String l = headers.get(LOCATION);
                if (l == null) {
                    task.onThrowable(new Exception(
                            "302: but has no location header"));
                    return;
                }
                URI loc = task.getUri().resolve(l);
                RetryHttpTask retry = new RetryHttpTask(task, loc);
                if (retry.retryTimes() < 4) {
                    // different thread
                    mTaskQueue.offer(retry);
                } else {
                    task.onThrowable(new Exception(
                            "redirect more than 4 times"));
                }
            } else {
                task.doTask(status, headers, body);
            }
        }

        public void onThrowable(Throwable t) {
            logger.debug(task.getUri().toString(), t);
            ++mCounter;
            recordStat(600); // 600 is error
            mConcurrent.release();
            task.onThrowable(t);
        }
    }

    class Worker implements Runnable {
        public void run() {
            try {
                while (mRunning) {
                    tryFillTask();
                    mConcurrent.acquire();
                    final IHttpTask task = mTaskQueue.poll();
                    if (task == null) {
                        break; // no job, die. can't happen => filled
                    }
                    try {
                        TextRespListener listener = new TextRespListener(
                                new TextHandler(task));
                        // copy. convert from Clojure map to java map
                        TreeMap<String, String> headers = new TreeMap<String, String>(
                                task.getHeaders());
                        CLIENT.get(task.getUri(), headers, task.getProxy(),
                                listener);
                    } catch (UnknownHostException e) {
                        task.onThrowable(e);
                    }
                }
            } catch (InterruptedException ignore) {
            }
            mRunning = false;
        }
    }

    static Logger logger = LoggerFactory.getLogger(HttpTaskRunner.class);
    private final IHttpTasksProvder mBulkProvider;
    private final IBlockingTaskProvider mBlockingProvider;
    private final String mName;
    private final ConcurrentLinkedQueue<IHttpTask> mTaskQueue;

    private volatile int mCounter = 0;
    private volatile long startTime;
    private volatile boolean mRunning;
    private Thread mWorkerThread;
    private final Semaphore mConcurrent; // limit concurrent http get

    private final int mBulkCheckInterval;
    private final int mBlockingGetTimeout;
    private final long DUMP_STATS_INTERVAL = HOURS.toMillis(6);
    private long lastBulkCheckTs = 0;
    private long lastDumpStatTs = currentTimeMillis();

    private final ConcurrentHashMap<Object, Object> mStat;

    public HttpTaskRunner(HttpTaskRunnerConf conf) {
        mBlockingGetTimeout = conf.blockingTimeOut;
        mBulkCheckInterval = conf.bulkCheckInterval;
        mBulkProvider = conf.bulkProvider;
        mBlockingProvider = conf.blockingProvider;
        if (mBulkProvider == null) {
            throw new NullPointerException("bulk provider can not be null");
        }
        // consumer and producer need access concurrently
        mTaskQueue = new ConcurrentLinkedQueue<IHttpTask>();

        // prevent too many concurrent HTTP request
        mConcurrent = new Semaphore(conf.queueSize);
        mName = conf.name;
        mStat = new ConcurrentHashMap<Object, Object>(24, 0.75f, 2);
        mStat.put("QueueSize", conf.queueSize);
    }

    private Map<Object, Object> computeStat() {
        mStat.put("Total", mCounter);
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        mStat.put("PerMiniute", mCounter / m);
        mStat.put("QueuePermits", mConcurrent.availablePermits());
        DateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss");
        mStat.put("StartTime", format.format(new Date(startTime)));
        return mStat;
    }

    public Map<Object, Object> getStat() {
        return new HashMap<Object, Object>(computeStat());
    }

    public boolean isRunning() {
        return mRunning;
    }

    private void recordStat(int code) {
        Object c = mStat.get(code);
        Integer count = 0;
        if (c instanceof Integer) {
            count = ((Integer) c).intValue() + 1;
        }
        mStat.put(code, count);
    }

    public void start() {
        mRunning = true;
        mWorkerThread = new Thread(new Worker(), mName);
        mWorkerThread.setDaemon(true);
        mWorkerThread.start();
        startTime = currentTimeMillis();
        logger.info("{} started", mName);
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;
            mWorkerThread.interrupt();
            logger.info(toString());
        }
    }

    public String toString() {
        return format("%s: %s", mName, computeStat());
    }

    void tryFillTask() {
        while (mTaskQueue.isEmpty()) {
            long currentTime = currentTimeMillis();

            // first bulk fetch, since it fast, then blocking get
            if (lastBulkCheckTs + mBulkCheckInterval < currentTime) {
                List<IHttpTask> tasks = mBulkProvider.getTasks();
                lastBulkCheckTs = currentTime;
                if (tasks != null && tasks.size() != 0) {
                    for (IHttpTask t : tasks) {
                        mTaskQueue.offer(t);
                    }
                    break;
                }
            }
            if (lastDumpStatTs + DUMP_STATS_INTERVAL < currentTime) {
                lastDumpStatTs = currentTime;
                logger.info(toString());
            }
            IHttpTask task = mBlockingProvider.getTask(mBlockingGetTimeout);
            if (task != null) {
                mTaskQueue.offer(task);
                break;
            }
        }
    }
}
