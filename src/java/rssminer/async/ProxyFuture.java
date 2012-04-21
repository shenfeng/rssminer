package rssminer.async;

import java.io.ByteArrayInputStream;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConstant;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IFn;
import clojure.lang.Keyword;

public class ProxyFuture extends AbstractFuture {
    static final Keyword resp_k = Keyword.intern("resp");
    static final Keyword status = Keyword.intern("status");
    static final Keyword header = Keyword.intern("headers");
    static final Keyword body = Keyword.intern("body");
    static final Keyword final_link_k = Keyword.intern("final-link");

    static Logger logger = LoggerFactory.getLogger(ProxyFuture.class);

    private final Map<String, Object> headers;
    private String finalLink;

    public ProxyFuture(HttpClient client, String uri,
            Map<String, Object> headers, Proxy proxy, IFn callback)
            throws URISyntaxException {
        this.client = client;
        this.proxy = proxy;
        this.headers = headers;
        this.callback = callback;
        doIt(uri);
    }

    private void doIt(String uri) throws URISyntaxException {
        finalLink = uri;
        if (uri == null)
            throw new NullPointerException("url can not be null");
        if (++retryCount < MAX_RETRY) {
            URI u = new URI(uri);
            HttpResponseFuture f = client.execGet(u, headers, proxy);
            f.addListener(new Handler(f, u));
        } else {
            finish(HttpClientConstant.UNKOWN_ERROR);
        }
    }

    private void finish(HttpResponse resp) {
        resp.removeHeader(Names.SET_COOKIE); // no cookie need
        resp.removeHeader("Server");
        Map<String, String> headers = new TreeMap<String, String>();
        for (String name : resp.getHeaderNames()) {
            headers.put(name, resp.getHeader(name));
        }
        headers.put("Cache-Control", "public, max-age=604800");

        Map<Keyword, Object> r = new TreeMap<Keyword, Object>();
        r.put(header, headers);
        r.put(status, resp.getStatus().getCode());
        r.put(body, new ByteArrayInputStream(resp.getContent().array()));
        r.put(final_link_k, finalLink);
        done(r);
    }

    class Handler implements Runnable {
        private HttpResponseFuture f;
        private URI base;

        public Handler(HttpResponseFuture f, URI base) {
            this.f = f;
            this.base = base;
        }

        public void run() {
            try {
                HttpResponse r = f.get();
                int code = r.getStatus().getCode();
                if (code == 301 || code == 302) { // handle redirect
                    String loc = r.getHeader(Names.LOCATION);
                    if (loc != null) {
                        doIt(base.resolve(loc).toString());
                    } else {
                        logger.debug("null location in header");
                        finish(HttpClientConstant.UNKOWN_ERROR);
                    }
                } else {
                    finish(r);
                }
            } catch (Exception e) {
                logger.trace("{} : {}", base, e);
                finish(HttpClientConstant.UNKOWN_ERROR);
            }
        };
    }
}
