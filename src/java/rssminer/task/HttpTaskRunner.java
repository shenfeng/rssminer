package rssminer.task;

import java.net.URISyntaxException;
import java.util.concurrent.Semaphore;

import me.shenfeng.http.AsyncHanlder;
import me.shenfeng.http.HttpClient;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Hander implements AsyncHanlder {

    private static Logger logger = LoggerFactory.getLogger(Hander.class);

    private IHttpTask mTask;
    private HttpTaskRunner mRunner;

    public Hander(IHttpTask task, HttpTaskRunner runner) {
        mTask = task;
        mRunner = runner;
    }

    public void onCompleted(HttpResponse response) {
        try {
            mTask.doTask(response);
            mRunner.markComplete(mTask, response);
        } catch (Exception e) {
            logger.error(mTask.getUri().toString(), e);
        }
    }
}

public class HttpTaskRunner {

    private static Logger logger = LoggerFactory
            .getLogger(HttpTaskRunner.class);

    private final Semaphore mSemaphore;
    private final HttpClient mClient;
    private final IHttpTaskProvder mSource;
    private final String mName;
    private final int mConcurrency;
    private volatile boolean mRunning;
    private Thread worker;

    public HttpTaskRunner(IHttpTaskProvder source, HttpClient client,
            int concurrency, String name) {
        mConcurrency = concurrency;
        mSemaphore = new Semaphore(concurrency);
        mClient = client;
        mSource = source;
        mName = name;
    }

    public void start() throws URISyntaxException {
        worker = new Thread(runner, mName);
        worker.start();
        logger.info("starting {}", mName);
    }

    public void stop() {
        mRunning = false;
        worker.interrupt();
        logger.info("stopping {}", mName);
    }

    public boolean isRunning() {
        return mRunning;
    }

    void markComplete(IHttpTask task, HttpResponse response) {
        mSemaphore.release();
    }

    public String toString() {
        return "HttpTaskRunner@" + mName + " running=" + mRunning
                + " concurrency = " + mConcurrency;
    }

    private Runnable runner = new Runnable() {
        public void run() {
            IHttpTask task = mSource.nextTask();
            while (mRunning && task != null) {
                try {
                    mSemaphore.acquire();
                    logger.trace("handle {}", task.getUri());
                    mClient.execGet(task.getUri(), task.getHeaders(),
                            new Hander(task, HttpTaskRunner.this));
                    task = mSource.nextTask();
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            logger.info("{} is stopped", mName);
            mRunning = false;
        }
    };
}
