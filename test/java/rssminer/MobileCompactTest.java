package rssminer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

public class MobileCompactTest {

    @Test
    public void testClean() throws SQLException {

        Connection con = Utils.getRssminerDB();

        Statement stat = con.createStatement();

        ResultSet rs = stat.executeQuery("select * from feed_data where id = 1263050");
        rs.next();

        String html = rs.getString("summary");

        String web = HtmlUtils.cleanForMobile(html, "http://google.com");
        
        System.out.println(html);
        
        System.out.println("\n\n");
        
        System.out.println(web);

    }
}
