/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import me.shenfeng.http.HttpUtils;
import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import rssminer.db.SubItem;
import rssminer.jsoup.HtmlUtils;
import rssminer.search.Searcher;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.*;

import static java.lang.Character.OTHER_PUNCTUATION;

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
    public static final HttpClient CLIENT;
    public static final String USER_AGETNT = "Mozilla/5.0 (X11; Linux x86_64; rv:10.0.9) Gecko/20100101 Firefox/10.0.9 Iceweasel/10.0.9";
//    public static final String USER_AGETNT = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";
    public static final String[] NO_IFRAME = new String[]{"groups.google"}; // X-Frame-Options
    public static final String[] RESETED_DOMAINS = new String[]{
            "wordpress", "appspot", "emacsblog", "blogger", "blogspot",
            "mikemccandless", "feedproxy", "blogblog"};

    public static final String FINAL_URI = "X-final-uri";

    static {
        try {
            CLIENT = new HttpClient(new HttpClientConfig(60000, USER_AGETNT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    public static String mysqlSafe(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (!Character.isHighSurrogate(ch)
                    && !Character.isLowSurrogate(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
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

    public static void zrem(JedisPool pool, int userid, int rssid, int feedid) {
        Jedis jedis = pool.getResource();
        try {
            jedis.zrem(genKey(userid, rssid), Integer.toString(feedid)
                    .getBytes());
        } finally {
            pool.returnResource(jedis);
        }
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

    public static List<String> split(String str, int ch) {
        int begin = 0;
        for (; begin < str.length(); begin++) {
            if (str.charAt(begin) != ch) {
                break;
            }
        }
        if (begin > 0) {
            str = str.substring(begin);
            begin = 0;
        }

        int idx = str.indexOf(ch);
        if (idx == -1) {
            return Arrays.asList(str.trim());
        } else {
            ArrayList<String> strs = new ArrayList<String>(2);
            while (idx > -1) {
                String s = str.substring(begin, idx).trim();
                if (s.length() > 0) {
                    strs.add(s);
                }
                begin = idx + 1;
                idx = str.indexOf(ch, begin);
            }
            String s = str.substring(begin).trim();
            if (s.length() > 0) {
                strs.add(s);
            }
            return strs;
        }
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

    public static List<String> simpleSplit(String str) {
        ArrayList<String> strs = new ArrayList<String>(2);
        int start = -1;
        boolean splitter = true;
        char ch;
        for (int i = 0; i < str.length(); ++i) {
            ch = str.charAt(i);
            if (Character.isWhitespace(ch)
                    || Character.getType(ch) == OTHER_PUNCTUATION) {
                if (!splitter) {
                    strs.add(str.substring(start + 1, i));
                }
                splitter = true;
                start = i;
            } else {
                splitter = false;
            }
        }
        if (start != str.length() - 1) {
            strs.add(str.substring(start + 1));
        }
        return strs;
    }

    public static long simHash(String html, String title) {
        if (html == null || html.length() < 100) {
            return -1;
        }
        String text = HtmlUtils.text(html, true);
        if (text.length() < 50) {
            return -1;
        }
        int[] bits = new int[64];
        simHash(text, bits);
        if (title != null && !title.isEmpty()) {
            simHash(title, bits);
        }
        long fingerprint = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] > 0) {
                fingerprint += (1 << i);
            }
        }
        return fingerprint;
    }

    private static void simHash(String text, int[] bits) {
//        TokenStream stream = new SimpleMMsegTokenizer(DictHolder.dic,
//                new me.shenfeng.mmseg.StringReader(text));

        // TODO much better than above: steam and stop words removal
        TokenStream stream = Searcher.analyzer.tokenStream("",
                new me.shenfeng.mmseg.StringReader(text));
        CharTermAttribute c = stream.getAttribute(CharTermAttribute.class);
        try {
            while (stream.incrementToken()) {
                String term = new String(c.buffer(), 0, c.length());
                long code = MurmurHash.hash64(term);
                for (int j = 0; j < bits.length; j++) {
                    if (((code >>> j) & 0x1) == 0x1) {
                        bits[j] += 1;
                    } else {
                        bits[j] -= 1;
                    }
                }
            }
        } catch (IOException ignore) { // can not happen
        }
    }

    public static int hammingDistance(long x, long y) {
        int dist = 0;
        long val = x ^ y;
        while (val != 0) {
            ++dist;
            val &= val - 1;
        }
        return dist;
    }
}
