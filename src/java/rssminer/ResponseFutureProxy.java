package rssminer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.handler.codec.http.HttpResponse;

import ring.adapter.netty.HttpResponseFuture;

public class ResponseFutureProxy implements HttpResponseFuture {

	me.shenfeng.http.HttpResponseFuture proxyed;

	public ResponseFutureProxy(me.shenfeng.http.HttpResponseFuture future) {
		this.proxyed = future;
	}

	public boolean cancel(boolean mayInterruptIfRunning) {
		return proxyed.cancel(mayInterruptIfRunning);
	}

	public boolean isCancelled() {
		return proxyed.isCancelled();
	}

	public boolean isDone() {
		return proxyed.isDone();
	}

	public HttpResponse get() throws InterruptedException, ExecutionException {
		return proxyed.get();
	}

	public HttpResponse get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return proxyed.get(timeout, unit);
	}

	public void addListener(Runnable listener) {
		proxyed.addListener(listener);
	}
}
