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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Before;
import org.junit.Test;

import rssminer.KStemStopAnalyzer;
import rssminer.Searcher;

public class LuceneTest {

	IndexSearcher searcher;

	static final Analyzer analyzer = new KStemStopAnalyzer();

	@Before
	public void setup() throws IOException {
		Directory dir = FSDirectory.open(new File("/var/rssminer/index"));
		IndexReader reader = IndexReader.open(dir);
		searcher = new IndexSearcher(reader);
	}

	@Test
	public void testIntern() {
		for (int i = 0; i < 1000 * 1000; i++) {
			"aaa".intern();
		}
	}

	@Test
	public void testSearch() throws IOException {
		for (int j = 0; j < 1000; j++) {

			TokenStream stream = analyzer.tokenStream("test", new StringReader(
					"java technology"));

			CharTermAttribute c = stream.getAttribute(CharTermAttribute.class);
			List<String> terms = new ArrayList<String>();
			while (stream.incrementToken()) {
				String term = new String(c.buffer(), 0, c.length());
				terms.add(term);
			}

			BooleanQuery q = new BooleanQuery();
			for (String field : Searcher.FIELDS) {
				BooleanQuery part = new BooleanQuery();
				for (String term : terms) {
					part.add(new TermQuery(new Term(field, term)), Occur.MUST);
				}
				q.add(part, Occur.SHOULD);
			}

			Random r = new Random();
			BooleanQuery ids = new BooleanQuery();
			int count = r.nextInt(450);
			for (int i = 0; i < count; i++) {
				ids.add(new TermQuery(new Term("rid", Integer.toString(i))),
						Occur.SHOULD);
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
