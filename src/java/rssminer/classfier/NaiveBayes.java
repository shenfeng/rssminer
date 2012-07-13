package rssminer.classfier;

import static rssminer.search.Searcher.SEARCHER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
        double l = 0.1, dis = 0.1; //
        if (like != 0 || read != 0) {
            l = like + read * 0.15;
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

class TermScoreEntry implements Comparable<TermScoreEntry> {
    String term;
    double score;

    public int compareTo(TermScoreEntry o) {
        if (o.score > score) {
            return 1;
        } else if (o.score < score) {
            return -1;
        }
        return 0;
    }

    public TermScoreEntry(String term, double score) {
        this.term = term;
        this.score = score;
    }

    public String toString() {
        return term + ":" + score;
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
            TermScoreEntry all[] = new TermScoreEntry[map.size()];
            Set<Entry<String, TermFeature>> entries = map.entrySet();
            int index = 0;
            for (Entry<String, TermFeature> e : entries) {
                all[index] = new TermScoreEntry(e.getKey(), e.getValue()
                        .getLogScore());
                index += 1;
            }
            Arrays.sort(all);
            for (int j = 0; j < MAX_FEATURE; j++) {
                TermScoreEntry e = all[j];
                result.put(e.term, e.score);
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
                double score = 1.0D;
                String[] terms = vetor.getTerms();
                int[] freqs = vetor.getTermFrequencies();
                for (int i = 0; i < freqs.length; i++) {
                    String term = terms[i];
                    Double w = submodel.get(term);
                    if (w != null) {
                        score += w * freqs[i];
                    }
                }
                score *= SEARCHER.getBoost().get(field); // boost
                result += score;
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
        reader.close();
        return result;
    }

    public static double classify(Map<String, Map<String, Double>> model,
            int feedid) throws CorruptIndexException, IOException {
        IndexReader reader = SEARCHER.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        int docid = SEARCHER.feedID2DocID(searcher, feedid);
        double score = classfiy(model, reader, docid);
        reader.close();
        return score;
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
        reader.close();
        return result;
    }

    private static Map<String, Double> trainField(IndexReader reader,
            List<Vote> votes, String field) throws IOException,
            CorruptIndexException {
        Map<String, TermFeature> map = null;
        if (Searcher.CONTENT.equals(field)) {
            map = new HashMap<String, TermFeature>(20480);
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

                for (int j = 0; j < freqs.length; j++) {
                    String term = terms[j];
                    int count = freqs[j];
                    TermFeature h = map.get(term);
                    if (h == null) {
                        h = new TermFeature();
                    }
                    if (vote.vote == 1) { // like
                        h.like += count;
                    } else if (vote.vote == -1) { // dislike
                        h.dislike += count;
                    } else { // read
                        h.read += count;
                    }
                    map.put(term, h);
                }
            }
        }
        return pick(map);
    }
}
