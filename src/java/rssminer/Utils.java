package rssminer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;

import me.shenfeng.http.client.HttpClient;
import me.shenfeng.http.client.HttpClientConfig;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rssminer.sax.ExtractFaviconHandler;
import rssminer.sax.ExtractTextHandler;
import rssminer.sax.HTMLMinfiyHandler;
import rssminer.sax.RewriteHandler;

public class Utils {
    final static Logger logger = LoggerFactory.getLogger(Utils.class);
    public static final HttpClient CLIENT;
    public static final String USER_AGETNT = "Mozilla/5.0 (compatible; Rssminer/1.0; +http://rssminer.net)";

    static {
        try {
            CLIENT = new HttpClient(new HttpClientConfig(50000, USER_AGETNT));
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

    public static String extractFaviconUrl(String html, String base)
            throws IOException, SAXException {
        Parser p = parser.get();
        ExtractFaviconHandler h = new ExtractFaviconHandler(base);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static String minfiyHtml(String html, String url)
            throws IOException, SAXException {
        Parser p = parser.get();
        HTMLMinfiyHandler m = new HTMLMinfiyHandler(html, url);
        p.setContentHandler(m);
        p.parse(new InputSource(new StringReader(html)));
        return m.get();
    }

    public static String rewrite(String html, String urlBase, String proxyURI)
            throws IOException, SAXException, URISyntaxException {
        Parser p = parser.get();
        RewriteHandler h = new RewriteHandler(html, urlBase, proxyURI);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static String reverse(String str) {
        if (str != null) {
            return new StringBuilder(str).reverse().toString();
        } else {
            return null;
        }
    }

    public static double[] pick(double[] prefs, double likeRatio,
            double dislikeRatio) {
        int likeIndex = prefs.length - (int) (prefs.length * likeRatio);
        int disLikeIndex = (int) (prefs.length * dislikeRatio);
        likeIndex = likeIndex == prefs.length ? prefs.length - 1 : likeIndex;
        Arrays.sort(prefs);
        return new double[] { prefs[likeIndex], prefs[disLikeIndex] };
    }

    public static String extractText(String html) throws IOException,
            SAXException {
        Parser p = parser.get();
        ExtractTextHandler h = new ExtractTextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }
}