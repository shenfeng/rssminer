package rssminer.proxy;

import static me.shenfeng.http.HttpUtils.CACHE_CONTROL;
import static me.shenfeng.http.HttpUtils.CONTENT_TYPE;
import static rssminer.Utils.K_DATA_SOURCE;
import static rssminer.Utils.K_PROXY;
import static rssminer.Utils.K_REDIS_SERVER;

import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import me.shenfeng.http.server.IListenableFuture;
import me.shenfeng.http.server.ServerConstant;
import redis.clients.jedis.JedisPool;
import clojure.lang.Keyword;

public class CommonFuture implements IListenableFuture {

    static final int MAX_RETRY = 5;

    static final int CACHE_TIME = 3600 * 24 * 5; // cache 5 day;
    static final String CACHE_VALUE = "public, max-age=" + CACHE_TIME;

    private volatile Runnable listener;
    private volatile Object resp;
    protected volatile int retryCount = 0;

    protected final Map<String, String> sendHeaders;

    protected final Proxy proxy;
    protected final JedisPool redis;
    protected final DataSource dataSource;

    public void addListener(Runnable listener) {
        if (this.listener != null)
            throw new RuntimeException("listener is already added");
        if (resp != null) {
            listener.run(); // listener will call get(), and get the real resp
        } else {
            this.listener = listener;
        }
    }

    protected Map<String, String> getHeaders(String ct) {
        Map<String, String> h = new TreeMap<String, String>();
        if (ct != null && !ct.isEmpty()) {
            h.put(CONTENT_TYPE, ct);
        }
        h.put(CACHE_CONTROL, CACHE_VALUE);
        return h;
    }

    public CommonFuture(Map<Keyword, Object> config,
            Map<String, String> headers) {
        this.proxy = (Proxy) (config.get(K_PROXY));
        this.redis = (JedisPool) config.get(K_REDIS_SERVER);
        this.dataSource = (DataSource) config.get(K_DATA_SOURCE);
        this.sendHeaders = new TreeMap<String, String>(headers);

        if (proxy == null || redis == null || dataSource == null) {
            throw new NullPointerException(
                    "proxy, redis, datasource can's be null");
        }
    }

    public Object get() {
        return resp;
    }

    protected void done(int status, Map<String, String> headers, Object body) {
        Map<Keyword, Object> r = new HashMap<Keyword, Object>(6);
        r.put(ServerConstant.STATUS, status);
        r.put(ServerConstant.HEADERS, headers);
        r.put(ServerConstant.BODY, body);
        resp = r;
        if (listener != null) {
            listener.run(); // will call get, get resp
        }
    }
}
