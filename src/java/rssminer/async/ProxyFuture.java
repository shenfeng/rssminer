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

import clojure.lang.IFn;

public class ProxyFuture extends AbstractFuture {

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
        if (++retryCount < 4) {
            HttpResponseFuture f = client.execGet(new URI(uri), headers,
                    proxy);
            f.addListener(new Handler(f));
        } else {
            done(HttpClientConstant.UNKOWN_ERROR);
        }
    }

    class Handler implements Runnable {
        private HttpResponseFuture f;

        public Handler(HttpResponseFuture f) {
            this.f = f;
        }

        public void run() {
            try {
                HttpResponse r = f.get();
                int code = r.getStatus().getCode();
                if (code == 301 || code == 302) { // handle redirect
                    doIt(r.getHeader(Names.LOCATION));
                } else {
                    done(r);
                }
            } catch (Exception e) {
                done(HttpClientConstant.UNKOWN_ERROR);
            }
        };
    }
}
