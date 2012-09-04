package rssminer.search;

import java.io.IOException;
import java.io.StringReader;

import me.shenfeng.mmseg.SimpleMMsegTokenizer;

import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.junit.Test;

import rssminer.search.RssminerAnalyzer.DictHolder;

public class AnalyzerTest {

    @Test
    public void testTest() throws IOException {
        StringReader reader = new StringReader("acategory abcde");
        SimpleMMsegTokenizer msegTokenizer = new SimpleMMsegTokenizer(
                DictHolder.dic, reader);
        TokenStream tok = new StopFilter(msegTokenizer);
        PorterStemFilter f = new PorterStemFilter(tok);
        CharTermAttribute termAttr = f.getAttribute(CharTermAttribute.class);
        KeywordAttribute keywordAttr = f.getAttribute(KeywordAttribute.class);
        
        while(f.incrementToken()) {
            System.out.println(termAttr.toString());
            System.out.println(keywordAttr.isKeyword());
        }

    }

}
