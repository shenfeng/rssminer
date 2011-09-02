package rssminer;

import static java.lang.Character.isLetter;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
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
import org.apache.lucene.search.similar.MoreLikeThis;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import clojure.lang.ISeq;
import clojure.lang.Seqable;

public class Searcher {
    static final Version V = Version.LUCENE_33;
    static final int LENGTH = 280;
    static final Analyzer analyzer = new PorterStopAnalyzer(V);
    static final Logger logger = Logger.getLogger(Searcher.class);
    static final String FEED_ID = "feedId";
    static final String AUTHOR = "author";
    static final String TITLE = "title";
    static final String CONTENT = "content";
    static final String TAG = "tag";
    static final String SNIPPET = "snippet";
    static String[] FIELDS = new String[] { AUTHOR, TITLE, CONTENT, TAG };

    private static String genSnippet(String summary) {
        if (summary.length() < LENGTH)
            return summary;
        else {
            int len = LENGTH;
            while (len < summary.length() && isLetter(summary.charAt(len)))
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

    public Searcher(String path, boolean debug) throws IOException {
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
        if (debug) {
            indexer.setInfoStream(System.out);
        }
    }

    @Override
    public String toString() {
        return "Searcher@" + path;
    }

    public void clear() throws IOException {
        indexer.deleteAll();
        indexer.commit();
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

    public void index(int feeId, String author, String title, String content,
            Seqable tags) throws CorruptIndexException, IOException {
        Document doc = new Document();
        NumericField fid = new NumericField(FEED_ID, Store.YES, false);
        fid.setIntValue(feeId);
        doc.add(fid);

        if (author != null) {
            Field a = new Field(AUTHOR, author, Store.YES, Index.ANALYZED);
            doc.add(a);
            doc.setBoost(1.2f);
        }

        if (title != null) {
            Field t = new Field(TITLE, title, Store.YES, Index.ANALYZED,
                    TermVector.YES);
            doc.add(t);
            doc.setBoost(1.5f);
        }

        if (content != null) {
            Field c = new Field(CONTENT, content, Store.NO, Index.ANALYZED,
                    TermVector.YES);
            doc.add(c);
            Field s = new Field(SNIPPET, genSnippet(content), Store.YES,
                    Index.NO);
            doc.add(s);
        }

        if (tags != null && tags.seq() != null) {
            ISeq seq = tags.seq();
            StringBuilder sb = new StringBuilder(seq.count() * 10);
            while (seq != null) {
                sb.append(seq.first()).append(", ");
                seq = seq.next();
            }

            String t = sb.toString();
            if (t.length() > 0) {
                Field f = new Field(TAG, t, Store.YES, Index.ANALYZED,
                        TermVector.YES);
                f.setBoost(1.5f);
                doc.add(f);
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

    private Feed[] searchQuery(IndexSearcher searcher, Query q, int count)
            throws IOException {
        TopDocs docs = searcher.search(q, count);
        final int len = docs.scoreDocs.length;
        Feed[] results = new Feed[len];
        for (int i = 0; i < len; i++) {
            Feed f = new Feed();
            int docid = docs.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            f.setTitle(doc.get(TITLE));
            f.setDocId(docid);
            f.setAuthor(doc.get(AUTHOR));
            f.setCategories(doc.get(TAG));
            f.setSnippet(doc.get(SNIPPET));
            f.setFeedid(doc.get(FEED_ID));
            results[i] = f;
        }
        return results;
    }

    public Feed[] search(String term, int count)
            throws CorruptIndexException, IOException, ParseException {
        IndexSearcher searcher = new IndexSearcher(IndexReader.open(indexer,
                false));
        QueryParser parser = new QueryParser(V, CONTENT, analyzer);
        Query query = parser.parse(term);
        return searchQuery(searcher, query, count);
    }

    public Feed[] likeThis(int docID, int count)
            throws CorruptIndexException, IOException {
        IndexReader reader = IndexReader.open(indexer, false);
        MoreLikeThis likeThis = new MoreLikeThis(reader);
        likeThis.setFieldNames(FIELDS);
        likeThis.setMinTermFreq(1);
        likeThis.setMinDocFreq(3);
        Query like = likeThis.like(docID);
        return searchQuery(new IndexSearcher(reader), like, count);
    }
}
