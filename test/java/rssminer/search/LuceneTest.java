/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

class DelegateCollector extends Collector {

    Collector collector;

    public DelegateCollector(Collector collector) {
        this.collector = collector;
    }

    public void setScorer(Scorer scorer) throws IOException {
        System.out.println("set---scoreer----" + scorer);
        collector.setScorer(scorer);
    }

    public void collect(int doc) throws IOException {
        // System.out.println("collect---------- " + doc);
        collector.collect(doc);
    }

    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        System.out.println("setNextReader--------------" + reader + " docBase: " + docBase);
        collector.setNextReader(reader, docBase);
    }

    public boolean acceptsDocsOutOfOrder() {
        System.out.println("acceptDocOutfOrder");
        return collector.acceptsDocsOutOfOrder();
    }
}

public class LuceneTest {

    IndexSearcher searcher;

    static final Analyzer analyzer = new RssminerAnalyzer();

    @Before
    public void setup() throws IOException {
        Directory dir = FSDirectory.open(new File("/var/rssminer/index"));
        IndexReader reader = IndexReader.open(dir);
        System.out.println(reader.maxDoc());
        searcher = new IndexSearcher(reader);
    }

    @Test
    public void testPrioriyQueue() {

        // PriorityQueue
    }

    @Test
    public void testIntern() {
        String intern = null;
        for (int i = 0; i < 1000 * 1000; i++) {
            intern = ("aaa" + i).intern();

        }
        System.out.println(intern);
    }

    @Test
    public void testSearch2() throws Exception {

        Query q = new TermQuery(new Term(Searcher.CONTENT, "java"));

        // System.out.println();

        searcher.search(q, new DelegateCollector(TopScoreDocCollector.create(100, false)));

        // searcher.search()

        // searcher.search(ids);

        // searcher.search()

    }

    @Test
    public void testSearch() throws IOException {
        for (int j = 0; j < 10; j++) {

            TokenStream stream = analyzer.tokenStream("test", new StringReader(
                    "java technology"));

            CharTermAttribute c = stream.getAttribute(CharTermAttribute.class);
            List<String> terms = new ArrayList<String>();
            while (stream.incrementToken()) {
                String term = new String(c.buffer(), 0, c.length());
                terms.add(term);
            }

            BooleanQuery q = new BooleanQuery();
            for (Term field : Searcher.ALL_FIELDS) {
                BooleanQuery part = new BooleanQuery();
                for (String term : terms) {
                    part.add(new TermQuery(field.createTerm(term)), Occur.MUST);
                }
                q.add(part, Occur.SHOULD);
            }

            Random r = new Random();
            BooleanQuery ids = new BooleanQuery();
            int count = r.nextInt(450);
            for (int i = 0; i < count; i++) {
                ids.add(new TermQuery(new Term("rid", Integer.toString(i))), Occur.SHOULD);
            }

            BooleanQuery query = new BooleanQuery();
            query.add(q, Occur.MUST);
            query.add(ids, Occur.MUST);

            TopDocs result = searcher.search(query, 100);
            // System.out.println(query);
            System.out.println(terms);
            System.out.println(result.totalHits + "\t" + count);
            // searcher.search(query, results)
            // System.out.println(c);
        }
    }
}
