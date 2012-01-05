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

    private static final String[] unCloseableTags = new String[] { "img",
            "input", "hr", "br", "meta", "link" };

    private final StringBuilder sb;
    private final URI uriBase;
    private String proxyURI;
    private boolean hasBase = false;

    public RewriteHandler(String html, String uriBase)
            throws URISyntaxException {
        this(html, uriBase, null);
    }

    public RewriteHandler(String html, String uriBase, String proxyURl)
            throws URISyntaxException {
        sb = new StringBuilder(html.length());
        if (html.length() > 100) {
            String h = html.substring(0, 20).toLowerCase();
            if (h.indexOf("doctype") != -1) {
                int end = html.indexOf('>');
                if (end < 150) { // copy doctype
                    sb.append(html.substring(0, end + 1)).append('\n');
                }
            }
            int index = html.indexOf("<base ");
            if (index != -1 && index < 512) {
                hasBase = true; // naive tell if has base tag
            }
        }

        this.uriBase = new URI(uriBase);
        this.proxyURI = proxyURl;
    }

    public String get() {
        return sb.toString();
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        // trim need care: space matters for pre, and other
        sb.append(ch, start, length);
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
            sb.append(name).append(EQUAL);
            if (rw
                    && proxyURI != null
                    && ("src".equalsIgnoreCase(name) || "href"
                            .equalsIgnoreCase(name)) && val != null) {
                sb.append(QUOTE);
                try {
                    String e = URLEncoder.encode(uriBase.resolve(val)
                            .toString(), "utf8");
                    sb.append(proxyURI).append(e);
                } catch (UnsupportedEncodingException ignore) {
                }
                sb.append(QUOTE);
            } else {
                if (isQuoteNeeded(val)) {
                    sb.append(QUOTE).append(val).append(QUOTE);
                } else {
                    sb.append(val);
                }
            }
        }
        sb.append(END);

        if (!hasBase && "head".equalsIgnoreCase(qName)) {
            sb.append("<base href=\"").append(uriBase.toString())
                    .append("\">");
        }
    }

    private boolean isQuoteNeeded(String val) {
        if (val.isEmpty() || val.length() > 10) {
            return true;
        } else {
            int i = val.length();
            while (--i >= 0) {
                char c = val.charAt(i);
                // http://www.cs.tut.fi/~jkorpela/qattr.html
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z') || c == '-' || c == '.') {
                } else {
                    return true;
                }
            }

            return false;
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        String l = qName.toLowerCase();
        boolean close = true;
        for (String tag : unCloseableTags) {
            if (tag.equals(l)) {
                close = false;
                break;
            }
        }
        if (close) {
            sb.append(START).append(SLASH).append(qName);
            sb.append(END);
        }
    }
}
