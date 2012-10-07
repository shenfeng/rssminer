package rssminer.search;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;

public class FactedSearch {

    public static void main(String[] args) throws CorruptIndexException,
            IOException {

        IndexReader reader = IndexReader.open(FSDirectory.open(new File(
                "/var/rssminer/index")));
        IndexSearcher searcher = new IndexSearcher(reader);

        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();

            TermQuery query = new TermQuery(new Term(Searcher.CONTENT, "http"));
            // TopDocs docs = searcher.search(query, 10);
            // System.out.println(docs.totalHits);

            FacetCollector collector = new FacetCollector(null);
            searcher.search(query, collector);
            collector.getAuthor(20);
            collector.getTag(20);
            System.out.println(System.currentTimeMillis() - start);
            // collector.print();
        }
    }
}
