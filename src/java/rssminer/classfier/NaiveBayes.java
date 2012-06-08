package rssminer.classfier;

import static rssminer.search.Searcher.SEARCHER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;

import rssminer.db.Vote;
import rssminer.search.Searcher;

class TermFeature implements Comparable<TermFeature> {

    double like = 0;
    double dislike = 0;
    double read = 0;

    public int compareTo(TermFeature o) {
        double left = Math.abs(getLogScore());
        double right = Math.abs(o.getLogScore());
        if (left > right) {
            return 1;
        } else if (left == right) {
            return 0;
        } else {
            return -1;
        }
    }

    public double getLogScore() {
        return Math.log(getScore());
    }

    private double getScore() {
        // term frequency => term / Math.sqrt(length)
        // if a doc has lot of terms, then term frequency may be low. has some
        // read, but no dislike, the score should not be negative
        double l = 0.018, dis = 0.018; // default 0.018; 3086 terms
        if (like != 0 || read != 0) {
            l = like + read * 0.1;
        }
        if (dislike != 0) {
            dis = dislike;
        }
        return l / dis;
    }

    public String toString() {
        return String.format("[l=%.4f, r=%.4f, dis=%.4f, r=%.4f, t=%.4f]",
                like, read, dislike, getLogScore(), getScore());
    }
}

public class NaiveBayes {

    static final int MAX_FEATURE = 512;

    static Map<String, Double> pick(Map<String, TermFeature> map) {
        Map<String, Double> result = new HashMap<String, Double>(
                (int) (MAX_FEATURE / 0.75f) + 2); // load factor
        if (map.size() < MAX_FEATURE) {
            for (Entry<String, TermFeature> e : map.entrySet()) {
                TermFeature h = e.getValue();
                double r = h.getLogScore();
                result.put(e.getKey(), r);
            }
        } else {
            List<Entry<String, TermFeature>> list = new ArrayList<Map.Entry<String, TermFeature>>(
                    map.entrySet());
            Collections.sort(list, new ReverseValueCmp());
            List<Entry<String, TermFeature>> choosen = list.subList(0,
                    MAX_FEATURE);
            // for (Entry<String, TermFeature> entry : choosen) {
            // System.out.println(entry.getKey() + "\t" + entry.getValue());
            // }
            for (Entry<String, TermFeature> e : choosen) {
                TermFeature h = e.getValue();
                double r = h.getLogScore();
                result.put(e.getKey(), r);
            }
        }
        return result;
    }

    private static double classfiy(Map<String, Map<String, Double>> model,
            IndexReader reader, int docid) throws IOException {
        double result = 1.0D;
        for (String field : Searcher.FIELDS) {
            Map<String, Double> submodel = model.get(field);
            TermFreqVector vetor = reader.getTermFreqVector(docid, field);
            if (vetor != null) {
                String[] terms = vetor.getTerms();
                double n = Math.sqrt(terms.length); // try to normalize term
                                                    // length
                int[] freqs = vetor.getTermFrequencies();
                for (int i = 0; i < freqs.length; i++) {
                    String term = terms[i];
                    Double w = submodel.get(term);
                    if (w != null) {
                        // result *= (w * freqs[i]); Infinite
                        result += w * freqs[i] / n;
                    }
                }
            }
        }
        return result;
    }

    public static double[] classify(Map<String, Map<String, Double>> model,
            List<Integer> feeds) throws CorruptIndexException, IOException {
        int[] ids = SEARCHER.feedID2DocIDs(feeds);
        double[] result = new double[ids.length];
        IndexReader reader = SEARCHER.getReader();
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            if (id != -1) {
                result[i] = classfiy(model, reader, id);
            } else {
                result[i] = 1;
            }
        }
        return result;
    }

    public static double classify(Map<String, Map<String, Double>> model,
            int feedid) throws CorruptIndexException, IOException {
        IndexReader reader = SEARCHER.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        int docid = SEARCHER.feedID2DocID(searcher, feedid);
        return classfiy(model, reader, docid);
    }

    public static Map<String, Map<String, Double>> train(List<Vote> votes)
            throws CorruptIndexException, IOException {
        // get ids
        List<Integer> feedids = new ArrayList<Integer>(votes.size());
        for (Vote vote : votes) {
            feedids.add(vote.feedID);
        }
        int[] docIDs = SEARCHER.feedID2DocIDs(feedids);
        for (int i = 0; i < docIDs.length; i++) {
            votes.get(i).setDocID(docIDs[i]);
        }
        IndexReader reader = SEARCHER.getReader();

        Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();
        for (String field : Searcher.FIELDS) {
            Map<String, Double> sub = trainField(reader, votes, field);
            result.put(field, sub);
        }
        return result;
    }

    private static Map<String, Double> trainField(IndexReader reader,
            List<Vote> votes, String field) throws IOException,
            CorruptIndexException {
        Map<String, TermFeature> map = null;
        if (Searcher.CONTENT.equals(field)) {
            map = new HashMap<String, TermFeature>(10240);
        } else {
            map = new HashMap<String, TermFeature>(768);
        }
        for (Vote vote : votes) {
            if (vote.docID == -1) {
                continue;
            }
            TermFreqVector termVector = reader.getTermFreqVector(vote.docID,
                    field);
            if (termVector != null) {
                String[] terms = termVector.getTerms();
                int[] freqs = termVector.getTermFrequencies();

                // make terms length less impact
                double n = SEARCHER.getBoost().get(field)
                        / Math.sqrt(terms.length); // try to normalize
                // // data
                // System.out.println(field + "\t" + n + "\t" + terms.length
                // + "\t" + boost);
                for (int j = 0; j < freqs.length; j++) {
                    String term = terms[j];
                    int count = freqs[j];
                    TermFeature h = map.get(term);
                    if (h == null) {
                        h = new TermFeature();
                    }
                    if (vote.vote == 1) { // like
                        h.like += count * n;
                    } else if (vote.vote == -1) { // dislike
                        h.dislike += count * n;
                    } else { // read
                        h.read += count * n;
                    }
                    map.put(term, h);
                }
            }
        }
        // System.out.println(field + "\t" + map.size());
        return pick(map);
    }
}

class ReverseValueCmp implements Comparator<Map.Entry<String, TermFeature>> {
    public int compare(Entry<String, TermFeature> o1,
            Entry<String, TermFeature> o2) {
        return -o1.getValue().compareTo(o2.getValue());
    }
}
