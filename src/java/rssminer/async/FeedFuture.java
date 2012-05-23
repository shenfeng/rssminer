package rssminer.async;

import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;
import static rssminer.Utils.minfiyHtml;

import java.net.URI;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;

import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;
import rssminer.Utils;
import clojure.lang.Keyword;

// fetch the orginal html
public class FeedFuture extends AbstractFuture {

    private int feedid = -1;
    private String proxyserver;

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
                try {
                    html = minfiyHtml(html, finalUrl);
                    html = Utils.rewrite(html, finalUrl, proxyserver);
                    headers = getHeaders("text/html; charset=utf8");
                    done(status, headers, html);
                } catch (Exception e) {
                    fail();
                }
            }
        }

        public void onThrowable(Throwable t) {
            fail();
        }
    }

    protected void doIt(URI uri) {
        if (!Utils.proxy(uri)) {
            String finalLink = uri.toString();
            if (feedid > 0) {
                // just do not cache it
                Connection con = null;
                PreparedStatement ps = null;
                try {
                    con = dataSource.getConnection();
                    ps = con.prepareStatement("update feeds set link = ? where id = ?");
                    ps.setString(1, finalLink);
                    ps.setInt(2, feedid);
                    ps.executeUpdate();
                } catch (Exception ignore) {
                } finally {
                    Utils.closeQuietly(con);
                }
            }
            Map<String, String> h = getHeaders(null);
            h.put(LOCATION, finalLink);
            // cache it, body is the final link
            // final link will be cached
            cacheIt(301, null, finalLink.getBytes());
            done(301, h, null); // Moved Permanently
            return;
        }

        if (++retryCount > MAX_RETRY) {
            fail();
            return;
        }
        try {
            CLIENT.get(uri, sendHeaders, proxy, new TextRespListener(
                    new ResultHandler(uri)));
        } catch (UnknownHostException e) {
            fail();
        }
    }

    public FeedFuture(int feedid, String uri, Map<String, String> headers,
            Map<Keyword, Object> config) {
        super(uri, headers, config);
        this.proxyserver = config.get(Utils.K_PROXY_SERVER) + "/p?u=";
        this.feedid = feedid;
    }
}
