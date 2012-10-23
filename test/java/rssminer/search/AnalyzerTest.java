/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import me.shenfeng.mmseg.SimpleMMsegTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.junit.Test;
import rssminer.search.RssminerAnalyzer.DictHolder;

import java.io.IOException;
import java.io.StringReader;

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

        while (f.incrementToken()) {
            System.out.println(termAttr.toString());
            System.out.println(keywordAttr.isKeyword());
        }

    }

}
