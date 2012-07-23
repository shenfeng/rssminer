package rssminer.jsoup;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeTraversor;

public class HtmlUtils {

    public static String compact(String html, String baseUri) {
        StringBuilder sb = new StringBuilder(html.length());
        CompactHtmlVisitor vistor = new CompactHtmlVisitor(sb, baseUri);
        Document doc = Jsoup.parse(html, baseUri);
        List<Node> nodes = doc.body().childNodes();
        for (Node e : nodes) {
            new NodeTraversor(vistor).traverse(e);
        }
        return vistor.toString();
    }

    public static boolean isQuoteNeeded(String val) {
        if (val.isEmpty() || val.length() > 10) {
            return true;
        } else {
            int i = val.length();
            while (--i >= 0) {
                char c = val.charAt(i);
                // http://www.cs.tut.fi/~jkorpela/qattr.html
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                        || (c >= 'A' && c <= 'Z') || c == '-' || c == '.') {
                } else {
                    return true;
                }
            }

            return false;
        }
    }

    public static String summaryText(String summay) {
        Document d = Jsoup.parse(summay);
        // Elements elements = d.getElementsByTag("code").remove();
        // System.out.println(elements.size());
        // Elements tags = d.getElementsByTag("pre").remove();
        // System.out.println(tags.size());
        return d.body().text();
    }
}
