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
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.IBinaryHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IFn;

public class ProxyFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(ProxyFuture.class);

    private final Map<String, String> header;

    private class ProxyHandler implements IBinaryHandler {
        private final URI base;

        public ProxyHandler(URI u) {
            this.base = u;
        }

        private Map<String, String> transformHeader(Map<String, String> header) {
            Map<String, String> h = new TreeMap<String, String>();
            String ct = header.get(CONTENT_TYPE);
            if (ct != null) {
                h.put(CONTENT_TYPE, ct);
            }

            String cache = header.get(CACHE_CONTROL);
            if (cache != null) {
                h.put(CACHE_CONTROL, cache);
            } else {
                h.put(CACHE_CONTROL, "public, max-age=604800");
            }

            return h;
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
                done(status, transformHeader(headers), body);
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    public ProxyFuture(String uri, Map<String, String> headers, Proxy proxy,
            IFn callback) {
        this.proxy = proxy;
        // defensive copy
        this.header = new TreeMap<String, String>(headers);
        this.callback = callback;
        try {
            URI u = new URI(uri);
            if (u.getScheme() == "http") {
                doIt(u);
            } else {
                fail();
            }
        } catch (URISyntaxException e) {
            fail();
        }
    }

    private void doIt(URI uri) {
        // finalLink = uri;
        if (++retryCount < MAX_RETRY) {
            try {
                CLIENT.get(uri, header, proxy, new BinaryRespListener(
                        new ProxyHandler(uri)));
            } catch (UnknownHostException e) {
                fail();
            }
        } else {
            fail();
        }
    }
}
