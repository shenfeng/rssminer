package rssminer.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;
import me.shenfeng.dbcp.PerThreadDataSource;

import org.apache.lucene.index.IndexReader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clojure.lang.Keyword;
import rssminer.SimHash;
import rssminer.search.Searcher;

public class SimHashTest {
    @Test
    public void testHammingDistance() {
        int diff = 1;
        String strx = "0";
        String stry = "1";
        for (int i = 0; i < 63; i++) {
            long x = Long.parseLong(strx, 2);
            long y = Long.parseLong(stry, 2);
            Assert.assertSame(SimHash.hammingDistance(x, y), diff);
            double r = Math.random();
            if (r > 0.7) {
                diff += 1;
                strx += "1";
                stry += "0";
            } else if (r > 0.4) {
                diff += 1;
                strx += "0";
                stry += "1";
            } else if (r > 0.2) {
                strx += "1";
                stry += "1";
            } else {
                strx += "0";
                stry += "0";
            }
        }
    }

    private int simhash(String[] terms) {
        int[] bits = new int[32];
        for (String term : terms) {
            int code = term.hashCode();
            for (int i = 0; i < bits.length; i++) {
                if (((code >>> i) & 0x1) == 0x1) {
                    ++bits[i];
                } else {
                    --bits[i];
                }
            }
        }
        int fingerprint = 0;
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] > 0) {
                fingerprint += (1 << i);
            }
        }
        return fingerprint;
    }

    private static final Logger logger = LoggerFactory
            .getLogger(SimHashTest.class);

    public static void main(String[] args) throws IOException, SQLException {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        Searcher.initGlobalSearcher("/var/rssminer/index", config);

        Connection con = db.getConnection();

        PreparedStatement ps = con
                .prepareStatement("select summary from feed_data where id = ?");

        PreparedStatement update = con
                .prepareStatement("update feeds set simhash = ? where id = ?");

        for (int i = 204565; i < 214565; i++) {
            ps.setInt(1, i);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String summary = rs.getString(1);
                // long h1 = SimHash.simHash(summary);
                long h2 = SimHash.simHash(i);
                if (h2 != -1) {
                    update.setLong(1, h2);
                    update.setInt(1, i);
                    update.execute();
                }
                // if (h1 != h2) {
                // System.out
                // .println("not equal " + i + "\t" + h1 + "\t" + h2);
                // }
            }
            if (i % 5000 == 0) {
                logger.info("handle " + i);
            }
            rs.close();
        }
    }

    @Test
    public void testSimHash() {
        int h1 = simhash("the cat sat on a mat".split(" "));
        int h2 = simhash("the cat sat on the mat".split(" "));
        int h3 = simhash("we all scream for ice cream".split(" "));
        System.out.println(SimHash.hammingDistance(h1, h2));
        System.out.println(SimHash.hammingDistance(h1, h3));
        System.out.println(SimHash.hammingDistance(h2, h3));
    }
}
