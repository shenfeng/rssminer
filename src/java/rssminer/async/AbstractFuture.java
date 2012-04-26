package rssminer.async;

import java.net.Proxy;
import java.util.Map;

import me.shenfeng.http.server.IListenableFuture;
import clojure.lang.IFn;

public class AbstractFuture implements IListenableFuture {

    static final int MAX_RETRY = 5;
    private volatile Runnable listener;
    protected IFn callback;
    private volatile Object resp;
    protected Proxy proxy;
    protected volatile int retryCount = 0;

    public void addListener(Runnable listener) {
        if (this.listener != null)
            throw new RuntimeException("listener is already added");
        this.listener = listener;
    }

    public Object get() {
        return resp;
    }

    protected void done(int status, Map<String, String> headers, Object bytes) {
        // resp is is what return to http server;
        resp = callback.invoke(status, headers, bytes);
        if (listener != null) {
            listener.run(); // listener should call get()
        }
    }

    protected void fail() {
        done(404, null, null);
    }
}
