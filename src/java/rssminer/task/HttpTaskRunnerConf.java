package rssminer.task;

import java.net.Proxy;

import me.shenfeng.http.HttpClient;

public class HttpTaskRunnerConf {

    IHttpTasksProvder bulkProvider;
    IBlockingTaskProvider blockingProvider;
    HttpClient client;
    int queueSize = 300;
    int bulkCheckInterval = 15 * 60 * 1000; // 15 min, mills
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

    public void setBulkCheckInterval(int bulkCheckInterval) {
        this.bulkCheckInterval = bulkCheckInterval;
    }

    public void setBulkProvider(IHttpTasksProvder bulkProvider) {
        this.bulkProvider = bulkProvider;
    }

    public void setClient(HttpClient client) {
        this.client = client;
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
