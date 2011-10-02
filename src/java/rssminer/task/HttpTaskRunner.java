package rssminer.task;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static me.shenfeng.http.HttpClientConstant.CONNECTION_RESET;

import java.net.Proxy;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final String mName;
    private final Proxy mProxy;
    private final ConcurrentLinkedQueue<IHttpTask> mTaskQueue;
    private long startTime;
    private AtomicInteger mCounter = new AtomicInteger(0);
    private volatile boolean mRunning;
    private Thread mWorkerThread;
    private Thread mConsummerThread;
    private final Semaphore mSemaphore;
    private final BlockingQueue<HttpResponseFuture> mDones;
    private final Map<Integer, Integer> mStat = new TreeMap<Integer, Integer>();

    private Runnable mWorker = new Runnable() {
        public void run() {
            IHttpTask task = null;
            try {
                while (mRunning) {
                    if (mTaskQueue.size() == 0) {
                        List<IHttpTask> tasks = mProvider.getTasks();
                        if (tasks == null || tasks.size() == 0)
                            break;
                        mTaskQueue.addAll(tasks);
                    }
                    mSemaphore.acquire();
                    task = mTaskQueue.poll();
                    final HttpResponseFuture future = mHttp
                            .execGet(task.getUri(), task.getHeaders(),
                                    task.getProxy());
                    future.setAttachment(task); // keep state
                    future.addListener(new Runnable() {
                        public void run() {
                            mSemaphore.release();
                            try {
                                mDones.put(future);
                            } catch (InterruptedException ignore) {
                            }
                        }
                    });
                }
            } catch (InterruptedException ignore) {
            }
            logger.info("{} producer is stopped", mName);
            mRunning = false;
        }
    };

    private void recordStat(IHttpTask task, HttpResponse resp) {
        int code = resp.getStatus().getCode();
        if (code == 200 && task.getProxy() == mProxy)
            code = 275;
        Integer c = mStat.get(code);
        if (c == null)
            c = 0;
        mStat.put(code, ++c);
    }

    private Runnable mConsummer = new Runnable() {

        void retryReseted(final IHttpTask orgin) {
            mTaskQueue.offer(new IHttpTask() {
                public URI getUri() {
                    return orgin.getUri();
                }

                public Proxy getProxy() {
                    return mProxy;
                }

                public Map<String, Object> getHeaders() {
                    return orgin.getHeaders();
                }

                public Object doTask(HttpResponse response) throws Exception {
                    return orgin.doTask(response);
                }
            });
        }

        public void run() {
            // finish job at hand
            while (!mDones.isEmpty() || mRunning) {
                try {
                    HttpResponseFuture future = mDones.take();
                    IHttpTask task = (IHttpTask) future.getAttachment();
                    try {
                        HttpResponse resp = future.get();
                        recordStat(task, resp);
                        logger.trace("{} {}", resp.getStatus(), task.getUri());
                        if (resp == CONNECTION_RESET
                                && task.getProxy() != mProxy) {
                            retryReseted(task);
                        }
                        task.doTask(resp);
                    } catch (InterruptedException ignore) {
                    } catch (Exception e) {
                        logger.error(task.getUri().toString(), e);
                    } finally {
                        mCounter.incrementAndGet();
                    }
                } catch (InterruptedException ignore) {
                }
            }
            logger.info("{} consummer has stopped", mName);
            mRunning = false;
        }
    };

    public HttpTaskRunner(IHttpTaskProvder source, HttpClient client,
            int queueSize, String name, Proxy proxy) {
        mProvider = source;
        mProxy = proxy;
        mTaskQueue = new ConcurrentLinkedQueue<IHttpTask>();
        mSemaphore = new Semaphore(queueSize);
        mDones = new LinkedBlockingDeque<HttpResponseFuture>();
        mHttp = client;
        mStat.put(1200, queueSize);
        mStat.put(1300, (int) (startTime / 1000));
        mName = name;
    }

    public String getRate() {
        double m = (double) (currentTimeMillis() - startTime) / 60000;
        return format("%.2f", mCounter.get() / m);
    }

    public Map<Integer, Integer> getStat() {
        mStat.put(1000, mCounter.get());
        return mStat;
    }

    public boolean isRunning() {
        return mRunning;
    }

    public void start() {
        mRunning = true;
        mWorkerThread = new Thread(mWorker, mName + " Workder");
        mWorkerThread.start();

        mConsummerThread = new Thread(mConsummer, mName + " Consumer");
        mConsummerThread.start();

        startTime = currentTimeMillis();
        logger.info("starting {}", mName);
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;
            mWorkerThread.interrupt();
            mConsummerThread.interrupt();
            logger.info("{}, {} req/min, stoping", mName, getRate());
        }
    }

    public String toString() {
        return format("%s, %d req, %s req/min \n%s", mName, mCounter.get(),
                getRate(), mStat);
    }
}
