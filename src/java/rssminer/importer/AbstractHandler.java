package rssminer.importer;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.helpers.DefaultHandler;

public class AbstractHandler extends DefaultHandler {

	protected List<Item> items = new ArrayList<Item>(8);

	public List<Item> getItems() {
		return items;
	}
}
