package rssminer.jsoup;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

public abstract class Atranversor {

    protected abstract boolean shouldIgnore(Node node);

    private final NodeVisitor visitor;

    public Atranversor(NodeVisitor visitor) {
        this.visitor = visitor;
    }

    public void traverse(Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            boolean ignore = shouldIgnore(node);
            if (!ignore) {
                visitor.head(node, depth);
            }

            if (!ignore && node.childNodes().size() > 0) {
                node = node.childNode(0);
                depth++;
            } else {
                while (node.nextSibling() == null && depth > 0) {
                    if (!shouldIgnore(node)) {
                        visitor.tail(node, depth);
                    }
                    node = node.parent();
                    depth--;
                }
                if (!shouldIgnore(node)) {
                    visitor.tail(node, depth);
                }
                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }
}
