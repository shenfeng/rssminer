package rssminer.jsoup;

import java.net.URI;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlUtils {

    private static final Logger logger = LoggerFactory
            .getLogger(HtmlUtils.class);

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

    public static URI extractFavicon(String html, URI base) {
        try {
            Document d = Jsoup.parse(html);
            Elements elements = d.getElementsByTag("link");
            for (Element e : elements) {
                String rel = e.attr("rel");
                if (rel != null && rel.indexOf("icon") != -1) {
                    String href = e.attr("href");
                    if (href != null) {
                        return base.resolve(href);
                    }
                }
            }
        } catch (Exception ignore) {
            logger.warn(base.toString(), ignore);
        }
        return null;
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
