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

    private static List<Item> parse(AbstractHandler handler, String input)
            throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        parser.parse(new InputSource(new StringReader(input)), handler);

        return handler.getItems();
    }

    public static List<Item> parseOPML(String input)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(new OPMLHandler(), input);
    }

    public static List<Item> parseGReaderSubs(String input)
            throws ParserConfigurationException, SAXException, IOException {
        return parse(new GoogleExportHandler(), input);
    }

}
