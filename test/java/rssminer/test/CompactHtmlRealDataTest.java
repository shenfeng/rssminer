package rssminer.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

public class CompactHtmlRealDataTest {

    @Test
    public void test() throws SQLException {
        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat
                .executeQuery("select d.id, d.summary, link from feed_data d join feeds f on f.id = d.id where f.id = 140450");

        while (rs.next()) {
            String summary = rs.getString(2);
            int id = rs.getInt(1);
            String link = rs.getString(3);

            System.out.println(summary);

            System.out.println("----------------------\n");

            String string = HtmlUtils.compact(summary, link);
            System.out.println(string);
        }

    }

    @Test
    public void testCompactHtmlRatio() throws SQLException {

        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat
                .executeQuery("select d.id, d.summary, link from feed_data d join feeds f on f.id = d.id");
        // PreparedStatement ps = con
        // .prepareStatement("update feed_data set jsoup=?, tagsoup=?, compact=? where id = ?");

        PreparedStatement ps = con
                .prepareStatement("update feed_data set summary=? where id = ?");

        int orignalLength = 0;
        int compactLength = 0;
        int count = 0;
        while (rs.next()) {
            int id = rs.getInt(1);
            String html = rs.getString(2);
            if (html == null) {
                html = "";
            }
            String compact = HtmlUtils.compact(html, rs.getString(3));
            orignalLength += html.length();
            compactLength += compact.length();
            if (orignalLength > compactLength) {
                // System.out.println(id + "\t" + orignalLength + "\t" +
                // compactLength);
            }
            count++;
            try {
                ps.setString(1, compact);
                ps.setInt(2, id);
                ps.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("count: " + count);
        System.out.println("orignal: " + orignalLength);
        System.out.println("compact: " + compactLength);
        System.out.println(compactLength / (double) orignalLength);

    }
}
