/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.jsoup;

import java.net.URI;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlUtils {

    private static final Logger logger = LoggerFactory.getLogger(HtmlUtils.class);

    final static String HREF = "href";
    final static String LINK = "link";
    final static String RSS = "application/rss+xml";
    final static String ATOM = "application/atom+xml";
    final static String TITLE = "title";
    final static String TYPE = "type";
    final static String REL = "rel";
    final static String ALTERNATE = "alternate";
    final static Pattern comment = Pattern.compile("comment", Pattern.CASE_INSENSITIVE);

    static final String[] IGNORE_TAGS = new String[] { "script", "style", "link", "#comment" };

    static String[] TEXT_IGNORE = new String[] { "code" };

    public static String compact(String html, String baseUri) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(html.length());
        CompactHtmlVisitor vistor = new CompactHtmlVisitor(sb, baseUri);
        PartialTraversor traversor = new PartialTraversor(vistor, IGNORE_TAGS);
        Document doc = Jsoup.parse(html, baseUri);
        List<Node> nodes = doc.body().childNodes();
        for (Node e : nodes) {
            traversor.traverse(e);
        }
        return vistor.toString();
    }

    public static URI extractFavicon(String html, URI base) {
        try {
            Document d = Jsoup.parse(html);
            Elements elements = d.getElementsByTag("link");
            for (Element e : elements) {
                String rel = e.attr("rel");
                if (rel != null && rel.contains("icon")) {
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

    public static String extractRssUrl(String html, URI base) {
        Document d = Jsoup.parse(html);
        Elements links = d.getElementsByTag(LINK);

        for (Element link : links) {
            if (ALTERNATE.equalsIgnoreCase(link.attr(REL))) {
                String type = link.attr(TYPE);
                if (RSS.equalsIgnoreCase(type) || ATOM.equalsIgnoreCase(type)) {
                    String href = link.attr(HREF);
                    String title = link.attr(TITLE);
                    if (title == null) {
                        title = "";
                    }
                    // ignore comment
                    if (href != null && !comment.matcher(href).find()
                            && !comment.matcher(title).find()) {
                        // return the first one
                        return base.resolve(href).toString();
                    }
                }
            }
        }

        return null;
    }

    public static boolean isQuoteNeeded(String val) {
        if (val.length() > 10) {
            return true;
        } else {
            int i = val.length();
            while (--i >= 0) {
                char c = val.charAt(i);
                // http://www.cs.tut.fi/~jkorpela/qattr.html
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || c == '-' || c == '.') {
                } else {
                    return true;
                }
            }

            return false;
        }
    }

    public static String summaryText(String summay) {
        if (summay == null) {
            return "";
        }
        Document d = Jsoup.parse(summay);
        // Elements elements = d.getElementsByTag("code").remove();
        // System.out.println(elements.size());
        // Elements tags = d.getElementsByTag("pre").remove();
        // System.out.println(tags.size());
        return d.body().text();
    }

    public static String text(String html, boolean withA) {
        if (html == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(html.length() / 2);
        TextAccumVisitor vistor = new TextAccumVisitor(sb, withA);
        PartialTraversor traversor = new PartialTraversor(vistor, TEXT_IGNORE);
        traversor.traverse(Jsoup.parse(html).body());
        return sb.toString();
    }

    public static String text(String html) {
        // default ignore if <a href="href">href</a>
        return text(html, false);
    }
}
