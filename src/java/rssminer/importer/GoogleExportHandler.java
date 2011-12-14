package rssminer.importer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class GoogleExportHandler extends AbstractHandler {

	private int objectDepth = 0;
	private boolean isTitle = false;
	private boolean isLabel = true;
	private Item current = new Item();
	private boolean isUrl = true;

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (isTitle) {
			current.setTitle(new String(ch, start, length).trim());
		} else if (isLabel) {
			current.setCategory(new String(ch, start, length).trim());
		} else if (isUrl) {
			current.setUrl(new String(ch, start, length).trim());
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
		}
		if ("string".equals(qName)) {
			String name = att.getValue("name");
			if ("title".equals(name)) {
				isTitle = true;
			} else if ("htmlUrl".equals(name)) {
				isUrl = true;
			} else if ("label".equals(name)) {
				isLabel = true;
			}
		}
	}
}