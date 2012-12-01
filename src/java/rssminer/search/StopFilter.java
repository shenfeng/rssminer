/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import static java.lang.Character.DECIMAL_DIGIT_NUMBER;
import static java.lang.Character.LOWERCASE_LETTER;
import static java.lang.Character.getType;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class StopFilter extends FilteringTokenFilter {
    public static final CharArraySet STOP_WORDS_SET;

    static {
        final List<String> stopWords = Arrays.asList("an", "and", "are", "able", "about",
                "above", "according", "accordingly", "across", "actually", "after",
                "afterwards", "again", "against", "all", "allow", "allows", "almost", "alone",
                "along", "already", "also", "although", "always", "am", "among", "amongst",
                "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything",
                "anyway", "anyways", "anywhere", "apart", "appear", "appreciate",
                "appropriate", "are", "around", "as", "aside", "ask", "asking", "associated",
                "at", "available", "away", "awfully", "b", "be", "became", "because", "become",
                "becomes", "becoming", "been", "before", "beforehand", "behind", "being",
                "believe", "below", "beside", "besides", "best", "better", "between", "beyond",
                "both", "brief", "but", "by", "c", "came", "can", "cannot", "cant", "cause",
                "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come",
                "comes", "concerning", "consequently", "consider", "considering", "contain",
                "containing", "contains", "corresponding", "could", "course", "currently", "d",
                "definitely", "described", "despite", "did", "different", "do", "does",
                "doing", "done", "down", "downwards", "during", "e", "each", "edu", "eg",
                "eight", "either", "else", "elsewhere", "enough", "entirely", "especially",
                "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything",
                "everywhere", "ex", "exactly", "example", "except", "f", "far", "few", "fifth",
                "first", "five", "followed", "following", "follows", "for", "former",
                "formerly", "forth", "four", "from", "further", "furthermore", "g", "get",
                "gets", "getting", "given", "gives", "goes", "going", "gone", "got", "gotten",
                "greetings", "h", "had", "happens", "hardly", "has", "have", "having", "he",
                "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein",
                "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither",
                "hopefully", "how", "howbeit", "however", "i", "ie", "if", "ignored",
                "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated",
                "indicates", "inner", "insofar", "instead", "into", "inward", "is", "it",
                "its", "itself", "j", "just", "k", "keep", "keeps", "kept", "know", "knows",
                "known", "l", "last", "lately", "later", "latter", "latterly", "least", "less",
                "lest", "let", "like", "liked", "likely", "little", "look", "looking", "looks",
                "ltd", "m", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile",
                "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my",
                "myself", "n", "name", "namely", "nd", "near", "nearly", "necessary", "need",
                "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no",
                "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel",
                "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay",
                "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others",
                "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over",
                "overall", "own", "p", "particular", "particularly", "per", "perhaps",
                "placed", "please", "plus", "possible", "presumably", "probably", "provides",
                "q", "que", "quite", "qv", "r", "rather", "rd", "re", "really", "reasonably",
                "regarding", "regardless", "regards", "relatively", "respectively", "right",
                "s", "said", "same", "saw", "say", "saying", "says", "second", "secondly",
                "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self",
                "selves", "sensible", "sent", "serious", "seriously", "seven", "several",
                "shall", "she", "should", "since", "six", "so", "some", "somebody", "somehow",
                "someone", "something", "sometime", "sometimes", "somewhat", "somewhere",
                "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such",
                "sup", "sure", "t", "take", "taken", "tell", "tends", "th", "than", "thank",
                "thanks", "thanx", "that", "thats", "the", "their", "theirs", "them",
                "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore",
                "therein", "theres", "thereupon", "these", "they", "think", "third", "this",
                "thorough", "thoroughly", "those", "though", "three", "through", "throughout",
                "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried",
                "tries", "truly", "try", "trying", "twice", "two", "u", "un", "under",
                "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us",
                "use", "used", "useful", "uses", "using", "usually", "uucp", "v", "value",
                "various", "very", "via", "viz", "vs", "w", "want", "wants", "was", "way",
                "we", "welcome", "well", "went", "were", "what", "whatever", "when", "whence",
                "whenever", "where", "whereafter", "whereas", "whereby", "wherein",
                "whereupon", "wherever", "whether", "which", "while", "whither", "who",
                "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with",
                "within", "without", "wonder", "would", "would", "x", "y", "yes", "yet", "you",
                "your", "yours", "yourself", "yourselves", "zero", "as", "at", "be", "but",
                "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or",
                "such", "that", "the", "their", "then", "there", "these", "they", "this", "to",
                "was", "will", "with", "一", "与", "且", "个", "为", "么", "乎", "上", "中", "于", "人",
                "以", "们", "会", "但", "你", "到", "后", "对", "将", "就", "年", "我", "时", "是", "有", "来",
                "用", "而", "被", "这", "都", "在", "和", "了", "从", "吗", "吧", "的", "也", "要", "也", "里",
                "或", "该", "能", "把", "它", "地", "等", "是一", "一些", "这样", "如果", "我们", "一个", "可以",
                "这个", "已经", "月", "日");

        final CharArraySet stopSet = new CharArraySet(Version.LUCENE_33, stopWords.size(),
                false);
        stopSet.addAll(stopWords);
        STOP_WORDS_SET = CharArraySet.unmodifiableSet(stopSet);
    }

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    public StopFilter(TokenStream input) {
        super(true, input);
    }

    @Override
    protected boolean accept() throws IOException {
        char[] buffer = termAtt.buffer();
        int length = termAtt.length();
        if (length == 1) {
            int type = getType(buffer[0]);
            // ignore number or char, is lowercased by upper logic
            if (type == DECIMAL_DIGIT_NUMBER || type == LOWERCASE_LETTER) {
                return false;
            }
        }
        return !STOP_WORDS_SET.contains(buffer, 0, length);
    }
}
