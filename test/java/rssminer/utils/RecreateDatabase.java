package rssminer.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrawlerLink {
	int id;
	String url;
	String domain;
	Timestamp added_ts;
	String title;
	int next_check_ts;
	String last_modofied;
	int check_interval;
	int referer_id;
	List<RssLink> links;
}

class Feed {
	int id;
	String author;
	String link;
	String title;
	String summary;
	String snippet;
	String updated_ts;
	String published_ts;
	int rss_link_id;
}

class FeedTag {
	String tag;
	int user_id;
	int feed_id;
}

class User {
	int id;
	String email;
	String name;
	String password;
	String authen_token;
	Timestamp added_ts;
}

class userSubscription {
	int user_id;
	int rss_link_id;
	String title;
	Timestamp added_ts;
}

class UserFeed {
	int user_id;
	int feed_id;
	boolean read;
	String pref;
	int read_date;
}

public class RecreateDatabase {

	final static String DEST = "/home/feng/Downloads/rssminer";
	final static String SRC = "/media/disk/rssminer/rssminer";
	final static String OPTIONS = ";LOG=0;CACHE_SIZE=65536;LOCK_MODE=0;UNDO_LOG=0";
	private static final Logger logger = LoggerFactory
			.getLogger(RecreateDatabase.class);

