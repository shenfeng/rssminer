package rssminer.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
}
