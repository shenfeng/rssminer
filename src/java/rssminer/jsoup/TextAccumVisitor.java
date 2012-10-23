/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.jsoup;

import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

public class TextAccumVisitor implements NodeVisitor {

    private StringBuilder sb;
    private boolean withA;

    public void head(Node node, int depth) {
    }

    public TextAccumVisitor(StringBuilder sb, boolean withA) {
        this.withA = withA;
        this.sb = sb;
    }

    public void tail(Node node, int depth) {
        if (node instanceof TextNode) {
            String text = ((TextNode) node).getWholeText();

            if (!withA && node.parent().nodeName() == "a"
                    && text.startsWith("http")) {
                // do not care if this is <a href="href">href</a>
                return;
            }

            boolean lastWhiteSpace = sb.length() > 0;
            if (sb.length() > 0
                    && !Character.isWhitespace(sb.charAt(sb.length() - 1))) {
                sb.append(' ');
            }
            for (int i = 0; i < text.length(); ++i) {
                char c = text.charAt(i);
                if (Character.isWhitespace(c)) {
                    if (!lastWhiteSpace) {
                        sb.append(' ');
                    }
                    lastWhiteSpace = true;
                } else {
                    lastWhiteSpace = false;
                    sb.append(c);
                }
            }
        }
    }
}