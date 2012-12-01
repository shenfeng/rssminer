/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

import rssminer.search.StopFilter;

public class PorterStopAnalyzer extends Analyzer {

    private final Version v;

    public PorterStopAnalyzer(Version v) {
        this.v = v;
    }

    public TokenStream tokenStream(String fieldName, Reader reader) {

        final StandardTokenizer src = new StandardTokenizer(v, reader);
        TokenStream tok = new StandardFilter(v, src);
        tok = new LowerCaseFilter(v, tok);
        tok = new StopFilter(tok);
        return new PorterStemFilter(tok);
    }
}