	public static void main(String[] args) {
		try {
			StopWatch w = new StopWatch().start();
			File f = new File(DEST).getParentFile();

			for (File file : f.listFiles()) {
				if (file.getName().startsWith("rssminer.")) {
					System.out.printf("delete %s, %s\n", file.getName(),
							file.delete());
				}
			}

			RecreateDatabase r = new RecreateDatabase();
			r.createDatabase();
			r.insertData();

			w.stop();
			System.out.println("all " + w.elipseMs() + "ms");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createDatabase() throws IOException, SQLException {
		InputStream is = RecreateDatabase.class.getClassLoader()
				.getResourceAsStream("rssminer.sql");
		String sql = IOUtils.toString(is);
		Connection con = DriverManager.getConnection("jdbc:h2:" + DEST
				+ OPTIONS, "sa", "");
		Statement stat = con.createStatement();

		for (String str : sql.split("\\s*----*\\s*")) {
			str = str.trim();
			if (!str.startsWith("insert") && !str.startsWith("--")) {
				stat.addBatch(str);
			}
		}

		stat.executeBatch();
		stat.close();
		con.close();
	}

	public List<User> getUsers() throws SQLException {
		List<User> users = new ArrayList<User>();

		Connection con = getSrcConnection();
		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from users");
		while (rs.next()) {
			User u = new User();
			u.id = rs.getInt("id");
			u.email = rs.getString("email");
			u.name = rs.getString("name");
			u.password = rs.getString("password");
			u.authen_token = rs.getString("authen_token");
			u.added_ts = rs.getTimestamp("added_ts");
			users.add(u);
		}

		return users;
	}

	public List<userSubscription> getUserSubscriptions() throws SQLException {
		List<userSubscription> users = new ArrayList<userSubscription>();

		Connection con = getSrcConnection();
		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from user_subscription");
		while (rs.next()) {
			userSubscription u = new userSubscription();
			u.user_id = rs.getInt("user_id");
			u.rss_link_id = rs.getInt("rss_link_id");
			u.title = rs.getString("title");
			u.added_ts = rs.getTimestamp("added_ts");
			users.add(u);
		}

		return users;
	}

	public List<UserFeed> getUserFeed() throws SQLException {
		List<UserFeed> users = new ArrayList<UserFeed>();

		Connection con = getSrcConnection();
		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from user_feed");
		while (rs.next()) {
			UserFeed u = new UserFeed();
			u.feed_id = rs.getInt("feed_id");
			u.user_id = rs.getInt("user_id");
			u.read = rs.getBoolean("read");
			u.pref = rs.getString("pref");
			u.read_date = rs.getInt("read_date");
			users.add(u);
		}

		return users;
	}

	public List<CrawlerLink> getCrawlerLinks() throws SQLException {
		Connection con = getSrcConnection();
		StopWatch w = new StopWatch().start();

		int count = getSize(con, "crawler_links");
		List<CrawlerLink> links = new ArrayList<CrawlerLink>(count);

		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from crawler_links");
		while (rs.next()) {
			CrawlerLink link = new CrawlerLink();
			link.id = rs.getInt("id");
			link.url = rs.getString("url");
			link.domain = rs.getString("domain");
			link.added_ts = rs.getTimestamp("added_ts");
			link.title = rs.getString("title");
			link.next_check_ts = rs.getInt("next_check_ts");
			link.check_interval = rs.getInt("check_interval");
			link.referer_id = rs.getInt("referer_id");

			links.add(link);
		}

		w.stop();
		System.out.println("get crawlerLinks " + w.elipseMs() + "ms");
		con.close();
		return links;

	}

	public List<Feed> getFeeds() throws SQLException, IOException {

		StopWatch w = new StopWatch().start();
		Connection con = getSrcConnection();
		int count = getSize(con, "feeds");
		List<Feed> feeds = new ArrayList<Feed>(count);

		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from feeds");
		while (rs.next()) {
			Feed f = new Feed();
			f.id = rs.getInt("id");
			f.author = rs.getString("author");
			f.link = rs.getString("link");
			f.title = rs.getString("title");
			Clob s = rs.getClob("summary");
			if (s != null) {
				f.summary = IOUtils.toString(s.getCharacterStream());
			}
			f.snippet = rs.getString("snippet");
			f.updated_ts = rs.getString("updated_ts");
			f.published_ts = rs.getString("published_ts");
			f.rss_link_id = rs.getInt("rss_link_id");

			feeds.add(f);
		}

		w.stop();
		System.out.println("get feeds " + count + "; " + w.elipseMs() + "ms");

		con.close();
		return feeds;
	}

	public List<FeedTag> getFeedTags() throws SQLException {
		Connection con = getSrcConnection();
		StopWatch w = new StopWatch().start();

		int count = getSize(con, "feed_tag");
		List<FeedTag> feedTags = new ArrayList<FeedTag>(count);

		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from feed_tag");
		while (rs.next()) {
			FeedTag ft = new FeedTag();
			ft.tag = rs.getString("tag");
			ft.feed_id = rs.getInt("feed_id");
			ft.user_id = rs.getInt("user_id");
			feedTags.add(ft);
		}
		w.stop();
		System.out
				.println("get feedtag " + w.elipseMs() + "ms, total " + count);

		con.close();
		return feedTags;

	}

	public List<RssLink> getRssLinks() throws SQLException {
		Connection con = getSrcConnection();
		StopWatch w = new StopWatch().start();

		int count = getSize(con, "rss_links");
		List<RssLink> links = new ArrayList<RssLink>(count);

		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select * from rss_links");
		while (rs.next()) {
			RssLink link = new RssLink();
			link.id = rs.getInt("id");
			link.url = rs.getString("url");
			link.title = rs.getString("title");
			link.description = rs.getString("description");
			link.alternate = rs.getString("alternate");
			link.added_ts = rs.getTimestamp("added_ts");
			link.next_check_ts = rs.getInt("next_check_ts");
			link.check_interval = rs.getInt("check_interval");
			link.last_modified = rs.getString("last_modified");
			link.subscription_count = rs.getInt("subscription_count");
			link.user_id = rs.getInt("user_id");
			link.crawler_link_id = rs.getInt("crawler_link_id");

			links.add(link);
		}

		w.stop();
		System.out.println("get rsslinks " + w.elipseMs() + "ms, total "
				+ count);
		con.close();
		return links;
	}

	private int getSize(Connection con, String table) throws SQLException {
		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select count(*) as c from " + table);
		int count = 10000;
		if (rs.next()) {
			count = rs.getInt(1);
		}
		rs.close();
		stat.close();
		return count;
	}

	public Connection getSrcConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:" + SRC, "sa", "");
	}

	private Map<Integer, Integer> inserFeeds(Connection con,
			Map<Integer, Integer> rssLinksMap) throws SQLException, IOException {
		Map<Integer, Integer> feedMap = new TreeMap<Integer, Integer>();
		PreparedStatement ps = con.prepareStatement("insert into feeds "
				+ "(author, link, title, summary, snippet, "
				+ "updated_ts, published_ts, rss_link_id ) "
				+ "values (?, ?, ?, ?, ?, ?, ?, ?)");
		List<Feed> feeds = getFeeds();

		StopWatch w = new StopWatch().start();
		for (Feed f : feeds) {
			ps.clearParameters();
			ps.setString(1, f.author);
			ps.setString(2, f.link);
			ps.setString(3, f.title);
			if (f.summary != null) {
				ps.setCharacterStream(4, new StringReader(f.summary));
			} else {
				ps.setCharacterStream(4, null);
			}
			ps.setString(5, f.snippet);
			if (f.updated_ts != null) {
				ps.setInt(6, Integer.valueOf(f.updated_ts));
			} else {
				ps.setObject(6, null);

			}
			if (f.published_ts != null) {
				ps.setInt(7, Integer.valueOf(f.published_ts));
			} else {
				ps.setObject(7, null);
			}
			if (rssLinksMap.get(f.rss_link_id) != null) {
				ps.setInt(8, rssLinksMap.get(f.rss_link_id));
			} else {
				ps.setObject(8, null);
			}
			ps.execute();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				feedMap.put(f.id, rs.getInt(1));
			}
		}

		w.stop();
		System.out.printf("insert feeds %dms, av %.2fus, feeds %d\n",
				w.elipseMs(), w.avg(feeds.size()), feeds.size());

		return feedMap;
	}

