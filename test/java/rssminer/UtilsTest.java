package rssminer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // htmlWithBr = IOUtils.toString(new FileInputStream("/tmp/a.html"));

        htmlWithBr = IOUtils.toString(new FileInputStream(
                "/home/feng/workspace/rssminer/test/python-iaq.html"));

    }

    @Test
    public void testMinify() throws Exception {

        String html = IOUtils.toString(new FileInputStream(new File(
                "/tmp/70468.html")));

        String minified = Utils.minfiyHtml(html,
                "http://www.alibuybuy.com/posts/70468.html");

        System.out.println(minified);
    }

    @Test
    public void testHtmlminfiy() throws IOException, SAXException {
        String result1 = Utils.minfiyHtml(htmlWithBr, "http://google.com");
        System.out.println(result1);
        System.out.println(htmlWithBr.length() + "\t" + result1.length());
    }

    @Test
    public void testBrHr() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.minfiyHtml(htmlWithBr, urlBase);
        Assert.assertEquals(-1, s.indexOf("</hr>"));
        Assert.assertEquals(-1, s.indexOf("</br>"));
    }

    @Test
    public void testRegex() {
        String s = "<img id=\"image\" alt=\"\" onkeydown=\"if(event.keyCode==13)event.keyCode=9\"  src=\"getimage.jsp?ranstr=097144\"> ";
        Pattern p = Pattern.compile("src=\"(.+?)\"", 2);

        Matcher m = p.matcher(s);
        while (m.find()) {
            System.out.println(m.group());
            System.out.println(m.group(1));
        }
    }

    @Test
    public void testInteger() {
        String s = Integer.toString(Integer.MAX_VALUE - 100, 35);
        System.out.println(s);
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
    public void testURI() throws URISyntaxException {
        URI uri = new URI("http://shenfeng.me/index.html#what");
        String str = uri.getRawFragment();
        System.out.println(System.currentTimeMillis() / 1000);
        System.out.println(str);
    }
}
