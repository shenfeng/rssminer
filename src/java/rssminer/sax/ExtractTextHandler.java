package rssminer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtractTextHandler extends DefaultHandler {
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