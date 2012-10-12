package rssminer.search;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

public class SearcherTest {

	Searcher searcher;

	@Before
	public void setup() throws IOException {
		searcher = Searcher.initGlobalSearcher("/var/rssminer/index", null);
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
	
	public static void main(String[] args) {
		String[] strings = "abc; sdfsdf".split(";\\s");
		for (String string : strings) {
			
			System.out.println(string);
		}
	}

}
