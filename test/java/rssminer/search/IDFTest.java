/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;

public class IDFTest {

    @Test
    public void testIDF() throws IOException {

        FSDirectory directory = FSDirectory.open(new File("/var/rssminer/index"));

        IndexReader r = IndexReader.open(directory);

        // IndexSearcher searcher = IndexReader.open(directory);

        IndexSearcher searcher = new IndexSearcher(r);
        TopDocs result = searcher.search(new TermQuery(new Term(Searcher.RSS_ID, "4787")), 100);
        System.out.println(result.totalHits);
        Term t = new Term(Searcher.CONTENT);
        int total = r.numDocs();
        for (ScoreDoc doc : result.scoreDocs) {
            int id = doc.doc;
            TermFreqVector vector = r.getTermFreqVector(id, Searcher.CONTENT);
            if (vector != null) {
                String[] terms = vector.getTerms();
                int[] frequencies = vector.getTermFrequencies();
                for (int i = 0; i < terms.length; i++) {
                    Term term = t.createTerm(terms[i]);
                    // TermEnum termEnum = r.terms(term);
                    // int df = termEnum.docFreq();
                    int df2 = r.docFreq(term);
                    // System.out.println(id + "\t" + terms[i] + "\t"
                    // + termEnum.term().text() + "\t" +
                    // + frequencies[i] + "\t" + df + "\t" + df2);
                }
            }
            // r.ter
            // System.out.println(id);
        }

        // TermEnum terms = r.terms(new Term(Searcher.TITLE, "stubborn"));
        // while (terms.next()){
        // // System.out.println(terms.term() +"\t" + terms.docFreq());
        // }

    }
}
