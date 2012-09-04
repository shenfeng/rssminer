package rssminer.test;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import rssminer.jsoup.CompactHtmlVisitor;
import rssminer.jsoup.PartialTraversor;
import rssminer.tools.Utils;

public class JsoupBench {

    static final String[] IGNORE_TAGS = new String[] { "script", "style",
            "link", "#comment" };

    @Test
    public void testJsoup() {
        File folder = new File("/home/feng/Downloads/htmls");
        long htmlLength = 0;
        long compactLength = 0;
        String baseUri = "";
        int count = 0;
        for (File f : folder.listFiles()) {
            count++;
            if (count % 1000 == 0) {
                System.out.println(count);
            }
            try {
                String html = Utils.readFile(f.getAbsolutePath());

                StringBuilder sb = new StringBuilder(html.length());
                CompactHtmlVisitor vistor = new CompactHtmlVisitor(sb,
                        baseUri);
                PartialTraversor traversor = new PartialTraversor(vistor,
                        IGNORE_TAGS);
                Document doc = Jsoup.parse(html, baseUri);
                traversor.traverse(doc);
                // System.out.println(sb.toString());
                htmlLength += html.length();
                compactLength += sb.length();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("orignal: " + htmlLength);
        System.out.println("compact: " + compactLength);
        System.out.println(compactLength / (double) htmlLength);
    }
}
