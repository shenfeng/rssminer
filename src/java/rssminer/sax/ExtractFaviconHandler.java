package rssminer.sax;

import java.net.URI;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtractFaviconHandler extends DefaultHandler {
    String url;
    private String urlbase;

    public ExtractFaviconHandler(String urlbase) {
        this.urlbase = urlbase;
    }

    public String get() {
        return url;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if ("link".equalsIgnoreCase(qName)) {
            int index = attributes.getIndex("rel");
            if (index != -1) {
                String val = attributes.getValue(index).toLowerCase();
                if (val.equals("icon") || val.indexOf(" icon") != -1) {
                    String v = attributes.getValue("href");
                    try {
                        url = new URI(urlbase).resolve(v).toString();
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }
}
