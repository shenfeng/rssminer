package rssminer.sax;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtractRssUriHandler extends DefaultHandler {

    final static String LINK = "link";
    final static String HREF = "href";
    final static String RSS = "application/rss+xml";
    final static String TYPE = "type";
    private List<String> rsses = new ArrayList<String>(1);

    private final URI base;

    public ExtractRssUriHandler(String base) {
        this.base = URI.create(base);
    }

    public List<String> getRsses() {
        return rsses;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        if (LINK.equalsIgnoreCase(qName)) {
            int index = attrs.getIndex(TYPE);
            if (index != -1 && RSS.equalsIgnoreCase(attrs.getValue(index))) {
                String v = attrs.getValue(HREF);
                rsses.add(base.resolve(v).toString());
            }
        }
    }
}
