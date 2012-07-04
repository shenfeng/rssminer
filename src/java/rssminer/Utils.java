package rssminer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rssminer.sax.ExtractFaviconHandler;
import rssminer.sax.ExtractRssHandler;
import rssminer.sax.ExtractTextHandler;
import rssminer.sax.HTMLMinfiyHandler;
import rssminer.sax.RewriteHandler;
import clojure.lang.Keyword;

public class Utils {
    final static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final HttpClient CLIENT;
    public static final String USER_AGETNT = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";
    public static final String[] NO_IFRAME = new String[] { "groups.google" }; // X-Frame-Options
    public static final String[] RESETED_DOMAINS = new String[] {
            "wordpress", "appspot", "emacsblog", "blogger", "blogspot",
            "mikemccandless", "feedproxy", "blogblog" };

    public static final String FINAL_URI = "X-final-uri";

    // config key
    public static final Keyword K_PROXY = Keyword.intern("proxy");
    public static final Keyword K_PROXY_SERVER = Keyword
            .intern("proxy-server");
    public static final Keyword K_REDIS_SERVER = Keyword
            .intern("redis-server");
    public static final Keyword K_DATA_SOURCE = Keyword.intern("data-source");

    public static final Keyword K_EVENTS_THRESHOLD = Keyword
            .intern("events-threshold");

    static {
        try {
            CLIENT = new HttpClient(new HttpClientConfig(60000, USER_AGETNT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static final ThreadLocal<Parser> parser = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            Parser p = new Parser();
            try {
                p.setFeature(Parser.defaultAttributesFeature, false);
            } catch (Exception ignore) {
            }
            return p;
        }
    };

    public static void closeQuietly(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ignore) {
            }
        }
    }

    public static void closeQuietly(ResultSet con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ignore) {
            }
        }
    }

    public static void closeQuietly(Statement con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException ignore) {
            }
        }
    }

    public static URI extractFaviconUrl(String html, URI base)
            throws IOException, SAXException {
        Parser p = parser.get();
        ExtractFaviconHandler h = new ExtractFaviconHandler(base);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static String extractRssUrl(String html, URI base)
            throws IOException, SAXException {
        Parser p = parser.get();
        ExtractRssHandler h = new ExtractRssHandler(base);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getRss();
    }

    public static String extractText(String html) throws IOException,
            SAXException {
        Parser p = parser.get();
        ExtractTextHandler h = new ExtractTextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }

    public static byte[] genKey(int userID) {
        return ("fs:all:u_" + userID).getBytes(HttpUtils.UTF_8);
    }

    public static byte[] genKey(int userID, int rssID) {
        return ("fs:u_" + userID + "_s_" + rssID).getBytes(HttpUtils.UTF_8);
    }

    public static String minfiyHtml(String html, String url)
            throws IOException, SAXException {
        Parser p = parser.get();
        HTMLMinfiyHandler m = new HTMLMinfiyHandler(html, url);
        p.setContentHandler(m);
        p.parse(new InputSource(new StringReader(html)));
        return m.get();
    }

    public static boolean proxy(String uri) throws URISyntaxException {
        return proxy(new URI(uri));
    }

    public static boolean proxy(URI uri) {
        String host = uri.getHost();
        for (String h : NO_IFRAME) {
            if (host.contains(h)) {
                return true;
            }
        }
        for (String h : RESETED_DOMAINS) {
            if (host.contains(h)) {
                return true;
            }
        }
        return false;
    }

    public static String reverse(String str) {
        if (str != null) {
            return new StringBuilder(str).reverse().toString();
        } else {
            return null;
        }
    }

    public static String rewrite(String html, String urlBase, String proxyURI)
            throws IOException, SAXException, URISyntaxException {
        Parser p = parser.get();
        RewriteHandler h = new RewriteHandler(html, urlBase, proxyURI);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

}
