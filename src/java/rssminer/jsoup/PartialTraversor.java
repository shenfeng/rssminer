package rssminer.jsoup;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

// copy and modified version of NodeTraversor
public class PartialTraversor {

    static final String[] IGNORE_TAGS = new String[] { "script", "style",
            "iframe", "link", "#comment" };

    private boolean ignoreNode(Node node) {
        String name = node.nodeName();
        for (String ignore : IGNORE_TAGS) {
            if (ignore.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private NodeVisitor visitor;

    public PartialTraversor(NodeVisitor visitor) {
        this.visitor = visitor;
    }

    public void traverse(Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            boolean ignore = ignoreNode(node);
            if (!ignore) {
                visitor.head(node, depth);
            }

            if (!ignore && node.childNodes().size() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    if (!ignoreNode(node)) {
                        visitor.tail(node, depth);
                    }
                    node = node.parent();
                    depth--;
                }
                if (!ignoreNode(node)) {
                    visitor.tail(node, depth);
                }
                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }
}