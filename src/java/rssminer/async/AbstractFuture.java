package rssminer.async;

import java.net.Proxy;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.server.IListenableFuture;
import clojure.lang.IFn;

public class AbstractFuture implements IListenableFuture {

    static final int MAX_RETRY = 5;
    private volatile Runnable listener;
    protected IFn callback;
    protected volatile Object resp;
    protected Proxy proxy;

    protected volatile int retryCount = 0;
    protected HttpClient client;

    public void addListener(Runnable listener) {
        if (this.listener != null)
            throw new RuntimeException("listener is already added");
        this.listener = listener;
    }

    public Object get() {
        return callback.invoke(resp);
    }

    protected void done(Object r) {
        resp = r;
        if (listener != null) {
            listener.run();
        }
    }
}
