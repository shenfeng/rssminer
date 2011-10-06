package rssminer;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import me.shenfeng.dns.DnsClient;
import me.shenfeng.dns.DnsClientConfig;
import me.shenfeng.dns.DnsPrefecher;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarmDnsCache {
    static Logger logger = LoggerFactory.getLogger(WarmDnsCache.class);
    final static int CHECK_COUNT = 1000;
    final static String ALL_URL = "select url from crawler_links";
    final static String FRONTIER_URL = "select url from crawler_links ORDER BY next_check_ts limit 1000";

    public static List<String> getAllHosts(String sql) throws SQLException {
        JdbcConnectionPool p = JdbcConnectionPool
                .create("jdbc:h2:/media/1082B19F82B189AC/rssminer/rssminer;AUTO_SERVER=TRUE",
                        "sa", "sa");
        Connection con = p.getConnection();
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery(sql);
        final List<String> hosts = new ArrayList<String>(1024 * 1024 * 2); // 2m
        while (rs.next()) {
            try {
                final String host = new URI(rs.getString("url")).getHost();
                hosts.add(host);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        rs.close();
        stat.close();
        con.close();
        p.dispose();
        return hosts;
    }

    @Test
    public void testAsyncCorrectness() throws SQLException {
        DnsClientConfig cfg = new DnsClientConfig(6000, 3000);

        DnsClient client = new DnsClient(cfg);
        List<String> allHosts = getAllHosts(ALL_URL);
        Random r = new Random();
        int begin = r.nextInt(allHosts.size() - CHECK_COUNT);
        List<String> hosts = new ArrayList<String>();
        for (int i = 0; i < CHECK_COUNT + begin; i++) {
            if (i > begin)
                hosts.add(allHosts.get(i));
        }

        for (String host : hosts) {
            try {
                InetAddress[] byName = InetAddress.getAllByName(host);
                boolean match = false;
                String ip = client.resolve(host).get();
                for (InetAddress add : byName) {
                    if (add.getHostAddress().equals(ip)) {
                        logger.info("{}, {} ok ", host, ip);
                        match = true;
                        break;
                    }
                }
                String msg = Arrays.deepToString(byName) + "not mach " + ip
                        + " for " + host;
                if (!match)
                    logger.error(msg);
                Assert.assertTrue(msg, match);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void warmAllCache() throws SQLException, InterruptedException {
        final List<String> hosts = getAllHosts(ALL_URL);
        DnsPrefecher prefecher = DnsPrefecher.getInstance();
        int i = 0;
        for (String host : hosts) {
            ++i;
            if (i % 40 == 0) {
                try {
                    InetAddress.getAllByName(host); // slow things down
                } catch (UnknownHostException e) {
                    logger.error(host + " has no ip");
                }
            } else {
                prefecher.prefetch(host);
            }
        }
        logger.info("this run ok");
    }

    @Test
    public void warmCache() throws SQLException, InterruptedException {
        final List<String> hosts = getAllHosts(FRONTIER_URL);
        DnsPrefecher prefecher = DnsPrefecher.getInstance();
        int i = 0;
        for (String host : hosts) {
            ++i;
            if (i % 40 == 0) {
                try {
                    InetAddress.getAllByName(host); // slow things down
                } catch (UnknownHostException e) {
                    logger.error(host + " has no ip");
                }
            } else {
                prefecher.prefetch(host);
            }
        }
        logger.info("this run ok");
    }
}
