package rssminer.importer;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Parser {

    public static List<Item> parseGReaderSubs(String input)
            throws ParserConfigurationException, SAXException, IOException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        GoogleExportHandler handler = new GoogleExportHandler();
        parser.parse(new InputSource(new StringReader(input)), handler);

        return handler.getItems();
    }
}
