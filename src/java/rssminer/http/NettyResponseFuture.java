package rssminer.http;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.handler.codec.http.HttpResponse;

public class NettyResponseFuture implements Future<HttpResponse> {

    private final CountDownLatch mLatch = new CountDownLatch(1);
    private volatile HttpResponse mResponse;
    private AsyncHanlder mHanlder;

    public NettyResponseFuture(AsyncHanlder hanlder) {
        mHanlder = hanlder;
    }

    public void done(HttpResponse response) {
        mResponse = response;
        mLatch.countDown();
        mHanlder.onCompleted(response);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return mResponse != null;
    }

    public HttpResponse get() throws InterruptedException, ExecutionException {
        boolean await = mLatch.await(4000, TimeUnit.MILLISECONDS);
        if (!await) {
            System.out.println("wait " + await);
        }
        return mResponse;
    }

    public HttpResponse get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

}
