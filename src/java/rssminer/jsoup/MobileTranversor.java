package rssminer.jsoup;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeVisitor;

public class MobileTranversor extends Atranversor {

    public MobileTranversor(NodeVisitor visitor) {
        super(visitor);
    }

    protected boolean shouldIgnore(Node node) {
        String name = node.nodeName();
        if ("table".equals(name)) {
            Element e = (Element) node;
            Elements trs = e.getElementsByTag("tr");
            // 无觅 您可能也喜欢：
            if (trs.size() == 3 && "无觅".equals(trs.last().text())) {
                // System.out.println("remove ============ 无觅" + node);
                return true;
            }
        } else if ("iframe".equals(name)) {
            String src = node.attr("src");
            if (src != null) {
                // if (src.contains("widget.weibo.com")) {
                // System.out.println("remove weibo================ " + node);
                // }
                return src.contains("widget.weibo.com");
            }
            // mobile does not support flash
        } else if ("object".equals(name)) {
//            System.out.println("remove object ==============" + node);
            return true;
        } else if ("div".equals(name)) {
            // 您可能感兴趣的文章：
            List<Node> childNodes = node.childNodes();
            if (childNodes.size() == 2) {
                Node p = childNodes.get(0);
                Node ul = childNodes.get(1);
                if ("p".equals(p.nodeName()) && "ul".equals(ul.nodeName())) {
                    Element p0 = (Element) p;
                    // if (p0.text().contains("可能感兴趣")) {
                    // System.out.println("remove------------------- " + node);
                    //                }
                    return p0.text().contains("可能感兴趣");
                }
            }
        }
        return false;
    }
}
