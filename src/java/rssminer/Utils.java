package rssminer;

import static org.jboss.netty.util.CharsetUtil.UTF_8;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.Charset;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Utils {
    private static final String CS = "charset=";

    private static final ThreadLocal<Parser> parser = new ThreadLocal<Parser>() {
        protected Parser initialValue() {
            return new Parser();
        }
    };

    public static String getPath(URI uri) {
        String path = uri.getPath();

        if ("".equals(path))
            path = "/";
        return path + "?" + uri.getRawQuery();
    }

    public static String getHost(URI uri) {
        String host = uri.getHost();
        if (host == null)
            return "localhost";
        else
            return host;
    }

    public static int getPort(URI uri) {
        int port = uri.getPort();
        if (port == -1) {
            port = 80;
        }
        return port;
    }

    public static Charset parseCharset(String type) {
        if (type != null) {
            int i = type.indexOf(CS);
            if (i != -1) {
                String charset = type.substring(i + CS.length()).trim();
                return Charset.forName(charset);
            }
        }
        return UTF_8;
    }

    public static String extractText(String html) throws IOException,
            SAXException {
        Parser p = parser.get();
        Handler h = new Handler();
        p.setContentHandler(h);
        p.parse(new InputSource(new StringReader(html)));
        return h.getText();
    }
}

class Handler extends DefaultHandler {
    private StringBuilder sb = new StringBuilder();
    private boolean keep = true;
    private boolean prev = false, current = false;
    private int i, end;

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

    public String getText() {
        return sb.toString();
    }

    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        if (localName.equalsIgnoreCase("script")) {
            keep = false;
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        keep = true;
    }
}