	private Map<Integer, Integer> insertCrawlerLinks(Connection con)
			throws SQLException {
		List<CrawlerLink> cralwerLinks = getCrawlerLinks();
		Map<Integer, Integer> crawlerMap = new TreeMap<Integer, Integer>();
		PreparedStatement ps = con
				.prepareStatement("insert into crawler_links "
						+ "(url, domain, added_ts, title, "
						+ "next_check_ts, last_modified, "
						+ "check_interval, referer_id) values (?,?,?,?,?,?,?,?)");

		int i = 0;
		StopWatch w = new StopWatch().start();
		for (CrawlerLink c : cralwerLinks) {

			if (i++ % 70000 == 0)
				logger.info("crawler links, {}/{}, {}", new Object[] { i,
						cralwerLinks.size(), (float) i / cralwerLinks.size() });

			ps.clearParameters();
			ps.setString(1, c.url);
			ps.setString(2, c.domain);
			ps.setTimestamp(3, c.added_ts);
			ps.setString(4, c.title);
			ps.setInt(5, c.next_check_ts);
			ps.setString(6, c.last_modofied);
			ps.setInt(7, c.check_interval);
			if (c.referer_id != 0) {
				ps.setInt(8, crawlerMap.get(c.referer_id));
			} else {
				ps.setObject(8, null);
			}
			ps.execute();
			// crawler.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				crawlerMap.put(c.id, rs.getInt(1));
			}
		}

		w.stop();
		System.out.printf("insert cralwerLinks %dms, av %.2fus, links %d\n",
				w.elipseMs(), w.avg(cralwerLinks.size()), cralwerLinks.size());

