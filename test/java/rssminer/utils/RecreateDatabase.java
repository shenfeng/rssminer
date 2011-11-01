package rssminer.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

public class RecreateDatabase {

	final static String PATH = "/dev/shm/rssminer";
	final static String SRC = "/media/disk/rssminer/rssminer";

	public void createDatabase() throws IOException, SQLException {
		InputStream is = RecreateDatabase.class.getClassLoader()
				.getResourceAsStream("rssminer.sql");
		String sql = IOUtils.toString(is);
		Connection con = DriverManager.getConnection("jdbc:h2:" + PATH, "sa",
				"");
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

	public List<CrawlerLink> getCrawlerLinks(Connection con)
			throws SQLException {

		Statement stat = con.createStatement();
		ResultSet rs = stat
				.executeQuery("select count(*) as c from crawler_links");
		int count = 10000;
		if (rs.next()) {
			count = rs.getInt(1);
		}
		rs.close();
		stat.close();

		stat = con.createStatement();
		rs = stat.executeQuery("select * from crawler_links");
		List<CrawlerLink> links = new ArrayList<CrawlerLink>(count);
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

		return links;

	}

	public List<RssLink> getRssLinks(Connection con) throws SQLException {

		Statement stat = con.createStatement();
		ResultSet rs = stat.executeQuery("select count(*) as c from rss_links");
		int count = 10000;
		if (rs.next()) {
			count = rs.getInt(1);
		}
		rs.close();
		stat.close();

		stat = con.createStatement();
		rs = stat.executeQuery("select * from rss_links");
		List<RssLink> links = new ArrayList<RssLink>(count);
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

		return links;

	}

	public void insertData(List<CrawlerLink> cralwerLinks,
			List<RssLink> rsslinks) throws SQLException {

		Connection con = DriverManager.getConnection("jdbc:h2:" + PATH, "sa",
				"");

		PreparedStatement crawler = con
				.prepareStatement("insert into crawler_links "
						+ "(url, domain, added_ts, title, "
						+ "next_check_ts, last_modified, "
						+ "check_interval, referer_id) values (?,?,?,?,?,?,?,?)");
		PreparedStatement rss = con.prepareStatement("insert into rss_links "
				+ "(url, title, description, alternate, "
				+ "added_ts, next_check_ts, check_interval, "
				+ "last_modified, subscription_count, user_id, "
				+ "crawler_link_id) values (?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)");

		Map<Integer, Integer> map = new TreeMap<Integer, Integer>();

		long start = System.nanoTime();
		for (CrawlerLink c : cralwerLinks) {
			crawler.clearParameters();
			crawler.setString(1, c.url);
			crawler.setString(2, c.domain);
			crawler.setTimestamp(3, c.added_ts);
			crawler.setString(4, c.title);
			crawler.setInt(5, c.next_check_ts);
			crawler.setString(6, c.last_modofied);
			crawler.setInt(7, c.check_interval);
			if (c.referer_id != 0) {
				crawler.setInt(8, map.get(c.referer_id));
			} else {
				crawler.setObject(8, null);
			}
			crawler.execute();
			// crawler.executeUpdate();
			ResultSet rs = crawler.getGeneratedKeys();
			if (rs.next()) {
				map.put(c.id, rs.getInt(1));
			}

		}
		long time = System.nanoTime() - start;
		float av = (float) time / cralwerLinks.size();
		System.out.printf("%.2fms, %dms, inserted crawler links: %d\n", av,
				time / 1000, cralwerLinks.size());

		start = System.nanoTime();
		for (RssLink r : rsslinks) {
			rss.clearParameters();
			rss.setString(1, r.url);
			rss.setString(2, r.title);
			rss.setString(3, r.description);
			rss.setString(4, r.alternate);
			rss.setTimestamp(5, r.added_ts);
			rss.setInt(6, r.next_check_ts);
			rss.setInt(7, r.check_interval);
			rss.setString(8, r.last_modified);
			rss.setInt(9, r.subscription_count);
			rss.setObject(10, null);// user-id
			if (map.get(r.crawler_link_id) != null) {
				rss.setInt(11, map.get(r.crawler_link_id));
			} else {
				rss.setObject(11, null);
			}
			rss.execute();
		}
		time = System.nanoTime() - start;
		av = (float) time / rsslinks.size();
		System.out.printf("%.2fms, %dms, inserted rss links: %d\n", av,
				time / 1000, rsslinks.size());

		con.close();
	}

	public static void main(String[] args) {
		try {
			long start = System.currentTimeMillis();
			File f = new File(PATH).getParentFile();

			for (File file : f.listFiles()) {
				if (file.getName().startsWith("rssminer.")) {
					System.out.printf("delete %s, %s\n", file.getName(),
							file.delete());
				}
			}

			Connection con = DriverManager.getConnection("jdbc:h2:" + SRC,
					"sa", "");
			RecreateDatabase r = new RecreateDatabase();
			r.createDatabase();

			List<CrawlerLink> crawlerLinks = r.getCrawlerLinks(con);
			List<RssLink> rssLinks = r.getRssLinks(con);
			r.insertData(crawlerLinks, rssLinks);
			con.close();

			System.out.println(System.currentTimeMillis() - start);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
