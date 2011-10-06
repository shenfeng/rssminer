package rssminer.task;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpResponse;

public class RetryHttpTask implements IHttpTask {

    final IHttpTask mTask;
    final Proxy mProxy;
    final URI mUri;

    public RetryHttpTask(IHttpTask task, Proxy proxy, URI uri) {
        mTask = task;
        mProxy = proxy;
        mUri = uri;
    }

    public URI getUri() {
        if (mUri != null)
            return mUri;
        return mTask.getUri();
    }

    public Map<String, Object> getHeaders() {
        return mTask.getHeaders();
    }

    public Object doTask(HttpResponse response) throws Exception {
        return mTask.doTask(response);
    }

    public Proxy getProxy() {
        if (mProxy != null)
            return mProxy;
        return mTask.getProxy();
    }

    public int retryTimes() {
        if (mTask instanceof RetryHttpTask)
            return ((RetryHttpTask) mTask).retryTimes() + 1;
        return 1;
    }
}
