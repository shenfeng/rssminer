package rssminer.search;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import rssminer.db.Feed;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SearcherTest {

    Searcher searcher;

    @Before
    public void setup() throws IOException {
        searcher = Searcher.initGlobalSearcher("/var/rssminer/index", null);
    }

    @Test
    public void testSearch() throws CorruptIndexException, IOException,
            ParseException, SQLException {
        List<String> rssids = new ArrayList<String>();
        Random r = new Random();
        int count = r.nextInt(450);
        for (int i = 1; i < count; i++) {
            rssids.add(i + "");
        }
        List<Feed> result = searcher.searchInSubIDs("java technology", 1,
                rssids, 10);
        // System.out.println(Arrays.toString(result));
    }

}
