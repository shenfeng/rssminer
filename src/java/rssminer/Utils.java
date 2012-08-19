package rssminer;

import clojure.lang.Keyword;
import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;
import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import rssminer.db.SubItem;
import rssminer.sax.RewriteHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class GoogleExportHandler extends DefaultHandler {

    protected List<SubItem> items = new ArrayList<SubItem>(8);

    private int objectDepth = 0;

    private boolean isTitle = false;
    private boolean isLabel = false;
    private SubItem current = new SubItem();
    private boolean isUrl = true;

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (isTitle) {
            current.setTitle(new String(ch, start, length).trim());
        } else if (isLabel) {
            current.setCategory(new String(ch, start, length).trim());
        } else if (isUrl) {
            String url = new String(ch, start, length).trim();
            if (url.startsWith("feed/")) {
                current.setUrl(url.substring(5));
            } else if (current.getUrl() != null) {
                current.setUrl(current.getUrl() + url);
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals("object") && --objectDepth == 1) {
            items.add(current);
            current = new SubItem();
        }

        isTitle = false;
        isUrl = false;
        isLabel = false;
    }

    public List<SubItem> getItems() {
        return items;
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes att) throws SAXException {
        if (qName.equals("object")) {
            ++objectDepth;
        } else if ("string".equals(qName)) {
            String name = att.getValue("name");
            if ("title".equals(name)) {
                isTitle = true;
            } else if (objectDepth == 2 && "id".equals(name)) {
                isUrl = true;
            } else if ("label".equals(name)) {
                isLabel = true;
            }
        }
    }
}

public class Utils {
    final static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final HttpClient CLIENT;
    public static final String USER_AGETNT = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";
    public static final String[] NO_IFRAME = new String[]{"groups.google"}; // X-Frame-Options
    public static final String[] RESETED_DOMAINS = new String[]{
            "wordpress", "appspot", "emacsblog", "blogger", "blogspot",
            "mikemccandless", "feedproxy", "blogblog"};

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

    public static byte[] genKey(int userID) {
        return ("fs:all:u" + userID).getBytes(HttpUtils.UTF_8);
    }

    public static byte[] genKey(int userID, int rssID) {
        return ("fs:u" + userID + ":s" + rssID).getBytes(HttpUtils.UTF_8);
    }

    public static byte[] genKey(int userID, List<Integer> rssIDs) {
        Collections.sort(rssIDs);
        StringBuilder sb = new StringBuilder(rssIDs.size() * 5 + 10);
        sb.append("fs:u").append(userID).append(":");
        for (Integer id : rssIDs) {
            sb.append(id).append("_");
        }
        sb.setLength(sb.length() - 1); // remove last _
        return sb.toString().getBytes(HttpUtils.UTF_8);
    }

    public static List<SubItem> parseGReaderSubs(String input)
            throws ParserConfigurationException, SAXException, IOException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        GoogleExportHandler handler = new GoogleExportHandler();
        parser.parse(new InputSource(new StringReader(input)), handler);

        return handler.getItems();
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

    public static String trimRemoveBom(String html) {
        html = html.trim();
        if (html.length() > 0) {
            char c = html.charAt(0);
            // bom, magic number
            if ((int) c == 65279) {
                html = html.substring(1);
            }
        }
        return html;
    }
}
