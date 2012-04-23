package rssminer.classfier;

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

import rssminer.Searcher;
import clojure.lang.ISeq;

class Holder implements Comparable<Holder> {

    double like = 0.3;
    double dislike = 0.3;

    public int compareTo(Holder o) {
        double left = Math.abs(Math.log(getRatio()));
        double right = Math.abs(Math.log(o.getRatio()));
        if (left > right) {
            return 1;
        } else if (left == right) {
            return 0;
        } else {
            return -1;
        }
    }

    public double getRatio() {
        return like / dislike;
    }

    public String toString() {
        return "[like=" + like + ", dislike=" + dislike + ", ratio="
                + getRatio() + "]";
    }

}

public class NaiveBayes {

    static final int MAX_FEATURE = 512;

    static Map<String, Double> calculate(Map<String, Holder> map,
            double totalLike, double totalDislike) {

        // keep most
        int size = map.size() < MAX_FEATURE ? map.size() : MAX_FEATURE;

        Map<String, Double> result = new HashMap<String, Double>(
                (int) (size / 0.75f) + 2); // load factor
        if (map.size() < MAX_FEATURE) {
            for (Entry<String, Holder> e : map.entrySet()) {
                Holder h = e.getValue();
                double r = h.getRatio();
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
                double r = h.getRatio();
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
                        result += Math.log(w) * freqs[i];
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

    public static double[] classify(Searcher searcher,
            Map<String, Map<String, Double>> model, ISeq feeds)
            throws CorruptIndexException, IOException {
        int[] ids = searcher.feedID2DocIDs(feeds);
        double[] result = new double[ids.length];
        IndexReader reader = searcher.getReader();
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

    public static Map<String, Map<String, Double>> train(Searcher searcher,
            ISeq like, ISeq dislike) throws CorruptIndexException,
            IOException {
        int[] likes = searcher.feedID2DocIDs(like);
        int[] dislikes = searcher.feedID2DocIDs(dislike);
        IndexReader reader = searcher.getReader();
        Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();
        for (String field : Searcher.FIELDS) {
            Map<String, Double> sub = trainField(reader, likes, dislikes,
                    field);
            result.put(field, sub);
        }
        return result;
    }

    private static Map<String, Double> trainField(IndexReader reader,
            int[] likes, int[] dislikes, String field) throws IOException,
            CorruptIndexException {
        Map<String, Holder> map = new HashMap<String, Holder>();
        int toalLike = 0;
        for (int id : likes) {
            if (id != -1) {
                TermFreqVector termVector = reader.getTermFreqVector(id,
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
                        toalLike += count;
                        h.like += count;
                        map.put(term, h);
                    }
                }
            }
        }

        int totalDislike = 0;
        for (int id : dislikes) {
            if (id != -1) {
                TermFreqVector termVector = reader.getTermFreqVector(id,
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
                        h.dislike += count;
                        totalDislike += count;
                        map.put(term, h);
                    }
                }
            }
        }
        return calculate(map, toalLike, totalDislike);
    }
}

class ReverseValueCmp implements Comparator<Map.Entry<String, Holder>> {
    public int compare(Entry<String, Holder> o1, Entry<String, Holder> o2) {
        return -o1.getValue().compareTo(o2.getValue());
    }
}
