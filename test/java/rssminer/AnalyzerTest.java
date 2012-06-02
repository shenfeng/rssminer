package rssminer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import me.shenfeng.mmseg.BSDictionary;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Test;

import rssminer.search.KStemStopAnalyzer;

public class AnalyzerTest {
    
    @Test
    public void test() throws IOException {
        URL url = BSDictionary.class.getClassLoader().getResource(
                "data/words.dic");
        
        InputStream is = BSDictionary.class.getClassLoader().getResourceAsStream("data/words.dic");
        System.out.println(is);
//        new BSDictionary(new File(url.getFile()));
    }

    private List<String> getTerms(Analyzer analyzer, String content)
            throws IOException {
        List<String> tokens = new ArrayList<String>();
        TokenStream stream = analyzer.tokenStream(field, new StringReader(
                content));
        stream.reset();

        CharTermAttribute termAtt = stream
                .addAttribute(CharTermAttribute.class);
        while (stream.incrementToken()) {
            tokens.add(termAtt.toString());
        }
        return tokens;
    }

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
        List<String> terms = getTerms(analyzer, content);
        Assert.assertTrue(!terms.contains("against"));
        Assert.assertTrue(!terms.contains("1111.1"));
        Assert.assertTrue(terms.contains("lazi"));

        printTerms(analyzer, content);
    }

    @Test
    public void testStandardAnalyzer() throws IOException {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_33);
        String content = "The Quick brown fox jumped over the lazy dog 以下新闻由机器每5分钟自动选取更新";
        printTerms(analyzer, content);
    }

    @Test
    public void testKStemStopAnalyzer() throws IOException {
        Analyzer analyzer = new KStemStopAnalyzer();
        String content = "against Lazy cats took catty 以下 1111.1 ";

        List<String> terms = getTerms(analyzer, content);
        Assert.assertTrue(!terms.contains("against"));
        Assert.assertTrue(!terms.contains("1111.1"));
        Assert.assertTrue(terms.contains("lazy"));

        printTerms(analyzer, content);
    }
}
