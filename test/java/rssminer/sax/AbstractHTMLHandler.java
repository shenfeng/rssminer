/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.sax;

import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractHTMLHandler extends DefaultHandler {

    StringBuilder sb;

    static final char SPACE = ' ';

    static final char START = '<';

    static final char END = '>';

    static final char SLASH = '/';
    static final char EQUAL = '=';
    static final char QUOTE = '\"';
    static final String[] UN_ClOSEABLE_TATS = new String[] { "img", "input", "hr", "br",
            "meta", "link" };
    static final String PRE = "pre";
    static final String SCRIPT = "script";

    static final String STYLE = "style";

    static final String[] PRESEVE_WHITESPACE_TAGS = new String[] { PRE, SCRIPT, STYLE };

    static boolean isQuoteNeeded(String val) {
        if (val.isEmpty() || val.length() > 10) {
            return true;
        } else {
            int i = val.length();
            while (--i >= 0) {
                char c = val.charAt(i);
                // http://www.cs.tut.fi/~jkorpela/qattr.html
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || c == '-' || c == '.') {
                } else {
                    return true;
                }
            }

            return false;
        }
    }

    public AbstractHTMLHandler(String html) {
        sb = new StringBuilder(html.length());

        if (html.length() > 100) {
            String h = html.substring(0, 20).toLowerCase();
            if (h.indexOf("doctype") != -1) {
                int end = html.indexOf('>');
                if (end < 150) { // copy doctype
                    sb.append(html.substring(0, end + 1).trim()).append('\n');
                }
            }
        }
    }

    public String get() {
        return sb.toString();
    }
}
