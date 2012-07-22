package rssminer.search;

import static java.lang.Character.OTHER_PUNCTUATION;
import static rssminer.Utils.K_DATA_SOURCE;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sql.DataSource;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import rssminer.Utils;
import rssminer.db.DBHelper;
import rssminer.db.Feed;
import rssminer.db.MinerDAO;
import rssminer.jsoup.HtmlUtils;
import rssminer.sax.ExtractMainTextHandler;
import clojure.lang.Keyword;

public class Searcher {

    static final Version V = Version.LUCENE_35;
    static final Analyzer analyzer = new RssminerAnalyzer();
    public static final Logger logger = LoggerFactory
            .getLogger(Searcher.class);
    public static final String FEED_ID = "id";
    public static final String RSS_ID = "rid";
    public static final String AUTHOR = "author";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final String TAG = "tag";

    static final float AUTHOR_BOOST = 2;
    static final float TITLE_BOOST = 3;
    static final float TAG_BOOST = 2;
    static final float CONTENT_BOOST = 1;

    public static String[] FIELDS = new String[] { AUTHOR, TITLE, CONTENT,
            TAG };

    // cache it, String.intern is heavy
    static Term[] TERMS = new Term[] { new Term(TITLE), new Term(CONTENT) };

    static Term[] SIMPLE_SPLIT_TERM = new Term[] { new Term(TAG),
            new Term(AUTHOR) };

    // static Term CONTENT_TERM = new Term(CONTENT);
    static Term FEED_ID_TERM = new Term(FEED_ID);
    static Term RSS_ID_TERM = new Term(RSS_ID);

    private IndexWriter indexer = null;
    private final String path;
    private Map<Keyword, Object> config;
    private DataSource ds;

    private Map<String, Float> boost = new TreeMap<String, Float>();

    public static Searcher SEARCHER; // global

    public static void closeGlobalSearcher() {
        if (SEARCHER != null) {
            try {
                SEARCHER.close(false);
            } catch (Exception ignore) {
            }
            SEARCHER = null;
        }
    }

    public Map<String, Float> getBoost() {
        return boost;
    }

    public static Searcher initGlobalSearcher(String path,
            Map<Keyword, Object> config) throws IOException {
        closeGlobalSearcher();
        SEARCHER = new Searcher(config, path);
        return SEARCHER;
    }

    public static List<String> simpleSplit(String str) {
        ArrayList<String> strs = new ArrayList<String>(2);
        int start = -1;
        boolean splitter = true;
        char ch;
        for (int i = 0; i < str.length(); ++i) {
            ch = str.charAt(i);
            if (Character.isWhitespace(ch)
                    || Character.getType(ch) == OTHER_PUNCTUATION) {
                if (splitter == false) {
                    strs.add(str.substring(start + 1, i));
                }
                splitter = true;
                start = i;
            } else {
                splitter = false;
            }
        }
        if (start != str.length() - 1) {
            strs.add(str.substring(start + 1));
        }
        return strs;
    }

    private Searcher(Map<Keyword, Object> config, String path)
            throws IOException {
        final IndexWriterConfig cfg = new IndexWriterConfig(V, analyzer);
        this.path = path;
        this.config = config;

        this.ds = (DataSource) config.get(K_DATA_SOURCE);
        if (this.ds == null) {
            throw new NullPointerException("ds can not be null");
        }

        cfg.setOpenMode(OpenMode.CREATE_OR_APPEND);
        Directory dir = null;
        if (path.equals("RAM")) {
            dir = new RAMDirectory();
        } else {
            dir = FSDirectory.open(new File(path));
        }
        // used by classifier
        boost.put(AUTHOR, AUTHOR_BOOST);
        boost.put(TITLE, TITLE_BOOST);
        boost.put(CONTENT, CONTENT_BOOST);
        boost.put(TAG, TAG_BOOST);

        indexer = new IndexWriter(dir, cfg);
    }

    private Query buildQuery(String text, List<String> rssids)
            throws IOException {
        TokenStream stream = analyzer.tokenStream("", new StringReader(text));

        CharTermAttribute c = stream.getAttribute(CharTermAttribute.class);
        List<String> terms = new ArrayList<String>(4);
        while (stream.incrementToken()) {
            String term = new String(c.buffer(), 0, c.length());
            terms.add(term);
        }

        BooleanQuery q = new BooleanQuery();

        for (Term t : TERMS) {
            BooleanQuery part = new BooleanQuery();
            for (String term : terms) {
                part.add(new TermQuery(t.createTerm(term)), Occur.MUST);
            }
            // boost is set at index time
            // part.setBoost(2);
            q.add(part, Occur.SHOULD);
        }

        List<String> parts = simpleSplit(text);
        for (Term t : SIMPLE_SPLIT_TERM) {
            BooleanQuery part = new BooleanQuery();
            for (String term : parts) {
                part.add(new TermQuery(t.createTerm(term.toLowerCase())), Occur.MUST);
            }
            q.add(part, Occur.SHOULD);
        }

        BooleanQuery ids = new BooleanQuery();
        for (String rid : rssids) {
            ids.add(new TermQuery(RSS_ID_TERM.createTerm(rid)), Occur.SHOULD);
        }

        BooleanQuery query = new BooleanQuery();
        query.add(q, Occur.MUST);
        query.add(ids, Occur.MUST);
        return query;
    }

