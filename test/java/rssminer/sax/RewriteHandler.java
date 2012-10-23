/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static java.net.URLEncoder.encode;
import static rssminer.Utils.reverse;

public class RewriteHandler extends AbstractHTMLHandler {

    private final URI uriBase;
    private String proxyURI;

    public RewriteHandler(String html, String uriBase, String proxyURl)
            throws URISyntaxException {
        super(html);
        this.uriBase = new URI(uriBase);
        this.proxyURI = proxyURl;
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        sb.append(ch, start, length);
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attrs) throws SAXException {
        qName = qName.toLowerCase();
        boolean rw = "script".equals(qName) || "img".equals(qName)
                || "link".equals(qName);
        sb.append(START).append(qName);
        int length = attrs.getLength();
        for (int i = 0; i < length; ++i) {
            String name = attrs.getQName(i).toLowerCase();
            String val = attrs.getValue(i);
            sb.append(SPACE);
            sb.append(name).append(EQUAL);
            if (rw && ("src".equals(name) || "href".equals(name))) {
                sb.append(QUOTE);
                try {
                    String url = uriBase.resolve(val).toString();
                    String e = encode(reverse(url), "utf8");
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
    }
}
