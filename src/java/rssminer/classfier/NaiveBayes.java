/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.classfier;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.IndexSearcher;
import rssminer.db.Vote;
import rssminer.search.Searcher;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static rssminer.search.Searcher.SEARCHER;

class TermFeature implements Comparable<TermFeature> {

    double like = 0;
    double dislike = 0;
    double read = 0;

    static final double LIKE_DEFAULT = 0.2;
    static final double DISLIKE_DEFAULT = 0.2;
    static final double READ_AS_LIKE_RATIO = 0.15;

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
        double l = LIKE_DEFAULT, dis = DISLIKE_DEFAULT; //
        if (like != 0 || read != 0) {
            l = like + read * READ_AS_LIKE_RATIO;
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
    static final int MIN_DF = 3;

    static Map<String, Double> pickTop(Map<String, TermFeature> map) {
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
            int index = 0;
            for (Entry<String, TermFeature> e : map.entrySet()) {
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
        int total = reader.numDocs();
        for (Term field : Searcher.ALL_FIELDS) {
            Map<String, Double> submodel = model.get(field.field());
            TermFreqVector vetor = reader.getTermFreqVector(docid, field.field());
            if (vetor != null) {
                double score = 1.0D;
                String[] terms = vetor.getTerms();
                int[] freqs = vetor.getTermFrequencies();
                for (int i = 0; i < freqs.length; i++) {
                    String text = terms[i];
                    Double w = submodel.get(text);
                    if (w != null) {
                        int df = reader.docFreq(field.createTerm(text));
                        double tfidf = freqs[i] * Math.log(total / df);
                        score += w * tfidf;
                    }
                }
                score *= SEARCHER.getBoost().get(field.field()); // boost
                int length = terms.length;
                if (length > 1) {
                    // make long article less significant
                    score = score / Math.log(length);
                }
                result += score;
            }
        }
        return result;
    }

    public static double[] classify(Map<String, Map<String, Double>> model,
                                    List<Integer> feeds) throws IOException {
        int[] ids = SEARCHER.feedID2DocIDs(feeds);
        double[] result = new double[ids.length];
        IndexReader reader = SEARCHER.openReader();
        try {
            for (int i = 0; i < ids.length; i++) {
                int id = ids[i];
                if (id != -1) {
                    result[i] = classfiy(model, reader, id);
                } else {
                    result[i] = 1;
                }
            }
        } finally {
            reader.decRef();
        }
        return result;
    }

    public static double classify(Map<String, Map<String, Double>> model,
                                  int feedid) throws IOException {
        IndexReader reader = SEARCHER.openReader();
        try {
            IndexSearcher searcher = new IndexSearcher(reader);
            int docid = SEARCHER.feedID2DocID(searcher, feedid);
            double score = classfiy(model, reader, docid);
            return score;
        } finally {
            reader.decRef();
        }
    }

    public static Map<String, Map<String, Double>> train(List<Vote> votes)
            throws IOException {
        // get ids
        List<Integer> feedids = new ArrayList<Integer>(votes.size());
        for (Vote vote : votes) {
            feedids.add(vote.feedID);
        }
        int[] docIDs = SEARCHER.feedID2DocIDs(feedids);
        for (int i = 0; i < docIDs.length; i++) {
            votes.get(i).setDocID(docIDs[i]);
        }
        IndexReader reader = SEARCHER.openReader();
        try {
            Map<String, Map<String, Double>> result = new HashMap<String, Map<String, Double>>();
            for (Term field : Searcher.ALL_FIELDS) {
                Map<String, Double> sub = trainField(reader, votes, field);
                result.put(field.field(), sub);
            }
            return result;

        } finally {
            reader.decRef();
        }
    }

    private static Map<String, Double> trainField(IndexReader reader,
                                                  List<Vote> votes, Term field) throws IOException {
        int total = reader.numDocs();
        Map<String, TermFeature> map;
        if (Searcher.CONTNET_TERM.equals(field)) {
            map = new HashMap<String, TermFeature>(20480);
        } else {
            map = new HashMap<String, TermFeature>(768);
        }
        for (Vote vote : votes) {
            if (vote.docID == -1) {
                continue;
            }
            TermFreqVector termVector = reader.getTermFreqVector(vote.docID,
                    field.field());
            if (termVector != null) {
                String[] terms = termVector.getTerms();
                int[] freqs = termVector.getTermFrequencies();

                for (int j = 0; j < freqs.length; j++) {
                    String text = terms[j];
                    int df = reader.docFreq(field.createTerm(text));
                    if (df > MIN_DF) {
                        double tfidf = freqs[j] * Math.log(total / df);
//                    int count = freqs[j];
                        TermFeature h = map.get(text);
                        if (h == null) {
                            h = new TermFeature();
                            map.put(text, h);
                        }
                        if (vote.vote == 1) { // like
                            h.like += tfidf;
                        } else if (vote.vote == -1) { // dislike
                            h.dislike += tfidf;
                        } else { // read
                            h.read += tfidf;
                        }
                    }
                }
            }
        }
        return pickTop(map);
    }
}
