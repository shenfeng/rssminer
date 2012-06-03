package rssminer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import rssminer.search.Searcher;

public class SearcherTest {

	private String html;
	Searcher searcher;

	@Before
	public void setup() throws FileNotFoundException, IOException {
		html = IOUtils.toString(new FileInputStream(
				"/tmp/What-s-new-in-Linux-3-2-1400680.html"));
		searcher = Searcher.initGlobalSearcher("RAM", null);
	}

	@Test
	public void testUpdateFeed() {
		for (int i = 0; i < 10000; i++) {
			searcher.updateIndex(1, 1, html);
		}
	}

}
