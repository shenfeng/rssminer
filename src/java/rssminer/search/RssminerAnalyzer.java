package rssminer.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import me.shenfeng.mmseg.BSDictionary;
import me.shenfeng.mmseg.Dictionary;
import me.shenfeng.mmseg.SimpleMMsegTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;

public class RssminerAnalyzer extends Analyzer {

    static class DictHolder {
        static final Dictionary dic;
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

    public TokenStream tokenStream(String fieldName, Reader reader) {

        SimpleMMsegTokenizer msegTokenizer = new SimpleMMsegTokenizer(
                DictHolder.dic, reader);

        // System.out.println("-----------");
        // setPreviousTokenStream(mmsegTokenizer); // 保存实例

        // final StandardTokenizer src = new StandardTokenizer(v, reader);
        // TokenStream tok = new StandardFilter(v, src);
        // new PorterStemmer
        TokenStream tok = new StopFilter(msegTokenizer);
        // return new KStemFilter(tok);
        return new PorterStemFilter(tok);
    }
}
