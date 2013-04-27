/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.fetcher;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static rssminer.Utils.CLIENT;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import org.httpkit.DynamicBytes;
import org.httpkit.HttpMethod;
import org.httpkit.client.*;
import org.httpkit.client.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.Utils;
import rssminer.jsoup.HtmlUtils;

class Filter implements IFilter {

    public boolean accept(Map<String, String> headers) {
        // just accept all, only limit size
        return true;
    }

    public boolean accept(DynamicBytes partialBody) {
        return partialBody.length() <= 2 * 1024 * 1024;
    }
}

public class HttpTaskRunner {
    private static boolean isValidUrl(String url) {
        try {
            URI.create(url);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static final String LOCATION = "location";

    class TextHandler implements IResponseHandler {

        private IHttpTask task;

        public TextHandler(IHttpTask task) {
            if (task == null) {
                throw new NullPointerException("task can not be null");
            }
            this.task = task;
        }

        private boolean isHtml(Map<String, String> headers, String body) {
            String ct = null;
            for (Entry<String, String> e : headers.entrySet()) {
                if ("content-type".equalsIgnoreCase(e.getKey())) {
                    ct = e.getValue();
                    break;
                }
            }
            if (ct != null && ct.toLowerCase().contains("html")) {
                return true;
            }
            return false;
        }

        public void finish(int status, String body, Map<String, String> headers) {
            body = Utils.trimRemoveBom(body);
            if (status == 200 && isHtml(headers, body)) {
                try {
                    String rss = HtmlUtils.extractRssUrl(body, task.getUri());
                    if (rss != null && rss.length() > 0) {
                        if (isValidUrl(rss)) {
                            headers.clear();
                            headers.put(LOCATION, rss);
                            status = 301;
                            logger.info("{} html, extract {}", task.getUri(), rss);
                        } else {
                            logger.info("no valid url: {}", rss);
                        }
                    } else {
                        logger.warn("{}, but no rss link", task.getUri());
                    }
                } catch (Exception e) {
                    logger.error("try to extract rss link", e);
                }
            }
            task.doTask(status, headers, body);
        }

        // run in the HTTP client loop thread
        public void onSuccess(int status, Map<String, String> headers, Object b) {
            if (!(b instanceof String)) {

                return;
            }

            String body = (String) b;

            try {
                if (status == 301) {
                    String l = headers.get(LOCATION);
                    if (l == null) {
                        task.onThrowable(new Exception("301: but has no location header"));
                        return;
                    } else {
                        l = task.getUri().resolve(l).toString();
                        headers.put(LOCATION, l); // convert to full path
                        task.doTask(status, headers, body);
                    }
                } else if (status == 302) {
                    String l = headers.get(LOCATION);
                    if (l == null) {
                        task.onThrowable(new Exception("302: but has no location header"));
                        return;
                    }
                    URI loc = task.getUri().resolve(l);
                    RetryHttpTask retry = new RetryHttpTask(task, loc);
                    if (retry.retryTimes() < 4 && !loc.equals(task.getUri())) {
                        addTask(retry);
                    } else {
                        task.onThrowable(new Exception("redirect more than 4 times"));
                    }
                } else {
                    finish(status, body, headers);
                }
            } finally {
                taskFinished(task, status);
            }
        }

        public void onThrowable(Throwable t) {
            try {
                logger.debug(task.getUri().toString(), t);
                task.onThrowable(t);
            } finally {
                if (t instanceof IOException) {
                    String mesg = t.getMessage();
                    if (mesg == null) {
                        mesg = "";
                    }
                    // Connection reset by peer
                    if (mesg.indexOf("reset") != -1) {
                        taskFinished(task, 610);
                    } else {
                        taskFinished(task, 611);
                    }
                } else if (t instanceof TimeoutException) {
                    taskFinished(task, 600);
                } else {
                    // logger.error(t.getMessage(), t);
                    taskFinished(task, 620);
                }
            }
        }
    }

    private static final ExecutorService pool = new ThreadPoolExecutor(0, 4, 10,
            TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    class Worker implements Runnable {
        public void run() {
            while (mRunning) {
                try {
                    tryFillTask();
                    mConcurrent.acquire(); // limit concurrency
                    final IHttpTask task = mTaskQueue.poll(); // can not be null
                    logger.info("task {}", task.getUri());
                    RespListener listener = new RespListener(new TextHandler(task), filter,
                            pool);
                    CLIENT.exec(task.getUri().toString(), task.getHeaders(), null,
                            new RequestConfig(HttpMethod.GET, 40000, 80000, null), listener);
                } catch (InterruptedException e) { // die
                } catch (Exception e) {
                    logger.error("ERROR! should not happend", e);
                }
            }
            mRunning = false;
        }
    }

    static Logger logger = LoggerFactory.getLogger(HttpTaskRunner.class);

    private final IHttpTasksProvder mBulkProvider;
    private final IBlockingTaskProvider mBlockingProvider;
    private final String mName;
    private final Filter filter = new Filter();
    private final ConcurrentLinkedQueue<IHttpTask> mTaskQueue;
    private final Semaphore mConcurrent;

    // mConnter is single Thread: http-client thread
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

    private boolean addTask(IHttpTask task) {
        if (task == null) {
            return false;
        }
        try {
            task.getUri();
        } catch (Exception e) {
            // TODO URL maybe not valid, clean db
            logger.error("exception: {}", e);
            task.doTask(304, new TreeMap<String, String>(), null);
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

    private Map<Object, Object> computeStat() {
        mStat.put("Total", mCounter);
        mStat.put("RemainJob", mTaskQueue.size());
        mStat.put("RunningJob", runningTasks.size());
        mStat.put("Permit", mConcurrent.availablePermits());
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        mStat.put("PerMiniute", String.format("%.3f", mCounter / m));
        mStat.put("Start", new Date(startTime));
        return mStat;
    }

    private void taskFinished(IHttpTask task, int status) {
        ++mCounter;
        mConcurrent.release();
        recordStat(status);
        synchronized (runningTasks) {
            runningTasks.remove(task.getUri());
        }
    }

    public Map<Object, Object> getStat() {
        return new HashMap<Object, Object>(computeStat());
    }

    public boolean isRunning() {
        return mRunning;
    }

    private void recordStat(int code) {
        Object c = mStat.get(code);
        Integer count = 1;
        if (c instanceof Integer) {
            count = (Integer) c + 1;
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
            // slow things down a bit
            if (addTask(mBlockingProvider.getTask(mBlockingGetTimeout))) {
                break;
            }
        }
    }
}
