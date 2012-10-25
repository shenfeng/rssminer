/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.search;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import me.shenfeng.mmseg.SimpleMMsegTokenizer;
import me.shenfeng.mmseg.StringReader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class SearcherTest {

    Searcher searcher;

    @Before
    public void setup() throws IOException {
        // searcher = Searcher.initGlobalSearcher("/var/rssminer/index", null);
    }

    @Test
    public void testSearch() throws CorruptIndexException, IOException,
            ParseException, SQLException {
        List<Integer> rssids = new ArrayList<Integer>();
        Random r = new Random();
        int count = r.nextInt(450);
        for (int i = 1; i < count; i++) {
            rssids.add(i);
        }
        // List<Feed> result = searcher.search("java technology", 1, 10);
        // System.out.println(Arrays.toString(result));
    }

    private void print(String input) throws IOException {
        StringBuilder sb = new StringBuilder();
        TokenStream stream = Searcher.analyzer.tokenStream("",
                new StringReader(input));
        CharTermAttribute termAtt = stream
                .getAttribute(CharTermAttribute.class);
        while (stream.incrementToken()) {
            String word = new String(termAtt.buffer(), 0, termAtt.length());
            sb.append(word).append("|");
        }
        System.out.println(input + " => " + sb.toString());
    }

    @Test
    public void testSeg() throws IOException {
        print("[漫猫字幕组][TARI TARI][01][GB][1280x720][10bit][mp4]");
    }

    public static void main(String[] args) {
        String[] strings = "abc; sdfsdf".split(";\\s");
        for (String string : strings) {

            System.out.println(string);
        }
    }

}
