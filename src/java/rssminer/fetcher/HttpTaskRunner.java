package rssminer.fetcher;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;

import java.net.URI;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.Utils;

public class HttpTaskRunner {

    public static String trimRemoveBom(String html) {
        html = html.trim();
        if (html.length() > 0) {
            char c = html.charAt(0);
            // bom, magic number
            if((int)c == 65279) {
                html = html.substring(1);
            }
        }
        return html;
    }

    class TextHandler implements ITextHandler {

        private IHttpTask task;

        public TextHandler(IHttpTask task) {
            if (task == null) {
                throw new NullPointerException("task can not be null");
            }
            this.task = task;
        }

        // run in the HTTP client loop thread
        public void onSuccess(int status, Map<String, String> headers,
                String body) {
            try {
                if (status == 301) {
                    String l = headers.get(LOCATION);
                    if (l == null) {
                        task.onThrowable(new Exception(
                                "301: but has no location header"));
                        return;
                    } else {
                        l = task.getUri().resolve(l).toString();
                        headers.put(LOCATION, l); // convert to full path
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
                        addTask(retry);
                    } else {
                        task.onThrowable(new Exception(
                                "redirect more than 4 times"));
                    }
                } else {
                    finish(body, headers);
                }
            } finally {
                finishTask(task, status);
            }
        }

        public void finish(String body, Map<String, String> headers) {
            int status = 200;
            body = trimRemoveBom(body);
            String ct = headers.get(HttpUtils.CONTENT_TYPE);
            if (ct != null && ct.toLowerCase().indexOf("html") != -1) {
                try {
                    String rss = Utils.extractRssUrl(body, task.getUri());
                    if (rss != null && rss.length() > 0) {
                        headers.clear();
                        headers.put(LOCATION, rss);
                        status = 301;
                        logger.info("{} html, extract {}", task.getUri(), rss);
                    } else {
                        logger.warn("{} {} no rss link", task.getUri(), ct);
                    }
                } catch (Exception e) {
                    logger.error("try to extract rss link", e);
                }
            }
            task.doTask(status, headers, body);
        }

        public void onThrowable(Throwable t) {
            try {
                logger.debug(task.getUri().toString(), t);
                task.onThrowable(t);
            } finally {
                finishTask(task, 600);
            }
        }
    }

    class Worker implements Runnable {
        public void run() {
            while (mRunning) {
                try {
                    tryFillTask();
                    mConcurrent.acquire(); // limit concurrency
                    final IHttpTask task = mTaskQueue.poll(); // can not be null
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
                } catch (InterruptedException e) { // die
                }
            }
            mRunning = false;
        }
    }

    static Logger logger = LoggerFactory.getLogger(HttpTaskRunner.class);
    private final IHttpTasksProvder mBulkProvider;
    private final IBlockingTaskProvider mBlockingProvider;
    private final String mName;
    private final ConcurrentLinkedQueue<IHttpTask> mTaskQueue;
    private final Semaphore mConcurrent;

    private volatile int mCounter = 0;
    private volatile long startTime;
    private volatile boolean mRunning;
    private Thread mWorkerThread;
    private final int mBlockingGetTimeout;
    private final HashSet<URI> runningTasks = new HashSet<URI>();

    private final ConcurrentHashMap<Object, Object> mStat;

    public HttpTaskRunner(HttpTaskRunnerConf conf) {
        mBlockingGetTimeout = conf.blockingTimeOut;
        mBulkProvider = conf.bulkProvider;
        mBlockingProvider = conf.blockingProvider;
        if (mBulkProvider == null) {
            throw new NullPointerException("bulk provider can not be null");
        }
        // consumer and producer need access concurrently,
        // prevent too many concurrent HTTP request
        mTaskQueue = new ConcurrentLinkedQueue<IHttpTask>();
        mConcurrent = new Semaphore(conf.queueSize);

        mName = conf.name;
        mStat = new ConcurrentHashMap<Object, Object>(24, 0.75f, 2);
        mStat.put("QueueSize", conf.queueSize);
    }

    private Map<Object, Object> computeStat() {
        mStat.put("Total", mCounter);
        mStat.put("Remain", mTaskQueue.size());
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        mStat.put("PerMiniute", mCounter / m);
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

    private void finishTask(IHttpTask task, int status) {
        ++mCounter;
        mConcurrent.release();
        recordStat(status);
        synchronized (runningTasks) {
            runningTasks.remove(task.getUri());
        }
    }

    private boolean addTask(IHttpTask task) {
        if (task == null) {
            return false;
        }
        synchronized (runningTasks) {
            if (runningTasks.contains(task.getUri())) {
                return false;
            }
            runningTasks.add(task.getUri());
        }
        mTaskQueue.add(task);
        return true;
    }

    void tryFillTask() {
        while (mTaskQueue.isEmpty()) {
            // slow things down a bit
            if (addTask(mBlockingProvider.getTask(1))) {// high priority
                break;
            }
            // first bulk fetch, since it fast, then blocking get
            List<IHttpTask> tasks = mBulkProvider.getTasks();
            if (tasks != null) {
                boolean add = false;
                for (IHttpTask t : tasks) {
                    if (addTask(t)) {
                        add = true;
                    }
                }
                if (add) {
                    break;
                }
            }
            if (addTask(mBlockingProvider.getTask(mBlockingGetTimeout))) {
                break;
            }
        }
    }
}
