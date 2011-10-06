package rssminer;

import static java.lang.Character.isLetter;

import java.io.IOException;
import java.io.StringReader;
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

class InfoHandler extends DefaultHandler {

    final static String A = "a";
    final static String HREF = "href";
    final static String LINK = "link";
    final static String RSS = "application/rss+xml";
    final static String TITLE = "title";
    final static String TYPE = "type";

    private boolean inTitle = false;
    List<String> links;
    List<Pair> rsses;
    StringBuilder sb;

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (inTitle) {
            sb.append(ch, start, length);
        }
    }

    public Info getInfo() {
        String title = sb == null ? null : sb.toString().trim();
        return new Info(rsses, links, title);
    }

    private boolean ignore(String href) {
        return href.length() == 0 || href.startsWith("#")
                || href.startsWith("mailto") || href.startsWith("javascript");
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        inTitle = false;
        if (LINK.equalsIgnoreCase(qName)) {
            int index = attrs.getIndex(TYPE);
            if (index != -1) {
                String href = attrs.getValue(HREF);
                if (RSS.equalsIgnoreCase(attrs.getValue(index))
                        && href != null && !ignore(href)) {
                    if (rsses == null)
                        rsses = new ArrayList<Pair>(1);
                    rsses.add(new Pair(href.trim(), attrs.getValue(TITLE)));
                }
            }
        } else if (A.equalsIgnoreCase(qName)) {
            int index = attrs.getIndex(HREF);
            if (index != -1) {
                String href = attrs.getValue(index).trim();
                if (!ignore(href)) {
                    if (links == null)
                        links = new LinkedList<String>();
                    links.add(href);
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

class TextHandler extends DefaultHandler {
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
        static Info EMPTY = new Info(new ArrayList<Pair>(1),
                new ArrayList<String>(1), null);
        public final List<String> links;
        public final List<Pair> rssLinks;
        public final String title;

        public Info(List<Pair> rssLinks, List<String> links, String title) {
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

    private static final ThreadLocal<Parser> parser = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            return new Parser();
        }
    };

    public static String genSnippet(String content, final int length) {
        if (content.length() < length)
            return content;
        else {
            int len = length;
            while (len < content.length() && isLetter(content.charAt(len)))
                ++len;
            return content.substring(0, len);
        }
    }

    public static Info extractInfo(String html) throws IOException,
            SAXException {
        try {
            Parser p = parser.get();
            InfoHandler h = new InfoHandler();
            p.setContentHandler(h);
            p.parse(new InputSource(new StringReader(html)));
            return h.getInfo();
        } catch (IOException e) {
            // Pushback buffer overflow, not html?
            logger.trace(e.getMessage(), e);
            return Info.EMPTY;
        }
    }

    public static String extractText(String html) throws IOException,
            SAXException {
        Parser p = parser.get();
        TextHandler h = new TextHandler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }
}