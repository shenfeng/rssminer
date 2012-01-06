package rssminer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class UtilsTest {

    String html;
    String htmlWithBr;
    String urlBase = "http://what.com";
    String proxyURi = "http://rssminer.net/p?u=";

    @Before
    public void setup() throws FileNotFoundException, IOException {
        html = IOUtils.toString(new FileInputStream(
                "/home/feng/workspace/rssminer/test/index.html"));
        htmlWithBr = IOUtils.toString(new FileInputStream(
                "/home/feng/workspace/rssminer/test/python-iaq.html"));
    }

    @Test
    public void testSoupBasedPerf() throws IOException, SAXException,
            URISyntaxException {
        String s = null;
        for (int i = 0; i < 100; i++) {
            s = Utils.rewrite(html, urlBase, proxyURi);
        }
        long start = System.currentTimeMillis();

        for (int i = 0; i < 100; i++) {
            s = Utils.rewrite(html, urlBase, proxyURi);
        }

        System.out.println(s);
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testBrHr() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.rewrite(htmlWithBr, urlBase, proxyURi);
        Assert.assertEquals(-1, s.indexOf("</hr>"));
        Assert.assertEquals(-1, s.indexOf("</br>"));
    }

    @Test
    public void testSoupBased() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.rewrite(htmlWithBr, urlBase, proxyURi);
        IOUtils.write(s, new FileOutputStream("/tmp/result.html"));
        System.out.println(htmlWithBr.length() + "\t" + s.length());
    }

    @Test
    public void testInteger() {
        String s = Integer.toString(Integer.MAX_VALUE - 100, 35);
        System.out.println(s);
    }

    @Test
    public void testRewriteGoogleGroup() {
        try {
            String html = IOUtils.toString(new FileInputStream(
                    "/tmp/4387.html"));
            String result = Utils
                    .rewrite(
                            html,
                            "http://groups.google.com/group/clojure-dev/browse_thread/thread/8b96a6d9dfc7c8f9/dce1e217c7201c17?show_docid=dce1e217c7201c17");
            System.out.println(result);
            IOUtils.write(result, new FileOutputStream("/tmp/result.html"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSoupBasedNoProxy() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.rewrite(html, urlBase);
        IOUtils.write(s, new FileOutputStream("/tmp/result.html"));
        System.out.println(html.length() + "\t" + s.length() + "\t"
                + s.length() / (double) (html.length()));
    }

    static String getContent(String file) throws IOException {
        InputStream s = UtilsTest.class.getClassLoader().getResourceAsStream(
                file);
        String content = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        String line = null;
        while ((line = br.readLine()) != null) {
            content += line;
        }
        return content;
    }

    @Test
    public void testExtractLinks() throws IOException, SAXException {
        // Info tuple = Utils.extract(getContent("page.html"));
        // Assert.assertTrue(tuple.links.size() > 0);
        // Assert.assertTrue(tuple.rssLinks.size() > 0);
    }

    @Test
    public void testExtractFavicon() throws FileNotFoundException,
            IOException, SAXException {
        String html = IOUtils.toString(new FileInputStream(
                "/home/feng/workspace/rssminer/templates/index.html"));
        String icon = Utils.extractFaviconUrl(html, "http://rssminer.net");

        Assert.assertEquals("http://rssminer.net/imgs/16px-feed-icon.png",
                icon);
    }

    @Test
    public void testURI() throws URISyntaxException {
        URI uri = new URI("http://shenfeng.me/index.html#what");
        String str = uri.getRawFragment();
        System.out.println(System.currentTimeMillis() / 1000);
        System.out.println(str);
    }
}
