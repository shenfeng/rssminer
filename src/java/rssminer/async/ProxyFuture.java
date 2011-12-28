package rssminer.async;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import me.shenfeng.http.HttpClient;
import me.shenfeng.http.HttpClientConstant;
import me.shenfeng.http.HttpResponseFuture;

import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.IFn;

public class ProxyFuture extends AbstractFuture {

    static Logger logger = LoggerFactory.getLogger(ProxyFuture.class);

    private final Map<String, Object> headers;

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
        if (uri == null)
            throw new NullPointerException("url can not be null");
        if (++retryCount < MAX_RETRY) {
            URI u = new URI(uri);
            HttpResponseFuture f = client.execGet(u, headers, proxy);
            f.addListener(new Handler(f, u));
        } else {
            done(HttpClientConstant.UNKOWN_ERROR);
        }
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
                        done(HttpClientConstant.UNKOWN_ERROR);
                    }
                } else {
                    done(r);
                }
            } catch (Exception e) {
                logger.trace("{} : {}", base, e);
                done(HttpClientConstant.UNKOWN_ERROR);
            }
        };
    }
}
