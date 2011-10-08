package rssminer.task;

import java.net.Proxy;

import rssminer.Links;

import me.shenfeng.http.HttpClient;

public class HttpTaskRunnerConf {

    IHttpTaskProvder provider;
    HttpClient client;
    Links links;
    int queueSize = 300;
    String name;
    Proxy proxy = Proxy.NO_PROXY;
    boolean dnsPrefetch = false;

    public void setClient(HttpClient client) {
        this.client = client;
    }

    public void setDnsPrefetch(boolean dnsPrefetch) {
        this.dnsPrefetch = dnsPrefetch;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProvider(IHttpTaskProvder provider) {
        this.provider = provider;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}
