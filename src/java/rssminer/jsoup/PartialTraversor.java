/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.jsoup;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

// copy and modified version of NodeTraversor
public class PartialTraversor extends Atranversor {

    protected boolean shouldIgnore(Node node) {
        String name = node.nodeName();
        for (String ignore : ignoreTags) {
            if (ignore.equals(name)) {
                return true;
            }
        }

        return false;
    }

    private final String[] ignoreTags;

    public PartialTraversor(NodeVisitor visitor, String[] ignoreTags) {
        super(visitor);
        this.ignoreTags = ignoreTags;
    }
}