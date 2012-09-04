package rssminer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.junit.Test;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

public class RssParserTest {

	public static String file = "/tmp/list";

	@Test
	public void testParse() throws IllegalArgumentException,
			FileNotFoundException, FeedException {
		SyndFeed s = new SyndFeedInput().build(new FileReader(file));
		@SuppressWarnings("unchecked")
        List<SyndEntry> entries = s.getEntries();
		for (SyndEntry en : entries) {
			System.out.println(en.getPublishedDate());
		}
	}
}