    public void close(boolean optimize) throws CorruptIndexException,
            IOException {
        if (indexer != null) {
            if (optimize) {
                logger.info("optimize index");
                indexer.forceMerge(1);
            }
            logger.info("close Searcher@" + path);
            indexer.close();
            indexer = null;
        }
    }

    private Document createDocument(int feeId, int rssID, String author,
            String title, String summary, String tags) throws IOException {
        Document doc = new Document();
        // not intern, already interned
        Field fid = new Field(FEED_ID, false, Integer.toString(feeId),
                Store.YES, Index.NOT_ANALYZED, TermVector.NO);
        doc.add(fid);

        Field rid = new Field(RSS_ID, false, Integer.toString(rssID),
                Store.NO, Index.NOT_ANALYZED, TermVector.NO);
        doc.add(rid);

        if (author != null && author.length() > 0) {
            List<String> authors = simpleSplit(author);
            for (String a : authors) {
                Field f = new Field(AUTHOR, false, a.toLowerCase(), Store.NO,
                        Index.NOT_ANALYZED, TermVector.YES);
                f.setBoost(AUTHOR_BOOST);
                doc.add(f);
            }
        }

        if (title != null) {
            Field f = new Field(TITLE, false, title, Store.NO,
                    Index.ANALYZED, TermVector.YES);
            f.setBoost(TITLE_BOOST);
            doc.add(f);
        }

        if (tags != null && tags.length() > 0) {
            List<String> ts = simpleSplit(tags);
            for (String tag : ts) {
                Field f = new Field(TAG, false, tag.toLowerCase(), Store.NO,
                        Index.NOT_ANALYZED, TermVector.YES);
                f.setBoost(TAG_BOOST);
                doc.add(f);
            }
        }

        if (summary != null) {
            try {
                // String content = Utils.extractText(summary);
                String content = HtmlUtils.summaryText(summary);
                Field f = new Field(CONTENT, false, content, Store.NO,
                        Index.ANALYZED, TermVector.YES);
                doc.add(f);
            } catch (Exception ignore) {
                logger.error("feed:" + feeId, ignore);
            }
        }
        return doc;
    }

    public int feedID2DocID(IndexSearcher searcher, int feedid)
            throws CorruptIndexException, IOException {
        TermQuery query = new TermQuery(FEED_ID_TERM.createTerm(Integer
                .toString(feedid)));
        TopDocs docs = searcher.search(query, 1);
        if (docs.totalHits == 1) {
            return docs.scoreDocs[0].doc;
        } else {
            return -1; // return -1, not found
        }
    }

    public int[] feedID2DocIDs(List<Integer> feeds)
            throws CorruptIndexException, IOException {
        int[] array = new int[feeds.size()];
        IndexReader reader = getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        for (int i = 0; i < feeds.size(); i++) {
            int l = feeds.get(i);
            array[i] = feedID2DocID(searcher, l);
        }
        reader.close();
        return array;
    }

    public IndexReader getReader() throws CorruptIndexException, IOException {
        return IndexReader.open(indexer, false);
    }

    public void index(int feeID, int rssID, String author, String title,
            String summary, String tags) throws CorruptIndexException,
            IOException {
        Document doc = createDocument(feeID, rssID, author, title, summary,
                tags);

        indexer.addDocument(doc);
    }

    // return feed ids
    public List<Feed> search(String term, int userID, int limit)
            throws CorruptIndexException, IOException, ParseException,
            SQLException {
        List<Integer> subids = DBHelper.getUserSubIDS(ds, userID);
        List<String> subs = new ArrayList<String>();
        for (Integer id : subids) {
            subs.add(Integer.toString(id));
        }
        return searchInSubIDs(term, userID, subs, limit);
    }

    // return feed ids
    public List<Feed> searchInSubIDs(String term, int userID,
            List<String> subids, int limit) throws CorruptIndexException,
            IOException, ParseException, SQLException {
        IndexReader reader = getReader();
        IndexSearcher searcher = new IndexSearcher(getReader());
        Query q = buildQuery(term, subids);
        TopDocs docs = searcher.search(q, limit);
        List<Integer> feedids = new ArrayList<Integer>(docs.scoreDocs.length);
        for (int i = 0; i < docs.scoreDocs.length; i++) {
            int docid = docs.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            feedids.add(Integer.valueOf(doc.get(FEED_ID)));
        }
        reader.close();
        if (feedids.isEmpty()) {
            return new ArrayList<Feed>(0);
        } else {
            MinerDAO db = new MinerDAO(config);
            return db.fetchFeedsWithScore(userID, feedids);
        }
    }

    public String toString() {
        return "Searcher@" + path;
    }

    public void updateIndex(int feedid, int rssID, String html) {
        if (html != null && !html.isEmpty()) {
            Parser p = Utils.parser.get();
            ExtractMainTextHandler h = new ExtractMainTextHandler();
            p.setContentHandler(h);
            try {
                p.parse(new InputSource(new StringReader(html)));
                String content = h.getContent();
                String title = h.getTitle();
                indexer.updateDocument(
                        FEED_ID_TERM.createTerm(Integer.toString(feedid)),
                        createDocument(feedid, rssID, null, title, content,
                                null));
            } catch (Exception e) {
                logger.info(e.getMessage(), e);
            }
        }
    }
}
