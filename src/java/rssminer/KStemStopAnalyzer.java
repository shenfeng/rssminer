package rssminer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.util.Version;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.analysis.MMSegTokenizer;

public class KStemStopAnalyzer extends Analyzer {

    private final Version v;

    public KStemStopAnalyzer(Version v) {
        this.v = v;
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {

        Seg seg = new ComplexSeg(Dictionary.getInstance());
        // System.out.println("-----------");
        MMSegTokenizer mmsegTokenizer = new MMSegTokenizer(seg, reader);
        setPreviousTokenStream(mmsegTokenizer); // 保存实例

        // final StandardTokenizer src = new StandardTokenizer(v, reader);
        // TokenStream tok = new StandardFilter(v, src);
        TokenStream tok = new LowerCaseFilter(v, mmsegTokenizer);
        tok = new StopFilter(tok);
        return new KStemFilter(tok);
    }
}
