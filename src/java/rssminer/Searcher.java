package rssminer;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

public class Searcher {
    static final Version V = Version.LUCENE_33;
    static final Analyzer analyzer = new StandardAnalyzer(V);
    static final Logger logger = Logger.getLogger(Searcher.class);
    static final String FEED_ID = "feedId";
    static final String AUTHOR = "author";
    static final String TITLE = "title";
    static final String RSS_ID = "rss_id";
    static final String SUMMARY = "summary";

    static String[] FIELDS = new String[] { AUTHOR, TITLE, SUMMARY };

    private IndexWriter indexer = null;
    private final String path;
    private Thread shutDownHook = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                synchronized (indexer) {
                    if (indexer != null) {
                        logger.info("jvm shutdown, close Searcher@" + path);
                        indexer.close();
                        indexer = null;
                    }
                }
            } catch (Exception e) {
                logger.error("shutdownHook", e);
            }
        }
    });

    public Searcher(String path) throws IOException {
        final IndexWriterConfig cfg = new IndexWriterConfig(V, analyzer);
        this.path = path;
        cfg.setOpenMode(OpenMode.CREATE_OR_APPEND);
        Directory dir = null;
        if (path == "RAM") {
            dir = new RAMDirectory();
        } else {
            dir = FSDirectory.open(new File(path));
        }
        indexer = new IndexWriter(dir, cfg);
        Runtime.getRuntime().addShutdownHook(shutDownHook);
    }

    @Override
    public String toString() {
        return "Searcher@" + path;
    }

    public void close() throws CorruptIndexException, IOException {
        Runtime.getRuntime().removeShutdownHook(shutDownHook);
        synchronized (indexer) {
            if (indexer != null) {
                logger.info("close Searcher@" + path);
                indexer.close();
                indexer = null;
            }
        }
    }

    public void index(int feeId, int rssId, String author, String title,
            String summary) throws CorruptIndexException, IOException {
        Document doc = new Document();
        NumericField id = new NumericField(FEED_ID, Store.YES, false);
        id.setIntValue(feeId);
        doc.add(id);

        NumericField _rssid = new NumericField(RSS_ID, Store.YES, false);
        _rssid.setIntValue(rssId);
        doc.add(_rssid);

        if (author != null) {
            Field a = new Field(AUTHOR, author, Store.YES, Index.NO);
            doc.add(a);
        }

        if (title != null) {
            Field t = new Field(TITLE, title, Store.YES, Index.ANALYZED);
            doc.add(t);
        }

        if (summary != null) {
            Field s = new Field(SUMMARY, summary, Store.NO, Index.ANALYZED);
            doc.add(s);
        }
        indexer.addDocument(doc);
    }

    public String[] searchForTitle(final String term, final int n)
            throws CorruptIndexException, IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(IndexReader.open(indexer,
                true));
        QueryParser parser = new MultiFieldQueryParser(V, FIELDS, analyzer);
        Query query = parser.parse(term);
        TopDocs docs = searcher.search(query, n);
        final int length = docs.scoreDocs.length;
        String[] results = new String[length];
        for (int i = 0; i < length; i++) {
            results[i] = searcher.doc(docs.scoreDocs[i].doc).get(TITLE);
        }
        return results;
    }
}
