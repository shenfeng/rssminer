package rssminer.proxy;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.HttpUtils;

import redis.clients.jedis.Jedis;
import clojure.lang.Keyword;

public abstract class AbstractFuture extends CommonFuture {

    protected String initUri;

    private static byte[] STATUS = "s".getBytes();
    private static byte[] CT = "t".getBytes();
    private static byte[] BODY = "b".getBytes();

    public AbstractFuture(String uri, Map<String, String> headers,
            Map<Keyword, Object> config) {
        super(config, headers);
        initUri = uri;
        if (!tryCache()) {
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

    protected void cacheIt(int status, String ct, byte[] body) {
        Jedis j = null;
        try {
            j = redis.getResource();
            if (ct == null) {
                ct = "";
            }
            if (body == null) { // redis throw NPE when body is null;
                body = new byte[0];
            }
            Map<byte[], byte[]> m = new HashMap<byte[], byte[]>(4);
            m.put(STATUS, Integer.toString(status).getBytes());
            m.put(BODY, body);
            m.put(CT, ct.getBytes());
            byte[] key = getkey();
            j.hmset(key, m);
            j.expire(key, CACHE_TIME);
        } finally {
            if (j != null) {
                redis.returnResource(j);
            }
        }
    }

    protected abstract void doIt(URI uri);

    protected void fail() {
        cacheIt(404, null, null);
        done(404, null, null);
    }

    private byte[] getkey() {
        return ("c_" + initUri).getBytes();
    }

    protected boolean tryCache() {
        Jedis j = null;
        try {
            j = redis.getResource();
            Map<byte[], byte[]> m = j.hgetAll(getkey());
            if (m.isEmpty()) {
                return false;
            }
            int status = Integer.valueOf(new String(m.get(STATUS)));
            String ct = new String(m.get(CT));
            byte[] body = m.get(BODY);
            if (status == 301) { // body is the link
                Map<String, String> h = new TreeMap<String, String>();
                h.put(HttpUtils.LOCATION, new String(body));
                done(status, h, null);
            } else {
                done(status, getHeaders(ct), new ByteArrayInputStream(body));
            }
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (j != null) {
                redis.returnResource(j);
            }
        }
    }
}
