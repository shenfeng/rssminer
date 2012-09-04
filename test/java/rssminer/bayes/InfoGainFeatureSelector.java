package rssminer.bayes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfoGainFeatureSelector {

    private double pCategory;
    private List<Word> wordProbabilities;

    public void setPCategory(double pCategory) {
        this.pCategory = pCategory;
    }

    public void setWordProbabilities(Map<String, double[]> wordProbabilities) {
        this.wordProbabilities = new ArrayList<Word>();
        for (String word : wordProbabilities.keySet()) {
            double[] probabilities = wordProbabilities.get(word);
            this.wordProbabilities.add(new Word(word, probabilities[0],
                    probabilities[1]));
        }
    }

    public Map<String, double[]> selectFeatures() throws Exception {
        for (Word word : wordProbabilities) {
            if (word.pInCat > 0.0D) {
                word.infoGain = word.pInCat
                        * Math.log(word.pInCat
                                / ((word.pInCat + word.pNotInCat) * pCategory));
            } else {
                word.infoGain = 0.0D;
            }
        }
        Collections.sort(wordProbabilities);
        List<Word> topFeaturesList = wordProbabilities.subList(0,
                (int) Math.round(Math.sqrt(wordProbabilities.size())));
        Map<String, double[]> topFeatures = new HashMap<String, double[]>();
        for (Word topFeature : topFeaturesList) {
            topFeatures.put(topFeature.term, new double[] {
                    topFeature.pInCat, topFeature.pNotInCat });
        }
        return topFeatures;
    }

    private class Word implements Comparable<Word> {
        private String term;
        private double pInCat;
        private double pNotInCat;

        public double infoGain;

        public Word(String term, double pInCat, double pNotInCat) {
            this.term = term;
            this.pInCat = pInCat;
            this.pNotInCat = pNotInCat;
        }

        public int compareTo(Word o) {
            if (infoGain == o.infoGain) {
                return 0;
            } else {
                return infoGain > o.infoGain ? -1 : 1;
            }
        }

        public String toString() {
            return term + "(" + pInCat + "," + pNotInCat + ")=" + infoGain;
        }
    }
}