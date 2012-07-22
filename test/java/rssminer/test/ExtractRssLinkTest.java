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

import rssminer.Utils;

public class ExtractRssLinkTest {

    @Test
    public void testExtractRssLink() throws FileNotFoundException,
            IOException, SAXException, URISyntaxException {
        String html = IOUtils.toString(new FileInputStream(
                "test/e_rss/planet_clojure.html"));
        String str = Utils.extractRssUrl(html, new URI(
                "http://planet.clojure.in/"));
        Assert.assertEquals("http://planet.clojure.in/atom.xml", str);
        // System.out.println(str);

        html = IOUtils.toString(new FileInputStream("test/e_rss/ul.uk.html"));
        str = Utils.extractRssUrl(html, new URI("http://www.uc.hk/"));

        Assert.assertEquals("http://www.uc.hk/rss.xml", str);
        // System.out.println(str);

        html = IOUtils
                .toString(new FileInputStream("test/e_rss/scottgu.html"));
        str = Utils.extractRssUrl(html, new URI(
                "http://weblogs.asp.net/scottgu/"));

        Assert.assertEquals("http://weblogs.asp.net/scottgu/atom.aspx", str);
        // System.out.println(str);

        html = IOUtils.toString(new FileInputStream(
                "test/e_rss/blog.golang.org.htm"));
        str = Utils.extractRssUrl(html, new URI("http://blog.golang.org/"));

        Assert.assertEquals(
                "http://blog.golang.org/feeds/posts/default?alt=rss", str);
        // System.out.println(str);

    }
}
