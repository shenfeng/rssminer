package rssminer.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

class Int {
    int i;

    public Int(int count) {
        this.i = count;
    }
}

class Item implements Comparable<Item> {
    public final String term;
    public final Int count;

    public Item(String term, Int count) {
        this.term = term;
        this.count = count;
    }

    public int compareTo(Item o) {
        return o.count.i - count.i;
    }

    public String toString() {
        return term + "=" + count;
    }
}

class Counter {
    private final int max;
    private final Map<String, Int> map;

    public Counter(int max) {
        this.max = max;
        this.map = new HashMap<String, Int>((int) (max / 0.75) + 2);
    }

    public void add(String term, int count) {
        Int c = map.get(term);
        if (map.size() >= max) {
            if (c != null) {
                c.i += count;
            }
        } else {
            if (c == null) {
                map.put(term, new Int(count));
            } else {
                c.i += count;
            }
        }
    }

    public Map<String, Integer> getTop(int top) {
        top = Math.min(map.size(), top);
        Item[] all = new Item[map.size()];
        int idx = 0;
        for (Entry<String, Int> e : map.entrySet()) {
            all[idx++] = new Item(e.getKey(), e.getValue());
        }
        Arrays.sort(all);
        Map<String, Integer> r = new TreeMap<String, Integer>();
        for (int j = 0; j < top; ++j) {
            r.put(all[j].term, all[j].count.i);
        }
        return r;
    }
}

public class FacetCollector extends Collector {
    private int base = 0;
    private int count = 0;
    private int step = 1;
    private IndexReader reader;
    private final Counter author = new Counter(1024);
    private final Counter tag = new Counter(1024);

    private static final int STEP = 3078;
    private static final int STEP2 = 10240;

    public void setScorer(Scorer scorer) throws IOException {
    }

    public void collect(int doc) throws IOException {
        int id = doc + base;
        count += 1;

        if (count > STEP2) {
            step = 4;
            if (count % 4 != 0) { // 25%
                return;
            }
        } else if (count > STEP) {
            step = 2;
            if (count % 2 != 0) { // 50%
                return;
            }
        }

        TermFreqVector tv = reader.getTermFreqVector(id, Searcher.AUTHOR);
        if (tv != null) {
            String[] terms = tv.getTerms();
            for (String t : terms) {
                author.add(t, step);
            }
        }

        tv = reader.getTermFreqVector(id, Searcher.TAG);
        if (tv != null) {
            String[] terms = tv.getTerms();
            for (String t : terms) {
                tag.add(t, step);
            }
        }
    }

    public Map<String, Integer> getAuthor(int limit) {
        return author.getTop(limit);
    }

    public Map<String, Integer> getTag(int limit) {
        return tag.getTop(limit);
    }

    public void setNextReader(IndexReader reader, int docBase)
            throws IOException {
        this.base = docBase;
        this.reader = reader;
    }

    public boolean acceptsDocsOutOfOrder() {
        return true;
    }
}
