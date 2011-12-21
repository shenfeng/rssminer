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
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class UtilsTest {

    String html;
    String urlBase = "http://what.com";
    String proxyURi = "http://rssminer.net/p?u=";

    @Before
    public void setup() throws FileNotFoundException, IOException {
        html = IOUtils.toString(new FileInputStream(
                "/home/feng/workspace/rssminer/test/index.html"));
    }

    @Test
    public void testSoupBasedPerf() throws IOException, SAXException,
            URISyntaxException {
        String s = null;
        for (int i = 0; i < 10000; i++) {
            s = Utils.rewrite(html, urlBase, proxyURi);
        }
        long start = System.currentTimeMillis();

        for (int i = 0; i < 10000; i++) {
            s = Utils.rewrite(html, urlBase, proxyURi);
        }

        System.out.println(s);
        System.out.println(System.currentTimeMillis() - start);
    }

    @Test
    public void testSoupBased() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.rewrite(html, urlBase, proxyURi);
        IOUtils.write(s, new FileOutputStream("/tmp/result.html"));
    }

    @Test
    public void testSoupBasedNoProxy() throws IOException, SAXException,
            URISyntaxException {
        String s = Utils.rewrite(html, urlBase);
        IOUtils.write(s, new FileOutputStream("/tmp/result.html"));
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
