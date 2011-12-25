package rssminer.importer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class GoogleExportHandler extends AbstractHandler {

    private int objectDepth = 0;
    private boolean isTitle = false;
    private boolean isLabel = false;
    private Item current = new Item();
    private boolean isUrl = true;

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (isTitle) {
            current.setTitle(new String(ch, start, length).trim());
        } else if (isLabel) {
            current.setCategory(new String(ch, start, length).trim());
        } else if (isUrl) {
            String url = new String(ch, start, length).trim();
            if (url.startsWith("feed/")) {
                current.setUrl(url.substring(5));
            } else if (current.getUrl() != null) {
                current.setUrl(current.getUrl() + url);
            }
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (qName.equals("object") && --objectDepth == 1) {
            items.add(current);
            current = new Item();
        }

        isTitle = false;
        isUrl = false;
        isLabel = false;
    }

    public void startElement(String uri, String localName, String qName,
            Attributes att) throws SAXException {
        if (qName.equals("object")) {
            ++objectDepth;
        } else if ("string".equals(qName)) {
            String name = att.getValue("name");
            if ("title".equals(name)) {
                isTitle = true;
            } else if (objectDepth == 2 && "id".equals(name)) {
                isUrl = true;
            } else if ("label".equals(name)) {
                isLabel = true;
            }
        }
    }
}
