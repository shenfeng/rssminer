package rssminer.async;

import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;
import static rssminer.Utils.FINAL_URI;
import static rssminer.Utils.minfiyHtml;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;
import clojure.lang.IFn;

// fetch the orginal html
public class FetchFuture extends AbstractFuture {

    private Map<String, String> header;

    private class ResultHandler implements ITextHandler {
        private final URI uri;

        public ResultHandler(URI u) {
            this.uri = u;
        }

        public void onSuccess(int status, Map<String, String> headers,
                String html) {
            if (status == 301 || status == 302) {
                String loc = headers.get(LOCATION);
                if (loc != null) {
                    doIt(uri.resolve(loc));
                } else {
                    fail();
                }
            } else {
                String finalUrl = uri.toString();
                headers.put(FINAL_URI, finalUrl);
                try {
                    html = minfiyHtml(html, finalUrl);
                } catch (Exception e) {
                    fail();
                }
                done(status, headers, html);
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    private void doIt(URI uri) {
        if (++retryCount < MAX_RETRY) {
            try {
                CLIENT.get(uri, header, proxy, new TextRespListener(
                        new ResultHandler(uri)));
            } catch (UnknownHostException e) {
                fail();
            }
        } else {
            fail();
        }
    }

    public FetchFuture(String uri, Map<String, String> headers, Proxy proxy,
            IFn callback) {
        this.proxy = proxy;
        // defensive copy
        this.header = new TreeMap<String, String>(headers);
        this.callback = callback;
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
