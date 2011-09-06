package rssminer;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.jboss.netty.util.CharsetUtil.*;
import org.junit.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testGetPath() throws URISyntaxException {
        URI uri = new URI("http://shenfeng.me?a=b");
        String path = Utils.getPath(uri);
        Assert.assertTrue("/?a=b".equals(path));
        uri = new URI(
                "http://www.baidu.com/s?wd=%D5%AC%BC%B1%CB%CD&rsv_bp=0&inputT=3664");
        Assert.assertNotSame("should equal",
                "s?wd=%D5%AC%BC%B1%CB%CD&rsv_bp=0&inputT=3664",
                Utils.getPath(uri));
    }

    @Test
    public void testParseCharset() {
        Assert.assertEquals("default utf8", UTF_8, Utils.parseCharset(null));
        Assert.assertEquals("parse gbk", Charset.forName("gbk"),
                Utils.parseCharset("text/html;charset=gbk"));
        Assert.assertEquals("parse gb2312", Charset.forName("gb2312"),
                Utils.parseCharset("text/html;charset=gb2312"));
    }
}
