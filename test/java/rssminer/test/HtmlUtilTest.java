package rssminer.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import rssminer.tools.Utils;
import rssminer.jsoup.HtmlUtils;

public class HtmlUtilTest {

    @Test
    public void testCompactHtml() {
        String[] snippets = new String[] { "<pre>a <span>b</span>c  d</pre>",
                "<div></div>", "<div>aa  aa</div>",
                "<div class='aaa' id='bbb' style='ccc'>aaaa</div>",
                "<div><p>aaaa</p></div>", "<div>a<p>aaaa</p>b</div>",
                "<div><div>a</div><div>b</div><div>c</div></div>",
                "<div a='b'></div>",
                "<div><p>12</p><style>.a{color:red}</style></div>" };

        String[] expects = new String[] { "<pre>a <span>b</span>c  d</pre>",
                "<div></div>", "<div>\naa aa</div>", "<div>\naaaa</div>",
                "<div><p>aaaa</p></div>", "<div>\na<p>aaaa</p>b</div>",
                "<div><div>\na</div><div>\nb</div><div>\nc</div></div>",
                "<div></div>", "<div><p>12</p></div>" };

        for (int i = 0; i < snippets.length; i++) {
            String expect = HtmlUtils.compact(snippets[i],
                    "http://rssminer.net");
            // System.out.println(expect);
            Assert.assertEquals("should the same", expects[i], expect);
        }
    }

    @Test
    public void testExtractFavicon() throws FileNotFoundException,
            IOException, SAXException, URISyntaxException {
        String html = IOUtils.toString(new FileInputStream(
                "test/html/python-iaq.html"));
        String icon = HtmlUtils.extractFavicon(html,
                new URI("http://rssminer.net/")).toString();

        Assert.assertEquals("http://rssminer.net/favicon.ico", icon);
    }

    @Test
    public void testExtractRssLink() throws FileNotFoundException,
            IOException, SAXException, URISyntaxException {
        String html = Utils.readFile("test/e_rss/planet_clojure.html");
        String str = HtmlUtils.extractRssUrl(html, new URI(
                "http://planet.clojure.in/"));
        Assert.assertEquals("http://planet.clojure.in/atom.xml", str);
        // System.out.println(str);

        html = Utils.readFile("test/e_rss/ul.uk.html");
        str = HtmlUtils.extractRssUrl(html, new URI("http://www.uc.hk/"));

        Assert.assertEquals("http://www.uc.hk/rss.xml", str);
        // System.out.println(str);

        html = Utils.readFile("test/e_rss/scottgu.html");
        str = HtmlUtils.extractRssUrl(html, new URI(
                "http://weblogs.asp.net/scottgu/"));

        Assert.assertEquals("http://weblogs.asp.net/scottgu/rss.aspx", str);
        // System.out.println(str);

        html = IOUtils.toString(new FileInputStream(
                "test/e_rss/blog.golang.org.htm"));
        str = HtmlUtils.extractRssUrl(html,
                new URI("http://blog.golang.org/"));

        Assert.assertEquals("http://blog.golang.org/feeds/posts/default", str);
        // System.out.println(str);
    }

    @Test
    public void testExtractSummary() {
        String s = HtmlUtils.summaryText("<a>text\n</a>");
        Assert.assertEquals(s, "text");
    }

    @Test
    public void testMinifyHtml() throws FileNotFoundException, IOException,
            SAXException {
        String htmlWithBr = Utils.readFile("test/html/python-iaq.html");

        String s = HtmlUtils.minfiyHtml(htmlWithBr, "http://what.com");
        Assert.assertEquals(-1, s.indexOf("</hr>"));
        Assert.assertEquals(-1, s.indexOf("</br>"));

        String file = Utils.readFile("test/html/alibuybuy-70468.html");
        s = HtmlUtils.minfiyHtml(file,
                "http://www.alibuybuy.com/posts/70468.html");
    }

}
