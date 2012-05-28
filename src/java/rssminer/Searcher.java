package rssminer;

import static java.lang.Character.OTHER_PUNCTUATION;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

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
import org.xml.sax.SAXException;

import rssminer.sax.ExtractMainTextHandler;

public class Searcher {

    static final Version V = Version.LUCENE_35;
    static final Analyzer analyzer = new KStemStopAnalyzer();
    static final Logger logger = LoggerFactory.getLogger(Searcher.class);
    static final String FEED_ID = "id";
    static final String RSS_ID = "rid";
    static final String AUTHOR = "author";
    static final String TITLE = "title";
    static final String CONTENT = "content";
    static final String TAG = "tag";

    public static String[] FIELDS = new String[] { AUTHOR, TITLE, CONTENT,
            TAG };

    // cache it, String.intern is heavy
    static Term[] TERMS = new Term[] { new Term(AUTHOR), new Term(TITLE),
            new Term(TAG), new Term(CONTENT) };

    // static Term CONTENT_TERM = new Term(CONTENT);
    static Term FEED_ID_TERM = new Term(FEED_ID);
    static Term RSS_ID_TERM = new Term(RSS_ID);

    private IndexWriter indexer = null;
    private final String path;

    public void toggleInfoStream(boolean toggle) throws IOException {
        if (toggle) {
            indexer.setInfoStream(System.out);
        } else {
            indexer.setInfoStream(null);
        }
    }

    public static Searcher SEARCHER; // global

    public static Searcher initGlobalSearcher(String path) throws IOException {
        closeGlobalSearcher();
        SEARCHER = new Searcher(path);
        return SEARCHER;
    }

    public static void closeGlobalSearcher() {
        if (SEARCHER != null) {
            try {
                SEARCHER.close();
            } catch (Exception ignore) {
            }
            SEARCHER = null;
        }
    }

    private Searcher(String path) throws IOException {
        final IndexWriterConfig cfg = new IndexWriterConfig(V, analyzer);
        this.path = path;
        cfg.setOpenMode(OpenMode.CREATE_OR_APPEND);
        Directory dir = null;
        if (path.equals("RAM")) {
            dir = new RAMDirectory();
        } else {
            dir = FSDirectory.open(new File(path));
        }
        indexer = new IndexWriter(dir, cfg);
    }

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

    public void index(int feeID, int rssID, String author, String title,
            String summary, String tags) throws CorruptIndexException,
            IOException {
        try {
            if (summary != null)
                summary = Utils.extractText(summary);
        } catch (SAXException ignore) {
        }
        Document doc = createDocument(feeID, rssID, author, title, summary,
                tags);

        indexer.addDocument(doc);
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
            List<String> as = simpleSplit(author);
            for (String au : as) {
                Field a = new Field(AUTHOR, false, au.toLowerCase(),
                        Store.NO, Index.NOT_ANALYZED, TermVector.YES);
                a.setBoost(2f);
                doc.add(a);
            }
        }

        if (title != null) {
            Field t = new Field(TITLE, false, title, Store.NO,
                    Index.ANALYZED, TermVector.YES);
            t.setBoost(3f);
            doc.add(t);
        }

        if (tags != null && tags.length() > 0) {
            List<String> ts = simpleSplit(tags);
            for (String tag : ts) {
                Field f = new Field(TAG, false, tag.toLowerCase(), Store.NO,
                        Index.NOT_ANALYZED, TermVector.YES);
                f.setBoost(2);
                doc.add(f);
            }
        }

        if (summary != null) {
            try {
                String content = Utils.extractText(summary);
                Field c = new Field(CONTENT, false, content, Store.NO,
                        Index.ANALYZED, TermVector.YES);
                doc.add(c);
            } catch (SAXException ignore) {
            }
        }
        return doc;
    }

    public IndexReader getReader() throws CorruptIndexException, IOException {
        return IndexReader.open(indexer, false);
    }

    private Query buildQuery(String text, List<Integer> rssids)
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

        BooleanQuery ids = new BooleanQuery();
        for (Integer rid : rssids) {
            ids.add(new TermQuery(RSS_ID_TERM.createTerm(rid.toString())),
                    Occur.SHOULD);
        }

        BooleanQuery query = new BooleanQuery();
        query.add(q, Occur.MUST);
        query.add(ids, Occur.MUST);

        return query;
    }

    // return feed ids
    public String[] search(String term, List<Integer> rssids, int count)
            throws CorruptIndexException, IOException, ParseException {
        IndexReader reader = getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        Query q = buildQuery(term, rssids);
        TopDocs docs = searcher.search(q, count);
        String[] array = new String[docs.scoreDocs.length];
        for (int i = 0; i < docs.scoreDocs.length; i++) {
            int docid = docs.scoreDocs[i].doc;
            Document doc = searcher.doc(docid);
            array[i] = doc.get(FEED_ID);
        }
        return array;
    }

    public int[] feedID2DocIDs(List<Integer> feeds)
            throws CorruptIndexException, IOException {
        int[] array = new int[feeds.size()];
        IndexSearcher searcher = new IndexSearcher(getReader());
        for (int i = 0; i < feeds.size(); i++) {
            int l = feeds.get(i);
            TermQuery query = new TermQuery(new Term(FEED_ID,
                    Integer.toString(l)));
            TopDocs docs = searcher.search(query, 1);
            if (docs.totalHits == 1) {
                array[i] = docs.scoreDocs[0].doc;
            } else {
                array[i] = -1; // return -1, not found
            }
        }
        return array;
    }
}
