package rssminer;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import rssminer.Utils.Info;
import rssminer.Utils.Pair;

class ExtractInfoHandler extends DefaultHandler {
    private final String base;
    private final Links linker;

    public ExtractInfoHandler(String base, Links linker) {
        this.base = base;
        this.linker = linker;
    }

    final static String A = "a";
    final static String HREF = "href";
    final static String LINK = "link";
    final static String RSS = "application/rss+xml";
    final static String TITLE = "title";
    final static String TYPE = "type";

    private boolean inTitle = false;
    List<URI> links;
    List<Pair> rsses;
    StringBuilder sb;

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (inTitle) {
            sb.append(ch, start, length);
        }
    }

    public Info get() {
        String title = sb == null ? null : sb.toString().trim();
        return new Info(rsses, links, title);
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        inTitle = false;
        if (LINK.equalsIgnoreCase(qName)) {
            int index = attrs.getIndex(TYPE);
            if (index != -1) {
                if (RSS.equalsIgnoreCase(attrs.getValue(index))) {
                    Pair p = linker.resolveRss(base, attrs.getValue(HREF),
                            attrs.getValue(TITLE));
                    if (p != null) {
                        if (rsses == null)
                            rsses = new ArrayList<Pair>(1);
                        rsses.add(p);
                    }
                }
            }
        } else if (A.equalsIgnoreCase(qName)) {
            int index = attrs.getIndex(HREF);
            if (index != -1) {
                String href = attrs.getValue(index).trim();
                URI u = linker.resoveAndClean(base, href);
                if (u != null) {
                    if (links == null)
                        links = new LinkedList<URI>();
                    links.add(u);
                }
            }
        } else if (TITLE.equalsIgnoreCase(qName)) {
            inTitle = true;
            if (sb == null) {
                sb = new StringBuilder(30);
            }
        }
    }
}

class ExtractTextHandler extends DefaultHandler {
    private int i, end;
    private boolean keep = true;
    private boolean prev = false, current = false;
    private StringBuilder sb = new StringBuilder();

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (keep) {
            end = start + length;
            for (i = start; i < end; ++i) {
                current = Character.isWhitespace(ch[i]);
                if (!prev || !current) {
                    sb.append(ch[i]);
                }
                prev = current;
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        keep = true;
    }

    public String getText() {
        return sb.toString();
    }

    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        if (localName.equalsIgnoreCase("script")
                || localName.equalsIgnoreCase("style")) {
            keep = false;
        }
    }
}

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

    static final ThreadLocal<Parser> parser = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            return new Parser();
        }
    };

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

    public static String extractText(String html) throws IOException,
            SAXException {
        Parser p = parser.get();
        ExtractTextHandler h = new ExtractTextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }
}