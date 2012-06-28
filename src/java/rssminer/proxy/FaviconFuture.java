package rssminer.proxy;

import static me.shenfeng.http.HttpUtils.LOCATION;
import static rssminer.Utils.CLIENT;
import static rssminer.Utils.extractFaviconUrl;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import me.shenfeng.http.DynamicBytes;
import me.shenfeng.http.client.BinaryRespListener;
import me.shenfeng.http.client.IBinaryHandler;
import me.shenfeng.http.client.ITextHandler;
import me.shenfeng.http.client.TextRespListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.Utils;
import clojure.lang.Keyword;

public class FaviconFuture extends CommonFuture {

    static Logger logger = LoggerFactory.getLogger(FaviconFuture.class);

    private String hostname;

    private void noIcon() {
        finish(404, null, true); // cache failed
    }

    private void finish(int status, byte[] data, boolean cache) {
        if (cache) {
            Connection con = null;
            PreparedStatement ps = null;
            try {
                con = dataSource.getConnection();
                ps = con.prepareStatement("insert into favicon(favicon, code, hostname) values (?, ?, ?)");
                ps.setBytes(1, data);
                ps.setInt(2, status);
                ps.setString(3, hostname);
                ps.executeUpdate();
            } catch (SQLException ignore) {
            } finally {
                Utils.closeQuietly(con);
                Utils.closeQuietly(ps);
            }
        }
        if (data != null) {
            done(200, getHeaders("image/x-icon"), new ByteArrayInputStream(
                    data));
        } else {
            // browser js will do it right
            done(200, getHeaders(null), null);
        }
    }

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
                    noIcon();
                }
            } else {
                byte[] data = Arrays.copyOf(bytes.get(), bytes.length());
                finish(status, data, true);
            }
        }

        public void onThrowable(Throwable t) {
            noIcon();
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
                    noIcon();
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
                    noIcon();
                }
            }
        }

        public void onThrowable(Throwable t) {
            noIcon();
        }
    }

    private void doIt(URI u, boolean img) {
        if (++retryCount > MAX_RETRY) {
            noIcon();
            return;
        }
        try {
            if (img) {
                CLIENT.get(u, sendHeaders, proxy, new BinaryRespListener(
                        new FaviconHandler(u)));
            } else {
                CLIENT.get(u, sendHeaders, proxy, new TextRespListener(
                        new WebPageHandler(u)));
            }
        } catch (Exception e) {
            noIcon();
        }
    }

    private boolean tryCache() {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement("SELECT favicon, code FROM favicon WHERE hostname = ?");
            ps.setString(1, hostname);
            rs = ps.executeQuery();
            if (rs.next()) {
                int code = rs.getInt(2);
                if (code == 200) {
                    byte[] data = rs.getBytes(1);
                    done(200, getHeaders("image/x-icon"),
                            new ByteArrayInputStream(data));
                } else {
                    noIcon();
                }
                return true;
            }
        } catch (SQLException ignore) {
        } finally {
            Utils.closeQuietly(rs);
            Utils.closeQuietly(ps);
            Utils.closeQuietly(con);
        }
        return false;
    }

    public FaviconFuture(String hostname, Map<String, String> headers,
            Map<Keyword, Object> config) {
        super(config, headers);
        this.hostname = hostname;
        if (!tryCache()) {
            try {
                doIt(new URI("http://" + hostname), false);
            } catch (Exception e) {
                noIcon();
            }
        }
    }
}
