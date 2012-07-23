package rssminer.test;

import org.junit.Assert;
import org.junit.Test;

import rssminer.jsoup.HtmlUtils;

public class HtmlUtilTest {

    @Test
    public void testCompactHtml() {
        String[] snippets = new String[] { "<pre>a <span>b</span>c  d</pre>",
                "<div></div>", "<div>aa  aa</div>",
                "<div class='aaa' id='bbb' style='ccc'>aaaa</div>",
                "<div><p>aaaa</p></div>", "<div>a<p>aaaa</p>b</div>",
                "<div><div>a</div><div>b</div><div>c</div></div>",
                "<div a='b'></div>" };

        String[] expects = new String[] { "<pre>a <span>b</span>c  d</pre>",
                "<div></div>", "<div>aa aa</div>", "<div>aaaa</div>",
                "<div><p>aaaa</p></div>", "<div>a<p>aaaa</p>b</div>",
                "<div><div>a</div><div>b</div><div>c</div></div>",
                "<div></div>" };

        for (int i = 0; i < snippets.length; i++) {
            String expect = HtmlUtils.compact(snippets[i], "http://rssminer.net");
            // System.out.println(expect);
            Assert.assertEquals("should the same", expects[i], expect);
        }
    }
}
