package rssminer.jsoup;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompactHtmlVisitor implements NodeVisitor {

    static Logger logger = LoggerFactory.getLogger(CompactHtmlVisitor.class);

    static final String[] KEEP_ATTRS = new String[] { "href", "src", "title",
            "type", "alt", "width", // feedburner track
            "height" };

    static final char SPACE = ' ';
    static final char START = '<';
    static final char END = '>';
    static final char SLASH = '/';
    static final char EQUAL = '=';
    static final char QUOTE = '\"';

    static Map<Character, String> encode = EscapeMode.base.getMap();

    private static boolean preserveWhitespace(Node node) {
        while (node != null) {
            if (node instanceof Element
                    && ((Element) node).tag().preserveWhitespace()) {
                return true;
            } else {
                node = node.parent();
            }
        }
        return false;
    }

    private StringBuilder sb;
    private URI baseUri;

    public CompactHtmlVisitor(StringBuilder sb, String baseUri) {
        this.sb = sb;
        try {
            URI uri = new URI(baseUri);
            String h = uri.getHost();
            // http://feedproxy.google.com
            if (h != null && h.indexOf("proxy") != -1) {
            } else {
                this.baseUri = uri;
            }
        } catch (URISyntaxException e) {
        }
    }

    private String trimLeft(String html) { // most space is on the left
        int start = 0;
        for (int i = 0; i < html.length(); ++i) {
            char c = html.charAt(i);
            if (Character.isWhitespace(c) || c == 0xA0) { // nbsp
                start = i;
            } else {
                break;
            }
        }

        if (html.charAt(start) == 0xA0) {
            return html.substring(start + 1);
        } else {
            return html.substring(start); // left one space
        }
    }

    public void head(Node node, int depth) {
        String name = node.nodeName();
        if (node instanceof TextNode) {
            TextNode t = (TextNode) node;
            boolean squash = !preserveWhitespace(t.parent());
            boolean lastWhiteSpace = false;

            // TODO, optimize it. leading \n can not be removed #144490
            // String html = t.toString();
            String html = t.getWholeText();
            if (squash) {
                html = trimLeft(html);
            }
            for (int i = 0; i < html.length(); ++i) {
                char c = html.charAt(i);
                if (squash) {
                    if (Character.isWhitespace(c)) {
                        if (!lastWhiteSpace) {
                            sb.append(' ');
                        }
                        lastWhiteSpace = true;
                        continue;
                    } else {
                        lastWhiteSpace = false;
                    }
                }

                String encoded = encode.get(c);
                if (encoded != null) {
                    sb.append('&').append(encoded).append(';');
                } else {
                    sb.append(c);
                }
            }
        } else {
            sb.append(START).append(name);
            Attributes attrs = node.attributes();
            if ("iframe".equals(name) || "object".equals(name)
                    || "embed".equals(name)) {
                // keep all attrs
                for (Attribute attr : attrs) {
                    addAttr(name, attr.getKey(), attr.getValue());
                }
            } else {
                for (Attribute attr : attrs) {
                    String key = attr.getKey();
                    // ignore unknown attribute
                    for (String k : KEEP_ATTRS) {
                        if (k.equals(key)) {
                            addAttr(name, k, attr.getValue());
                            break;
                        }
                    }
                }
            }
            sb.append(END);
        }
    }

    private void addAttr(String name, String k, String val) {
        sb.append(SPACE).append(k);
        if (!val.isEmpty()) {
            sb.append(EQUAL);
            val = resolve(name, k, val);
            if (HtmlUtils.isQuoteNeeded(val)) {
                sb.append(QUOTE).append(val).append(QUOTE);
            } else {
                sb.append(val);
            }
        }
    }

    private String resolve(String node, String key, String val) {
        if (this.baseUri == null) {
            return val;
        }
        if (val.isEmpty()) {
            return val;
        }

        if ("img".equals(node) && "src".equals(key)) {
            try {
                if (!val.startsWith("http") && !val.startsWith("data:")) {
                    val = baseUri.resolve(val).toString();
                }
            } catch (Exception e) {
            }
        }
        return val;
    }

    public void tail(Node node, int depth) {
        if (node instanceof TextNode) {
            return;
        }

        if (node instanceof Element && ((Element) node).tag().isEmpty()) {
            return;
        }

        String name = node.nodeName();
        sb.append(START).append(SLASH).append(name);
        sb.append(END);
    }

    public String toString() {
        return sb.toString();
    }
}
