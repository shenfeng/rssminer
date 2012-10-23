/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.fetcher;

import java.net.Proxy;

public class HttpTaskRunnerConf {

    IHttpTasksProvder bulkProvider;
    IBlockingTaskProvider blockingProvider;
    int queueSize = 300;
    int blockingTimeOut = 5; // blocking provider timeout, ie redis blpop.
    // seconds

    String name;
    Proxy proxy = Proxy.NO_PROXY;

    public void setBlockingProvider(IBlockingTaskProvider blockingProvider) {
        this.blockingProvider = blockingProvider;
    }

    public void setBlockingTimeOut(int blockingTimeOut) {
        this.blockingTimeOut = blockingTimeOut;
    }

    public void setBulkProvider(IHttpTasksProvder bulkProvider) {
        this.bulkProvider = bulkProvider;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProxy(Proxy proxy) {
        if (proxy != null)
            this.proxy = proxy;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}
