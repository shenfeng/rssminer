/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import me.shenfeng.mmseg.BSDictionary;
import me.shenfeng.mmseg.Dictionary;
import me.shenfeng.mmseg.SimpleMMsegTokenizer;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class RssminerAnalyzer extends Analyzer {

    public static class DictHolder {
        public static final Dictionary dic;

        static {
            InputStream is = RssminerAnalyzer.class.getClassLoader()
                    .getResourceAsStream("words.dic");
            Dictionary tmp = null;
            try {
                tmp = new BSDictionary(is);
                is.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(is);
            }
            dic = tmp;
        }
    }

    public final TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        return super.reusableTokenStream(fieldName, reader);
    }

    public final TokenStream tokenStream(String fieldName, Reader reader) {

        SimpleMMsegTokenizer msegTokenizer = new SimpleMMsegTokenizer(
                DictHolder.dic, reader);

        // System.out.println("-----------");
        // setPreviousTokenStream(mmsegTokenizer); // 保存实例

        // final StandardTokenizer src = new StandardTokenizer(v, reader);
        // TokenStream tok = new StandardFilter(v, src);
        // new PorterStemmer
        // return msegTokenizer;
        TokenStream tok = new StopFilter(msegTokenizer);
        tok = new PorterStemFilter(tok);
        return tok;
//        return new StopFilter(msegTokenizer);
        // return tok;
        // return new KStemFilter(tok);
    }
}
