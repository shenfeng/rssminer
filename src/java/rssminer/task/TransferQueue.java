package rssminer.task;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import me.shenfeng.http.HttpResponseFuture;

public class TransferQueue {
    private final Semaphore mSemaphore;
    private final Queue<HttpResponseFuture> mDones;
    private final Queue<HttpResponseFuture> mPendings;

    public TransferQueue(int maxCapacity) {
        mSemaphore = new Semaphore(maxCapacity);
        mDones = new LinkedList<HttpResponseFuture>();
        mPendings = new LinkedList<HttpResponseFuture>();
    }

    private HttpResponseFuture waitGet() throws InterruptedException {
        while (true) {
            synchronized (mPendings) {
                final Iterator<HttpResponseFuture> iterator = mPendings
                        .iterator();
                while (iterator.hasNext()) {
                    HttpResponseFuture future = iterator.next();
                    future.checkTimeout(new Runnable() {
                        public void run() {
                            iterator.remove();
                        }
                    });
                }
            }

            synchronized (mDones) {
                mDones.wait(200);
                if (mDones.size() > 0) {
                    mSemaphore.release();
                    return mDones.poll();
                }
            }
        }
    }

    public int pendingSize() {
        return mPendings.size();
    }

    public HttpResponseFuture take() throws InterruptedException {
        synchronized (mDones) {
            if (mDones.size() > 0) {
                mSemaphore.release();
                return mDones.poll();
            }
        }
        return waitGet();
    }

    public void put(HttpResponseFuture future) throws InterruptedException {
        mSemaphore.acquire();
        synchronized (mPendings) {
            mPendings.add(future);
        }
    }

    public void done(HttpResponseFuture future) {
        synchronized (mDones) {
            synchronized (mPendings) {
                mPendings.remove(future);
                mDones.notify();
                mDones.add(future);
            }
        }
    }

}
