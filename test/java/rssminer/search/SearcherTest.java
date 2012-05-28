package rssminer.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.sound.midi.SysexMessage;

import junit.framework.Assert;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import rssminer.Searcher;

public class SearcherTest {

    Searcher searcher;

    @Before
    public void setup() throws IOException {
        searcher = Searcher.initGlobalSearcher("/var/rssminer/index");
    }

    @Test
    public void testSearch() throws CorruptIndexException, IOException,
            ParseException {
        List<Integer> rssids = new ArrayList<Integer>();
        Random r = new Random();
        int count = r.nextInt(450);
        for (int i = 1; i < count; i++) {
            rssids.add(i);
        }
        String[] result = searcher.search("java technology", rssids, 10);
        System.out.println(Arrays.toString(result));
    }

    @Test
    public void testSpaceSplit() {
        List<String> results = Searcher.simpleSplit("  a  b   c");
        System.out.println(results);
        for (String str : results) {
            System.out.println(str.length());
        }
        Assert.assertEquals(3, results.size());
        results = Searcher.simpleSplit("what; are you doing;");
        System.out.println(results);
        Assert.assertEquals(4, results.size());
        results = Searcher.simpleSplit("我所在的是10号车厢，满载118人，分排坐");
        System.out.println(results);
        Assert.assertEquals(3, results.size());
        results = Searcher.simpleSplit("a");
        Assert.assertEquals(1, results.size());

        results = Searcher.simpleSplit(" a ");
        Assert.assertEquals(1, results.size());

        results = Searcher.simpleSplit("");
        Assert.assertEquals(0, results.size());
    }
}
