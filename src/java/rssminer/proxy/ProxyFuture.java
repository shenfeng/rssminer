package rssminer.proxy;

import clojure.lang.Keyword;
import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.IBinaryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rssminer.Utils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;

import static me.shenfeng.http.HttpUtils.CONTENT_TYPE;
import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;

// binary
public class ProxyFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(ProxyFuture.class);

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
                cacheIt(status, ct,
                        Arrays.copyOf(bytes.get(), bytes.length()));
                done(status, getHeaders(ct), body);
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    public ProxyFuture(String uri, Map<String, String> headers,
                       Map<Keyword, Object> config) {
        super(uri, headers, config);
    }

    public void doIt(URI uri) {
        if (!Utils.proxy(uri)) {
            Map<String, String> h = getHeaders(null);
            h.put(LOCATION, uri.toString());
            done(301, h, null); // Moved Permanently
            return;
        }
        // finalLink = uri;
        if (++retryCount < MAX_RETRY) {
            try {
                CLIENT.get(uri, sendHeaders, proxy, new BinaryRespListener(
                        new ProxyHandler(uri)));
            } catch (UnknownHostException e) {
                fail();
            }
        } else {
            fail();
        }
    }
}
