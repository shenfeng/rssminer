package rssminer.jsoup;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

// copy and modified version of NodeTraversor
public class PartialTraversor {

    private boolean ignoreNode(Node node) {
        String name = node.nodeName();
        for (String ignore : ignoreTags) {
            if (ignore.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private NodeVisitor visitor;
    private String[] ignoreTags;

    public PartialTraversor(NodeVisitor visitor, String[] ignoreTags) {
        this.visitor = visitor;
        this.ignoreTags = ignoreTags;
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