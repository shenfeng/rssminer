package rssminer.jsoup;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompactHtmlVisitor implements NodeVisitor {

    static Logger logger = LoggerFactory.getLogger(CompactHtmlVisitor.class);

    static final String[] UN_ClOSEABLE_TAGS = new String[] { "img", "input",
            "hr", "br", "meta", "link", "#text" };
    static final String[] IGNORE_TAGS = new String[] { "script", "style",
            "iframe", "#comment" };
    static final String[] IGNORE_ATTRS = new String[] { "class", "id",
            "style" };
    static final String[] KEEP_ATTRS = new String[] { "href", "src", "title",
            "type", "alt", "width", // feedburner track
            "height" };

    static final char SPACE = ' ';
    static final char START = '<';
    static final char END = '>';
    static final char SLASH = '/';
    static final char EQUAL = '=';
    static final char QUOTE = '\"';

    private StringBuilder sb;
    private URI baseUri;

    public static Map<String, Integer> all_attrs = new HashMap<String, Integer>();

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

    public void head(Node node, int depth) {
        String name = node.nodeName();
        for (String ignore : IGNORE_TAGS) {
            if (ignore.equals(name)) {
                return;
            }
        }

        if (node instanceof DataNode) {
            return;
        }

        if (node instanceof TextNode) {
            TextNode t = (TextNode) node;
            String html = t.toString();
            if(html.startsWith("\n")) { // remove string
                html = html.substring(1);
            }
            sb.append(html);
        } else {
            sb.append(START).append(name);
            // ignore any attribute
            Attributes attrs = node.attributes();
            for (Attribute attr : attrs) {
                String key = attr.getKey();
                for (String k : KEEP_ATTRS) {
                    if (k.equals(key)) {
                        sb.append(SPACE).append(key).append(EQUAL);
                        String val = resolve(name, k, attr.getValue());
                        if (HtmlUtils.isQuoteNeeded(val)) {
                            sb.append(QUOTE).append(val).append(QUOTE);
                        } else {
                            sb.append(val);
                        }
                        break;
                    }
                }
            }
            sb.append(END);
        }
    }

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

    public void tail(Node node, int depth) {
        if (node instanceof DataNode) {
            return;
        }
        String name = node.nodeName();
        for (String tag : UN_ClOSEABLE_TAGS) {
            if (tag.equals(name)) {
                return;
            }
        }
        sb.append(START).append(SLASH).append(name);
        sb.append(END);
    }

    public String toString() {
        return sb.toString();
    }
}
