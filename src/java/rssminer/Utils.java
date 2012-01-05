package rssminer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import rssminer.sax.ExtractFaviconHandler;
import rssminer.sax.ExtractInfoHandler;
import rssminer.sax.ExtractRssUriHandler;
import rssminer.sax.ExtractTextHandler;
import rssminer.sax.RewriteHandler;

public class Utils {
    final static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static class Info {
        static Info EMPTY = new Info(new ArrayList<Pair>(0),
                new ArrayList<URI>(0), null);

        public final List<URI> links;
        public final List<Pair> rssLinks;
        public final String title;

        public Info(List<Pair> rssLinks, List<URI> links, String title) {
            this.rssLinks = rssLinks;
            this.links = links;
            this.title = title;
        }
    }

    public static class Pair {
        public final String title;
        public final String url;

        public Pair(String url, String title) {
            this.url = url;
            this.title = title;
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

    public static String rewrite(String html, String urlBase)
            throws IOException, SAXException, URISyntaxException {
        Parser p = parser.get();
        RewriteHandler h = new RewriteHandler(html, urlBase);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static String rewrite(String html, String urlBase, String proxyURI)
            throws IOException, SAXException, URISyntaxException {
        Parser p = parser.get();
        RewriteHandler h = new RewriteHandler(html, urlBase, proxyURI);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static List<String> extractRssLink(String html, String base)
            throws IOException, SAXException {
        Parser p = parser.get();
        ExtractRssUriHandler h = new ExtractRssUriHandler(base);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getRsses();
    }

    public static String extractFaviconUrl(String html, String base)
            throws IOException, SAXException {
        Parser p = parser.get();
        ExtractFaviconHandler h = new ExtractFaviconHandler(base);
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.get();
    }

    public static Info extract(String html, String base, Links linker)
            throws IOException, SAXException {
        try {
            Parser p = parser.get();
            ExtractInfoHandler h = new ExtractInfoHandler(base, linker);
            p.setContentHandler(h);
            p.parse(new InputSource(new StringReader(html)));
            return h.get();
        } catch (IOException e) {
            // Pushback buffer overflow, not html?
            logger.trace(e.getMessage(), e);
            return Info.EMPTY;
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