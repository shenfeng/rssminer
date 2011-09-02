package rssminer;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;

public class AnalyzerTest {

    private void printTerms(Analyzer analyzer, String content)
            throws IOException {
        System.out.println("-------" + analyzer);
        TokenStream stream = analyzer.tokenStream(field, new StringReader(
                content));
        stream.reset();

        CharTermAttribute termAtt = stream
                .addAttribute(CharTermAttribute.class);
        OffsetAttribute offAtt = stream.addAttribute(OffsetAttribute.class);

        while (stream.incrementToken()) {
            String term = new String(termAtt.buffer(), 0, termAtt.length());
            System.out.println(term + "\t" + offAtt.startOffset() + " "
                    + offAtt.endOffset());
        }
    }

    private final String field = "content";

    @Test
    public void testPorterStopAnalyzer() throws IOException {
        Analyzer analyzer = new PorterStopAnalyzer(Version.LUCENE_33);
        String content = "against Lazy cats took catty 以下 1111.1 ";

        printTerms(analyzer, content);
    }

    @Test
    public void testStandardAnalyzer() throws IOException {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);
        String content = "The Quick brown fox jumped over the lazy dog 以下新闻由机器每5分钟自动选取更新";
        printTerms(analyzer, content);
    }
}
