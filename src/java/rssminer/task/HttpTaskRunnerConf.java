package rssminer.task;

import java.net.Proxy;

import me.shenfeng.http.HttpClient;

public class HttpTaskRunnerConf {

    IHttpTaskProvder provider;
    HttpClient client;
    int queueSize = 300;
    String name;
    Proxy proxy = Proxy.NO_PROXY;
    boolean dnsPrefetch = false;

    public void setProvider(IHttpTaskProvder provider) {
        this.provider = provider;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setDnsPrefetch(boolean dnsPrefetch) {
        this.dnsPrefetch = dnsPrefetch;
    }
}
