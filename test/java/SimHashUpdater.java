import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import me.shenfeng.dbcp.PerThreadDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.SimHash;
import rssminer.search.Searcher;
import clojure.lang.Keyword;

public class SimHashUpdater {

    private static final Logger logger = LoggerFactory
            .getLogger(SimHashUpdater.class);

    public static void main(String[] args) throws IOException, SQLException {
        Map<Keyword, Object> config = new HashMap<Keyword, Object>();
        PerThreadDataSource db = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer", "feng", "");
        config.put(rssminer.Utils.K_DATA_SOURCE, db);
        Searcher.initGlobalSearcher("/var/rssminer/index", config);

        Connection con = db.getConnection();
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select max(id) from feed_data");

        rs.next();

        int max = rs.getInt(1);

        PreparedStatement update = con
                .prepareStatement("update feeds set simhash = ? where id = ?");

        for (int i = 0; i <= max; i++) {
            long h2 = SimHash.simHash(i);
            if (h2 != -1) {
                update.setLong(1, h2);
                update.setInt(2, i);
                update.execute();
            }
            if (i % 5000 == 0) {
                logger.info("handle " + i);
            }
        }
    }
}
