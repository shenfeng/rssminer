/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.fetcher;

import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;

public class RetryHttpTask implements IHttpTask {

    final IHttpTask mTask;
    final URI mUri;

    public RetryHttpTask(IHttpTask task, URI uri) {
        mTask = task;
        mUri = uri;
    }

    public URI getUri() {
        if (mUri != null)
            return mUri;
        return mTask.getUri();
    }

    public Map<String, Object> getHeaders() {
        // with empty header to retry (302)
        return new TreeMap<String, Object>();
    }

    public Proxy getProxy() {
        return mTask.getProxy();
    }

    public int retryTimes() {
        if (mTask instanceof RetryHttpTask)
            return ((RetryHttpTask) mTask).retryTimes() + 1;
        return 1;
    }

    public Object doTask(int status, Map<String, String> headers, String body) {
        return mTask.doTask(status, headers, body);
    }

    public Object onThrowable(Throwable t) {
        return mTask.onThrowable(t);
    }

    public String toString() {
        return "retry: " + retryTimes() + " " + getUri() + " " + getHeaders();
    }
}
