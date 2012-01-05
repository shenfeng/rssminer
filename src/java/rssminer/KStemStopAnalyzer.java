package rssminer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

public class KStemStopAnalyzer extends Analyzer {

    private final Version v;

    public KStemStopAnalyzer(Version v) {
        this.v = v;
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {
        final StandardTokenizer src = new StandardTokenizer(v, reader);
        TokenStream tok = new StandardFilter(v, src);
        tok = new LowerCaseFilter(v, tok);
        tok = new StopFilter(tok);
        return new KStemFilter(tok);
    }

}
