package rssminer.classfier;

import static rssminer.search.Searcher.SEARCHER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

class Holder implements Comparable<Holder> {

    int like = 0;
    int dislike = 0;
    int read = 0;

    public int compareTo(Holder o) {
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
        double l = 0.3, dis = 0.3; // default 0.3;

        if (like != 0 || read != 0) {
            l = like + read * 0.1;
        }
        if (dislike != 0) {
            dis = dislike;
        }
        return l / dis;
    }

    public String toString() {
        return String.format("[l=%d, r=%d, dis=%d, r=%.4f, t=%.4f]\n", like,
                read, dislike, getLogScore(), getScore());
    }
}

public class NaiveBayes {

    static final int MAX_FEATURE = 512;

    static Map<String, Double> calculate(Map<String, Holder> map,
            double totalLike, double totalDislike, int totalRead) {

        // keep most
        int size = map.size() < MAX_FEATURE ? map.size() : MAX_FEATURE;

        Map<String, Double> result = new HashMap<String, Double>(
                (int) (size / 0.75f) + 2); // load factor
        if (map.size() < MAX_FEATURE) {
            for (Entry<String, Holder> e : map.entrySet()) {
                Holder h = e.getValue();
                double r = h.getLogScore();
                result.put(e.getKey(), r);
            }
        } else {
            List<Entry<String, Holder>> list = new ArrayList<Map.Entry<String, Holder>>(
                    map.entrySet());
            Collections.sort(list, new ReverseValueCmp());
            List<Entry<String, Holder>> choosen = list
                    .subList(0, MAX_FEATURE);

            for (Entry<String, Holder> e : choosen) {
                Holder h = e.getValue();
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
                int[] freqs = vetor.getTermFrequencies();
                for (int i = 0; i < freqs.length; i++) {
                    String term = terms[i];
                    Double w = submodel.get(term);
                    if (w != null) {
                        // result *= (w * freqs[i]); Infinite
                        result += w * freqs[i];
                    }
                }
            }
        }
        return result;
    }

    public static double[] pick(double[] prefs, double likeRatio,
            double dislikeRatio) {
        int likeIndex = prefs.length - (int) (prefs.length * likeRatio);
        int disLikeIndex = (int) (prefs.length * dislikeRatio);
        likeIndex = likeIndex == prefs.length ? prefs.length - 1 : likeIndex;
        Arrays.sort(prefs);
        return new double[] { prefs[likeIndex], prefs[disLikeIndex] };
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
        Map<String, Holder> map = new HashMap<String, Holder>(2048);
        int toalLike = 0;
        int totalRead = 0;
        int totalDislike = 0;
        for (Vote vote : votes) {
            if (vote.docID == -1) {
                continue;
            }
            TermFreqVector termVector = reader.getTermFreqVector(vote.docID,
                    field);
            if (termVector != null) {
                String[] terms = termVector.getTerms();
                int[] freqs = termVector.getTermFrequencies();
                for (int j = 0; j < freqs.length; j++) {
                    String term = terms[j];
                    int count = freqs[j];
                    Holder h = map.get(term);
                    if (h == null) {
                        h = new Holder();
                    }
                    if (vote.vote == 1) { // like
                        h.like += count;
                        toalLike += count;
                    } else if (vote.vote == -1) { // dislike
                        h.dislike += count;
                        totalDislike += count;
                    } else { // read
                        totalRead += count;
                        h.read += count;
                    }
                    map.put(term, h);
                }
            }
        }
        return calculate(map, toalLike, totalDislike, totalRead);
    }
}

class ReverseValueCmp implements Comparator<Map.Entry<String, Holder>> {
    public int compare(Entry<String, Holder> o1, Entry<String, Holder> o2) {
        return -o1.getValue().compareTo(o2.getValue());
    }
}
