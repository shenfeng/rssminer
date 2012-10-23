/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.test;

import org.junit.Test;
import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

import java.sql.*;

public class HtmlTextBench {

    @Test
    public void testText2() throws SQLException {
        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat
                .executeQuery("select d.id, d.summary from feed_data d join feeds f on f.id = d.id");

        while (rs.next()) {
            int id = rs.getInt(1);
            String html = rs.getString(2);
            HtmlUtils.text(html);
        }
    }

    @Test
    public void testTextSummary() throws SQLException {
        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat
                .executeQuery("select d.id, d.summary from feed_data d join feeds f on f.id = d.id");

        while (rs.next()) {
            int id = rs.getInt(1);
            String html = rs.getString(2);
            String text = HtmlUtils.summaryText(html);
        }
    }
}
