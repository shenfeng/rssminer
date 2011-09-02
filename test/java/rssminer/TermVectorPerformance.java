package rssminer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TermVectorPerformance {

    private static Logger logger = Logger
            .getLogger(TermVectorPerformance.class);
    private IndexReader reader;

    @Before
    public void setup() throws IOException {
        Version v = Version.LUCENE_33;
        IndexWriterConfig config = new IndexWriterConfig(v,
                new StandardAnalyzer(v));
        config.setOpenMode(OpenMode.CREATE_OR_APPEND);
        config.setRAMBufferSizeMB(128);
        FSDirectory dir = FSDirectory.open(new File("/tmp/index"));
        IndexWriter writer = new IndexWriter(dir, config);
        writer.setInfoStream(System.out);
        // writer.optimize(true);
        reader = IndexReader.open(writer, false);
    }

    @After
    public void tearDown() throws IOException {
        reader.close();
    }

    @Test
    public void testPopularTerms() throws IOException {
        TermEnum terms = reader.terms();
        int max = 0;
        int i = 0;
        while (terms.next()) {
            Term term = terms.term();
            int c = terms.docFreq();
            max = max > c ? max : c;
            // System.out.printf("%18s", term.text());
            if (++i % 8 == 0) {
                // System.out.println();
            }
            // System.out.println(reader.docFreq(term) + "\t" +
            // terms.docFreq());
        }
        logger.info(max);
    }

    @Test
    public void testGetAllTermVector() throws IOException {
        int numDocs = reader.numDocs();
        int count = 0;
        Term t = new Term("content");
        for (int i = 0; i < numDocs; ++i) {
            if (reader.isDeleted(i))
                continue;
            count++;
            TermFreqVector termFreqVector = reader.getTermFreqVector(i,
                    "content");
            if (termFreqVector != null) {
                int[] frequencies = termFreqVector.getTermFrequencies();
                String[] terms = termFreqVector.getTerms();
                Map<String, Integer> map = new HashMap<String, Integer>(
                        frequencies.length);

                for (int j = 0; j < frequencies.length; ++j) {
                    String text = terms[j];
                    t.createTerm(text);
                    int freq = reader.docFreq(t);
                    // int f = frequencies[j];

                    // System.out.println(freq + "\t" + f + "\t" + text);
                    map.put(text, freq);
                }
                if (count % 1000 == 0)
                    logger.info(frequencies.length + "\t" + i);
                // break;
            }

            // if (count > 10)
            // break;
        }
    }

    @Test
    public void testIsLetter() {
        String s = "去年，《abc def   3,085";
        for (int i = 0; i < s.length(); ++i) {
            System.out.println(s.charAt(i) + "\t"
                    + Character.isLetter(s.charAt(i)));
        }
    }
}