		return crawlerMap;
	}

	public void insertData() throws SQLException, IOException {
		String path = "jdbc:h2:" + DEST + OPTIONS;
		Connection con = DriverManager.getConnection(path, "sa", "");

		Map<Integer, Integer> userMap = insertUsers(con);
		Map<Integer, Integer> crawlerMap = insertCrawlerLinks(con);
		Map<Integer, Integer> rssLinksMap = insertRssLinks(con, crawlerMap);
		Map<Integer, Integer> feedMap = inserFeeds(con, rssLinksMap);
		insertFeedTag(con, feedMap, userMap);
		insertUserSubscriptions(con, userMap, rssLinksMap);
		insertUserFeeds(con, userMap, feedMap);

		con.close();
	}

	private void insertUserFeeds(Connection con, Map<Integer, Integer> userMap,
			Map<Integer, Integer> feedMap) throws SQLException {
		List<UserFeed> userFeeds = getUserFeed();
		PreparedStatement ps = con
				.prepareStatement("insert into user_feed (feed_id, user_id, read, pref, read_date) values (?, ?, ?, ?, ?)");
		for (UserFeed uf : userFeeds) {
			ps.clearParameters();
			ps.setInt(1, feedMap.get(uf.feed_id));
			ps.setInt(2, feedMap.get(uf.user_id));
			ps.setBoolean(3, uf.read);
			if (uf.pref != null) {
				ps.setBoolean(4, Boolean.valueOf(uf.pref));
			} else {
				ps.setObject(4, null);
			}
			ps.setInt(5, uf.read_date);
			ps.execute();
		}
	}

	private void insertUserSubscriptions(Connection con,
			Map<Integer, Integer> userMap, Map<Integer, Integer> linksMap)
			throws SQLException {
		List<userSubscription> subscriptions = getUserSubscriptions();
		PreparedStatement ps = con
				.prepareStatement("insert into user_subscription "
						+ "(user_id, rss_link_id, title, added_ts) values (?, ?, ?, ?)");
		for (userSubscription us : subscriptions) {
			ps.clearParameters();
			ps.setInt(1, userMap.get(us.user_id));
			ps.setInt(2, linksMap.get(us.rss_link_id));
			ps.setString(3, us.title);
			ps.setTimestamp(4, us.added_ts);
			ps.execute();
		}

		logger.info("insert {} user_subscriptions ", subscriptions.size());

	}

	private Map<Integer, Integer> insertUsers(Connection con)
			throws SQLException {

		Map<Integer, Integer> userMap = new TreeMap<Integer, Integer>();

		List<User> users = getUsers();
		PreparedStatement ps = con
				.prepareStatement("insert into users (email, name, password, authen_token) values (?, ?, ?, ?)");
		for (User user : users) {
			ps.clearParameters();
			ps.setString(1, user.email);
			ps.setString(2, user.name);
			ps.setString(3, user.password);
			ps.setString(4, user.authen_token);
			ps.execute();

			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				userMap.put(user.id, rs.getInt(1));
			}
		}

		logger.info("insert {} users", users.size());

		return userMap;

	}

	private void insertFeedTag(Connection con, Map<Integer, Integer> feedMap,
			Map<Integer, Integer> userMap) throws SQLException {

		List<FeedTag> tags = getFeedTags();
		PreparedStatement ps = con
				.prepareStatement("insert into feed_tag (tag, user_id, feed_id) values (?, ?, ?)");
		StopWatch w = new StopWatch().start();
		for (FeedTag ft : tags) {
			ps.clearParameters();
			ps.setString(1, ft.tag);
			if (ft.user_id != 0) {
				ps.setInt(2, userMap.get(ft.user_id));
			} else {
				ps.setObject(2, null);
			}

			if (feedMap.get(ft.feed_id) != null) {
				ps.setInt(3, feedMap.get(ft.feed_id));
				ps.execute();
			} else {
				logger.warn("no mapping feed_id for {}", ft.feed_id);
			}
		}
		w.stop();
		System.out.printf("insert feedtags %dms, av %.2fus, tag %d\n",
				w.elipseMs(), w.avg(tags.size()), tags.size());

	}

	private Map<Integer, Integer> insertRssLinks(Connection con,
			Map<Integer, Integer> crawerMap) throws SQLException {
		Map<Integer, Integer> rssLinksMap = new TreeMap<Integer, Integer>();

		List<RssLink> links = getRssLinks();
		PreparedStatement ps = con.prepareStatement("insert into rss_links "
				+ "(url, title, description, alternate, "
				+ "added_ts, next_check_ts, check_interval, "
				+ "last_modified, subscription_count, user_id, "
				+ "crawler_link_id) values (?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)");
		int i = 0;
		StopWatch w = new StopWatch().start();
		for (RssLink r : links) {

			if (i++ % 30000 == 0) {
				logger.info(
						"rsslinks, {}/{}, {}",
						new Object[] { i, links.size(),
								(float) i / links.size() });
			}

			ps.clearParameters();
			ps.setString(1, r.url);
			ps.setString(2, r.title);
			ps.setString(3, r.description);
			ps.setString(4, r.alternate);
			ps.setTimestamp(5, r.added_ts);
			ps.setInt(6, r.next_check_ts);
			ps.setInt(7, r.check_interval);
			ps.setString(8, r.last_modified);
			ps.setInt(9, r.subscription_count);
			ps.setObject(10, null);// user-id
			if (crawerMap.get(r.crawler_link_id) != null) {
				ps.setInt(11, crawerMap.get(r.crawler_link_id));
			} else {
				ps.setObject(11, null);
			}
			ps.execute();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				rssLinksMap.put(r.id, rs.getInt(1));
			}
		}
		w.stop();
		System.out.printf("insert rsslinks %dms, av %.2fus, links %d\n",
				w.elipseMs(), w.avg(links.size()), links.size());

		return rssLinksMap;
	}
}

class RssLink {
	int id;
	String url;
	String title;
	String description;
	String alternate;
	Timestamp added_ts;
	int next_check_ts;
	int check_interval;
	String last_modified;
	int subscription_count;
	int user_id;
	int crawler_link_id;
}

class StopWatch {
	private long start;
	private long stop;

	public float avg(int count) {
		return (float) (stop - start) / count / 1000;
	}

	public long elipse() {
		return stop - start;
	}

	public long elipseMs() {
		return (stop - start) / 1000000;
	}

	public StopWatch start() {
		start = System.nanoTime();
		return this;
	}

	public long stop() {
		stop = System.nanoTime();
		return stop - start;
	}
}
