package rssminer.async;

import static me.shenfeng.Utils.bodyStr;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConstant;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.Utils;
import clojure.lang.IFn;

public class FaviconFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(FaviconFuture.class);

    private final String hostname;
    private static final Map<String, Object> EMPTY = new TreeMap<String, Object>();

    class Handler implements Runnable {
        private HttpResponseFuture f;
        private boolean img;
        private URI base;

        public Handler(HttpResponseFuture f, URI base, boolean img) {
            this.f = f;
            this.img = img;
            this.base = base;
        }

        public void run() {
            try {
                HttpResponse r = f.get();
                int code = r.getStatus().getCode();
                if (code == 301 || code == 302) {
                    String loc = r.getHeader(Names.LOCATION);
                    if (loc != null) {
                        doIt(base.resolve(loc).toString(), img);
                    } else {
                        logger.debug("null location in header: {}", base);
                        done(HttpClientConstant.UNKOWN_ERROR);
                    }

                } else if (img) {
                    String type = r.getHeader(Names.CONTENT_TYPE);
                    // some website return gziped text
                    // http://blogs.oracle.com/
                    if (type != null && type.indexOf("text") != -1) {
                        r.setContent(ChannelBuffers.copiedBuffer(bodyStr(r),
                                CharsetUtil.UTF_8));
                    }
                    done(r); // is image, not 301, 302, OK, get it
                } else {
                    if (code == 200) {
                        String url = Utils.extractFaviconUrl(bodyStr(r),
                                hostname);
                        if (url != null) {
                            doIt(url, true); // get url, do it
                        } else {
                            doIt(hostname + "/favicon.ico", true); // default
                        }
                    } else {
                        logger.debug("{}, status is {}", base, r.getStatus());
                        done(r); // error, accept
                    }
                }
            } catch (Exception e) {
                logger.trace("{} : {}", base, e);
                done(HttpClientConstant.UNKOWN_ERROR);
            }
        }
    }

    private void doIt(String uri, boolean img) throws URISyntaxException {
        if (uri == null)
            throw new NullPointerException("url can not be null");
        if (++retryCount < MAX_RETRY) {
            URI u = new URI(uri);
            HttpResponseFuture f = client.execGet(new URI(uri), EMPTY, proxy);
            f.addListener(new Handler(f, u, img));
        } else {
            done(HttpClientConstant.UNKOWN_ERROR);
        }
    }

    public FaviconFuture(HttpClient client, String hostname, Proxy proxy,
            IFn callback) throws URISyntaxException {
        this.hostname = "http://" + hostname;
        this.client = client;
        this.proxy = proxy;
        this.callback = callback;
        doIt(this.hostname, false);
    }

}
