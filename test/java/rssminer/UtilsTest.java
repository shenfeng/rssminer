package rssminer;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
