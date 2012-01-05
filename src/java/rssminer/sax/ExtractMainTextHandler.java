package rssminer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtractMainTextHandler extends DefaultHandler {

    final static String[] TAGS = new String[] { "h1", "h2", "h3", "h4", "h5",
            "h6", "p" };

    private boolean interesTag;
    private boolean isTitle;
    private String title = "";
    private StringBuilder sb = new StringBuilder();

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return sb.toString();
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (interesTag) {
            sb.append(ch, start, length);
        } else if (isTitle) {
            title = new String(ch, start, length);
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        qName = qName.toLowerCase();
        if (qName.equals("a")) {
            interesTag = false; // do not include a
        } else if (qName.equals("title") || qName.equals("h1")) {
            isTitle = true;
        } else {
            for (String tag : TAGS) {
                if (tag.equals(qName)) {
                    interesTag = true;
                    break;
                }
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        qName = qName.toLowerCase();
        isTitle = false;
        for (String tag : TAGS) {
            if (tag.equals(qName)) {
                interesTag = false;
                break;
            }
        }
    }
}
