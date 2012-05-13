package rssminer.importer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class OPMLHandler extends AbstractHandler {

    private String currentCat = null;
    private int outLineDepth = 0;

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("outline".equals(qName)) {
            --outLineDepth;
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if ("outline".equals(qName)) {
            ++outLineDepth;
            if (attributes.getValue("xmlUrl") != null) {
                Item item = new Item();
                item.setTitle(attributes.getValue("title"));
                item.setUrl(attributes.getValue("xmlUrl"));
                if (outLineDepth > 1) {
                    item.setCategory(currentCat);
                }
                items.add(item);
            } else {
                currentCat = attributes.getValue("text");
            }
        }
    }
}
