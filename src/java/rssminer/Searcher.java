package rssminer;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class Searcher {

    static final Version V = Version.LUCENE_33;
    static final Analyzer analyzer = new KStemStopAnalyzer(V);
    static final Logger logger = LoggerFactory.getLogger(Searcher.class);
    static final String FEED_ID = "id";
    static final String AUTHOR = "author";
    static final String TITLE = "title";
    static final String CONTENT = "content";
    static final String TAG = "tag";
    static String[] FIELDS = new String[] { AUTHOR, TITLE, CONTENT, TAG };

    private IndexWriter indexer = null;
    private final String path;

    public void toggleInfoStream(boolean toggle) throws IOException {
        if (toggle) {
            indexer.setInfoStream(System.out);
        } else {
            indexer.setInfoStream(null);
        }
    }

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
        if (indexer != null) {
            logger.info("close Searcher@" + path);
            indexer.close();
            indexer = null;
        }
    }

    public void index(int feeId, String author, String title, String summary,
            String tags) throws CorruptIndexException, IOException {
        Document doc = new Document();
        NumericField fid = new NumericField(FEED_ID, Store.YES, false);
        fid.setIntValue(feeId);
        doc.add(fid);

        if (author != null && author.length() > 0) {
            // TODO why NOT_ANALYZED searched nothing?
            Field a = new Field(AUTHOR, author, Store.NO, Index.ANALYZED);
            a.setBoost(1.2f);
            doc.add(a);
        }

        if (title != null) {
            Field t = new Field(TITLE, title, Store.NO, Index.ANALYZED,
                    TermVector.YES);
            t.setBoost(1.5f);
            doc.add(t);
        }

        if (summary != null) {
            try {
                String content = Utils.extractText(summary);
                Field c = new Field(CONTENT, content, Store.NO,
                        Index.ANALYZED, TermVector.YES);
                doc.add(c);
            } catch (SAXException ignore) {
            }
        }
        if (tags != null) {
            String[] ts = tags.split(", ");
            for (String tag : ts) {
                Field f = new Field(TAG, tag, Store.NO, Index.NOT_ANALYZED,
                        TermVector.YES);
                f.setBoost(1.3f);
                doc.add(f);
            }
        }

        indexer.addDocument(doc);
    }

    private Map<String, Feed> doSearch(IndexSearcher searcher, Query q,
            int count) throws IOException {
        TopDocs docs = searcher.search(q, count);
        Map<String, Feed> map = new TreeMap<String, Feed>();
        for (int i = 0; i < docs.scoreDocs.length; i++) {
            int docid = docs.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            map.put(doc.get(FEED_ID), new Feed(docid, doc.get(FEED_ID)));
        }
        return map;
    }

    public Map<String, Feed> search(String term, int count)
            throws CorruptIndexException, IOException, ParseException {
        IndexReader reader = IndexReader.open(indexer, false);
        IndexSearcher searcher = new IndexSearcher(reader);
        if (term.startsWith("related:")) {
            int docId = Integer.valueOf(term.substring("related:".length()));
            MoreLikeThis likeThis = new MoreLikeThis(reader);
            likeThis.setFieldNames(FIELDS);
            likeThis.setMinTermFreq(1);
            likeThis.setMinDocFreq(3);
            Query like = likeThis.like(docId);
            return doSearch(searcher, like, count);
        } else {
            QueryParser parser = new QueryParser(V, CONTENT, analyzer);
            Query query = parser.parse(term);
            return doSearch(searcher, query, count);
        }
    }

    public static class Feed {
        public final int docId;
        public final String id;

        public Feed(int docId, String id) {
            this.docId = docId;
            this.id = id;
        }
    }
}
