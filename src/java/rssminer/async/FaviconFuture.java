package rssminer.async;

import static me.shenfeng.Utils.bodyStr;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import static me.shenfeng.Utils.bodyStr;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConstant;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

import rssminer.Utils;
import clojure.lang.IFn;

public class FaviconFuture extends AbstractFuture {

    private final String hostname;
    private static final Map<String, Object> EMPTY = new TreeMap<String, Object>();

    class Handler implements Runnable {
        private HttpResponseFuture f;
        private boolean img;

        public Handler(HttpResponseFuture f, boolean img) {
            this.f = f;
            this.img = img;
        }

        public void run() {
            try {
                HttpResponse r = f.get();
                int code = r.getStatus().getCode();
                if (code == 301 || code == 302) {
                    doIt(r.getHeader(Names.LOCATION), img);
                } else if (img) {
                    String type = r.getHeader(Names.CONTENT_TYPE);
                    // some website return gziped text
                    // http://blogs.oracle.com/
                    if (type != null && type.indexOf("text") != -1) {
                        r.setContent(ChannelBuffers.copiedBuffer(bodyStr(r),
                                CharsetUtil.UTF_8));
                    }
                    done(r); // img, not 301, 302, ok, get it
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
                        done(r); // error, accept
                    }
                }
            } catch (Exception e) {
                done(HttpClientConstant.UNKOWN_ERROR);
            }
        }
    }

    private void doIt(String uri, boolean img) throws URISyntaxException {
        if (uri == null)
            throw new NullPointerException("url can not be null");
        if (++retryCount < 4) {
            HttpResponseFuture f = client.execGet(new URI(uri), EMPTY, proxy);
            f.addListener(new Handler(f, img));
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
