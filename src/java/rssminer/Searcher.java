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

import clojure.lang.ISeq;
import clojure.lang.Seqable;

public class Searcher {
    static final Version V = Version.LUCENE_33;
    static final int LENGTH = 280;
    static final Analyzer analyzer = new StandardAnalyzer(V);
    static final Logger logger = Logger.getLogger(Searcher.class);
    static final String FEED_ID = "feedId";
    static final String AUTHOR = "author";
    static final String TITLE = "title";
    static final String RSS_ID = "rss_id";
    static final String SUMMARY = "summary";
    static final String TAG = "tag";
    static final String SNIPPET = "snippet";

    static String[] FIELDS = new String[] { AUTHOR, TITLE, SUMMARY, TAG };

    private static String genSnippet(String summary) {
        if (summary.length() < LENGTH)
            return summary;
        else {
            int len = LENGTH;
            while (len < summary.length() && summary.charAt(len) != ' ')
                ++len;
            return summary.substring(0, len);
        }
    }

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

    public void commit() throws CorruptIndexException, IOException {
        long start = System.currentTimeMillis();
        indexer.commit();
        long duration = System.currentTimeMillis() - start;
        logger.debug("commit index, taked " + duration + "ms");
    }

    public void close() throws CorruptIndexException, IOException {
        Runtime.getRuntime().removeShutdownHook(shutDownHook);
        synchronized (indexer) {
            if (indexer != null) {
                logger.debug("close Searcher@" + path);
                indexer.close();
                indexer = null;
            }
        }
    }

    public void index(int feeId, int rssId, String author, String title,
            String summary, Seqable tags) throws CorruptIndexException,
            IOException {
        Document doc = new Document();
        NumericField fid = new NumericField(FEED_ID, Store.YES, false);
        fid.setIntValue(feeId);
        doc.add(fid);

        NumericField rid = new NumericField(RSS_ID, Store.YES, false);
        rid.setIntValue(rssId);
        doc.add(rid);

        if (author != null) {
            Field a = new Field(AUTHOR, author, Store.YES, Index.ANALYZED);
            doc.add(a);
        }

        if (title != null) {
            Field t = new Field(TITLE, title, Store.YES, Index.ANALYZED);
            doc.add(t);
        }

        if (summary != null) {
            Field sum = new Field(SUMMARY, summary, Store.NO, Index.ANALYZED);
            doc.add(sum);
            Field s = new Field(SNIPPET, genSnippet(summary), Store.YES,
                    Index.NO);
            doc.add(s);
        }

        if (tags != null) {
            String t = "";
            ISeq seq = tags.seq();
            while (seq != null) {
                t += (seq.first().toString() + ", ");
                seq = seq.next();
            }

            if (t != "") {
                Field ca = new Field(TAG, t, Store.YES, Index.ANALYZED);
                doc.add(ca);
            }
        }

        indexer.addDocument(doc);
    }

    public String[] searchForTitle(String term, int n)
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

    public Feed[] search(String term, int count)
            throws CorruptIndexException, IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(IndexReader.open(indexer,
                true));
        QueryParser parser = new QueryParser(V, SUMMARY, analyzer);
        Query query = parser.parse(term);
        TopDocs docs = searcher.search(query, count);
        final int len = docs.scoreDocs.length;
        Feed[] results = new Feed[len];
        for (int i = 0; i < len; i++) {
            Feed f = new Feed();
            Document doc = searcher.doc(docs.scoreDocs[i].doc);
            f.setTitle(doc.get(TITLE));
            f.setAuthor(doc.get(AUTHOR));
            f.setCategories(doc.get(TAG));
            f.setSnippet(doc.get(SNIPPET));
            f.setFeedid(doc.get(FEED_ID));
            results[i] = f;
        }
        return results;
    }
}
