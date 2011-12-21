package rssminer.sax;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import rssminer.Links;
import rssminer.Utils.Info;
import rssminer.Utils.Pair;

public class ExtractInfoHandler extends DefaultHandler {
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