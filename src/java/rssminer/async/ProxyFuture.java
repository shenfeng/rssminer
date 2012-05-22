package rssminer.async;

import static me.shenfeng.http.HttpUtils.CACHE_CONTROL;
import static me.shenfeng.http.HttpUtils.CONTENT_TYPE;
import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;

import java.io.ByteArrayInputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.IBinaryHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import clojure.lang.IFn;

public class ProxyFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(ProxyFuture.class);

    private final Map<String, String> requestHeaders;

    private final JedisPool redis;
    private final String initUri;

    private static final int CACHE_TIME = 3600 * 48; // cache 2 day;
    private static final String CACHE = "public, max-age=" + CACHE_TIME;

    private static byte[] STATUS = "s".getBytes();
    private static byte[] CT = "t".getBytes();
    private static byte[] BODY = "b".getBytes();

    protected void fail() {
        cacheIt(initUri, 404, null, new byte[0]);
        super.done(404, getHeaders(null), null);
    }

    private Map<String, String> getHeaders(String ct) {
        Map<String, String> h = new TreeMap<String, String>();
        if (ct != null && !ct.isEmpty()) {
            h.put(CONTENT_TYPE, ct);
        }
        h.put(CACHE_CONTROL, CACHE);
        return h;
    }

    private class ProxyHandler implements IBinaryHandler {
        private final URI base;

        public ProxyHandler(URI u) {
            this.base = u;
        }

        public void onSuccess(int status, Map<String, String> headers,
                DynamicBytes bytes) {
            if (status == 301 || status == 302) {
                String loc = headers.get(LOCATION);
                if (loc != null) {
                    doIt(base.resolve(loc));
                } else {
                    fail();
                }
            } else {
                ByteArrayInputStream body = new ByteArrayInputStream(
                        bytes.get(), 0, bytes.length());
                String ct = headers.get(CONTENT_TYPE);
                cacheIt(initUri, status, ct,
                        Arrays.copyOf(bytes.get(), bytes.length()));
                done(status, getHeaders(ct), body);
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    private byte[] getkey(String uri) {
        return ("c_" + uri).getBytes();
    }

    private void cacheIt(String uri, int status, String ct, byte[] body) {
        Jedis j = null;
        try {
            j = redis.getResource();
            if (ct == null) {
                ct = "";
            }
            Map<byte[], byte[]> m = new HashMap<byte[], byte[]>(4);
            m.put(STATUS, Integer.toString(status).getBytes());
            m.put(BODY, body);
            m.put(CT, ct.getBytes());
            byte[] key = getkey(uri);
            j.hmset(key, m);
            j.expire(key, CACHE_TIME);
        } finally {
            if (j != null) {
                redis.returnResource(j);
            }
        }
    }

    private boolean tryCache(String uri) {
        Jedis j = null;
        try {
            j = redis.getResource();
            Map<byte[], byte[]> m = j.hgetAll(getkey(uri));
            if (m.isEmpty()) {
                return false;
            }
            int status = Integer.valueOf(new String(m.get(STATUS)));
            String ct = new String(m.get(CT));
            byte[] body = m.get(BODY);
            done(status, getHeaders(ct), new ByteArrayInputStream(body));
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (j != null) {
                redis.returnResource(j);
            }
        }
    }

    public ProxyFuture(String uri, Map<String, String> headers, Proxy proxy,
            JedisPool redis, IFn callback) {
        this.redis = redis;
        this.proxy = proxy;
        this.initUri = uri;
        // defensive copy
        this.requestHeaders = new TreeMap<String, String>(headers);
        this.callback = callback;
        if (!tryCache(uri)) {
            try {
                URI u = new URI(uri);
                if ("http".equals(u.getScheme())) {
                    doIt(u);
                } else {
                    fail();
                }
            } catch (URISyntaxException e) {
                fail();
            }
        }
    }

    private void doIt(URI uri) {
        // finalLink = uri;
        if (++retryCount < MAX_RETRY) {
            try {
                CLIENT.get(uri, requestHeaders, proxy,
                        new BinaryRespListener(new ProxyHandler(uri)));
            } catch (UnknownHostException e) {
                fail();
            }
        } else {
            fail();
        }
    }
}
