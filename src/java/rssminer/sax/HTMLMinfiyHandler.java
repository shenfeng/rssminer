package rssminer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HTMLMinfiyHandler extends AbstractHTMLHandler {

    private boolean squashWithspace = true;
    private boolean isInPre = false;
    private boolean isInScript = false;
    private boolean isInStyle = false;

    private boolean inComment = false; // for css
    private String uriBase;

    public HTMLMinfiyHandler(String html, String url) {
        super(html);
        if (html.length() > 100) {
            int index = html.indexOf("<base ");
            if (index == -1 && index < 512) {
                uriBase = url; // naive tell if has base tag
            }
        }
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        squashWithspace = !isInPre && !isInScript && !isInStyle;
        if (squashWithspace) {
            boolean isLastWhiteSpace = false;
            for (int i = start; i < start + length; i++) {
                if (Character.isWhitespace(ch[i])) {
                    if (!isLastWhiteSpace) { // ignore adjacent whitespace
                        sb.append(SPACE);
                    }
                    isLastWhiteSpace = true;
                } else {
                    sb.append(ch[i]);
                    isLastWhiteSpace = false;
                }
            }
        } else if (isInStyle) {
            final int total = start + length;
            for (int i = start; i < total; ) {
                // skip leading space
                while (i < total && Character.isWhitespace(ch[i])) {
                    ++i;
                }
                while (i < total && ch[i] != '\n') {
                    if (ch[i] == '/' && ch[i + 1] == '*') {
                        inComment = true;
                        ++i;
                    } else if (ch[i] == '*' && ch[i + 1] == '/') {
                        inComment = false;
                        ++i;
                    } else if (!inComment) {
                        sb.append(ch[i]);
                    }
                    ++i;
                }
                // \n is not needed
            }
        } else {
            sb.append(ch, start, length);
        }
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attrs) throws SAXException {
        qName = qName.toLowerCase();
        sb.append(START).append(qName);
        int length = attrs.getLength();
        for (int i = 0; i < length; ++i) {
            String name = attrs.getQName(i).toLowerCase();
            String val = attrs.getValue(i);
            sb.append(SPACE);
            sb.append(name).append(EQUAL);
            if (isQuoteNeeded(val)) {
                sb.append(QUOTE).append(val).append(QUOTE);
            } else {
                sb.append(val);
            }
        }
        sb.append(END);
        if (PRE.equals(qName)) {
            isInPre = true;
        } else if (SCRIPT.equals(qName)) {
            isInScript = true;
        } else if (STYLE.equals(qName)) {
            isInStyle = true;
        }

        if (uriBase != null && "head".equals(qName)) {
            sb.append("<base href=\"").append(uriBase).append("\">");
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        String l = qName.toLowerCase();
        boolean close = true;
        for (String tag : UN_ClOSEABLE_TATS) {
            if (tag.equals(l)) {
                close = false;
                break;
            }
        }
        if (close) {
            sb.append(START).append(SLASH).append(qName);
            sb.append(END);
        }

        if (PRE.equals(qName)) {
            isInPre = false;
        } else if (SCRIPT.equals(qName)) {
            isInScript = false;
        } else if (STYLE.equals(qName)) {
            isInStyle = false;
        }
    }
}
