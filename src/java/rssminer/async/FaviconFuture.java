package rssminer.async;

import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;
import static rssminer.Utils.extractFaviconUrl;

import java.net.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.IBinaryHandler;
import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IFn;

public class FaviconFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(FaviconFuture.class);

    private final String hostname;

    private class FaviconHandler implements IBinaryHandler {
        private final URI base;

        public FaviconHandler(URI u) {
            this.base = u;
        }

        public void onSuccess(int status, Map<String, String> headers,
                DynamicBytes bytes) {
            if (status == 301 || status == 302) {
                String loc = headers.get(LOCATION);
                if (loc != null) {
                    doIt(base.resolve(loc), true);
                } else {
                    fail();
                }
            } else {
                done(status, headers,
                        Arrays.copyOf(bytes.get(), bytes.length()));
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    private class WebPageHandler implements ITextHandler {

        private final URI base;

        public WebPageHandler(URI u) {
            this.base = u;
        }

        public void onSuccess(int status, Map<String, String> headers,
                String body) {
            if (status == 301 || status == 302) {
                String loc = headers.get(LOCATION);
                if (loc != null) {
                    doIt(base.resolve(loc), true);
                } else {
                    fail();
                }
            } else if (status == 200) {
                try {
                    URI uri = extractFaviconUrl(body, base);
                    if (uri == null) {
                        uri = new URI(base.getScheme() + "://"
                                + base.getHost() + "/favicon.ico");
                    }
                    doIt(uri, true);
                } catch (Exception e) {
                    onThrowable(e);
                }
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    private void doIt(URI u, boolean img) {
        if (++retryCount < MAX_RETRY) {
            try {
                TreeMap<String, String> header = new TreeMap<String, String>();
                if (img) {
                    CLIENT.get(u, header, proxy, new BinaryRespListener(
                            new FaviconHandler(u)));
                } else {
                    CLIENT.get(u, header, proxy, new TextRespListener(
                            new WebPageHandler(u)));
                }
            } catch (Exception e) {
                fail();
            }
        } else {
            fail();
        }
    }

    public FaviconFuture(String hostname, Proxy proxy, IFn callback) {
        this.hostname = "http://" + hostname;
        this.proxy = proxy;
        this.callback = callback;
        try {
            doIt(new URI(this.hostname), false);
        } catch (Exception e) {
            fail();
        }
    }
}
