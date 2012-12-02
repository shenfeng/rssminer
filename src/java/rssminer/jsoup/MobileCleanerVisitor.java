package rssminer.jsoup;

import org.jsoup.nodes.TextNode;

public class MobileCleanerVisitor extends CompactHtmlVisitor {

    public MobileCleanerVisitor(StringBuilder sb, String baseUri) {
        super(sb, baseUri);
        this.baseUri = null; // no need to resolve again
    }

    public String[] getKeepAttr() {
        return KEEP_ATTRS;
    }

    protected void addTextNode(TextNode t) {
        String html = t.getWholeText();
        for (int i = 0; i < html.length(); ++i) {
            char c = html.charAt(i);
            String encoded = encode.get(c);
            if (encoded != null) {
                sb.append('&').append(encoded).append(';');
            } else {
                sb.append(c);
            }
        }
    }

    static final String[] KEEP_ATTRS = new String[] { "href", "src", "type", "alt" };
}
