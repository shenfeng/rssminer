package rssminer.sax;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class RewriteHandler extends DefaultHandler {

    static final char SPACE = ' ';
    static final char START = '<';
    static final char END = '>';
    static final char SLASH = '/';
    static final char EQUAL = '=';
    static final char QUOTE = '\"';

    private final StringBuilder sb;
    private final URI uriBase;
    private String proxyURI;

    public RewriteHandler(String html, String uriBase)
            throws URISyntaxException {
        this(html, uriBase, null);
    }

    public RewriteHandler(String html, String uriBase, String proxyURl)
            throws URISyntaxException {
        sb = new StringBuilder(html.length());
        this.uriBase = new URI(uriBase);
        this.proxyURI = proxyURl;
    }

    public String get() {
        return sb.toString();
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        String s = new String(ch, start, length).trim();
        if (s.length() > 0) {
            sb.append(s);
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        boolean rw = "script".equalsIgnoreCase(qName)
                || "img".equalsIgnoreCase(qName)
                || "link".equalsIgnoreCase(qName);
        sb.append(START).append(qName);
        int length = attrs.getLength();
        for (int i = 0; i < length; ++i) {
            String name = attrs.getQName(i);
            String val = attrs.getValue(i);
            sb.append(SPACE);
            sb.append(name).append(EQUAL).append(QUOTE);
            if (rw
                    && proxyURI != null
                    && ("src".equalsIgnoreCase(name) || "href"
                            .equalsIgnoreCase(name)) && val != null) {
                try {
                    String e = URLEncoder.encode(uriBase.resolve(val)
                            .toString(), "utf8");
                    sb.append(proxyURI).append(e);
                } catch (UnsupportedEncodingException ignore) {
                }
            } else {
                sb.append(val);
            }
            sb.append(QUOTE);
        }
        sb.append(END);

        if ("head".equalsIgnoreCase(qName)) {
            sb.append("<base href=\"").append(uriBase.toString())
                    .append("\">");
        }

    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        sb.append(START).append(SLASH).append(qName);
        sb.append(END);
        sb.append("\n");
    }

}
