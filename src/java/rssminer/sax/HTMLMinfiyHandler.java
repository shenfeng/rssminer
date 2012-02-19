package rssminer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class HTMLMinfiyHandler extends DefaultHandler {

    static final char SPACE = ' ';
    static final char START = '<';
    static final char END = '>';
    static final char SLASH = '/';
    static final char EQUAL = '=';
    static final char QUOTE = '\"';

    static final String[] unCloseableTags = new String[] { "img", "input",
            "hr", "br", "meta", "link" };

    static final String[] preseveWhiteSpaceTags = new String[] { "pre",
            "script", "style" };
    private StringBuilder sb;
    static final String PRE = "pre";
    static final String SCRIPT = "script";
    static final String STYLE = "style";
    private boolean squashWithspace = true;
    private boolean isInPre = false;
    private boolean isInScript = false;
    private boolean isInStyle = false;

    private boolean inComment = false; // for css
    private String uriBase;

    public HTMLMinfiyHandler(String html, String url) {
        sb = new StringBuilder(html.length());
        if (html.length() > 100) {
            String h = html.substring(0, 20).toLowerCase();
            if (h.indexOf("doctype") != -1) {
                int end = html.indexOf('>');
                if (end < 150) { // copy doctype
                    sb.append(html.substring(0, end + 1).trim()).append('\n');
                }
            }

            int index = html.indexOf("<base ");
            if (index == -1 && index < 512) {
                uriBase = url; // naive tell if has base tag
            }
        }
    }

    public String get() {
        return sb.toString();
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
            for (int i = start; i < total;) {
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
        }

        // } else if (isInScript) {
        // final int total = start + length;
        // String s = new String(ch, start, length);
        // System.out.println(s);
        // for (int i = start; i < total;) {
        // char last = ' ';
        // // skip leading space
        // while (i < total && Character.isWhitespace(ch[i])) {
        // ++i;
        // }
        // while (i < total && ch[i] != '\n') {
        // last = ch[i];
        // sb.append(last);
        // ++i;
        // }
        // // i am sure \n is not need
        // if (last == ';' || last == ',' || last == '{' || last == '}') {
        // } else if (i < total && ch[i] == '\n') {
        // sb.append('\n');
        // }
        // }
        // }
        else {
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

            if (name.equals("src")) { // prevent browser do request
                name = "data-src";
            } else if (name.equals("href")) {
                name = "data-href";
            }

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

        if (PRE.equals(qName)) {
            isInPre = false;
        } else if (SCRIPT.equals(qName)) {
            isInScript = false;
        } else if (STYLE.equals(qName)) {
            isInStyle = false;
        }
    }

    private static boolean isQuoteNeeded(String val) {
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
}
