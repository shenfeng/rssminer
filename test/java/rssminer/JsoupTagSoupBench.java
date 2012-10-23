/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer;

import org.junit.BeforeClass;
import org.junit.Test;
import rssminer.jsoup.HtmlUtils;
import rssminer.tools.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JsoupTagSoupBench {
    static List<String> summarys = new ArrayList<String>();

    @BeforeClass
    public static void setup() throws SQLException {
        Connection con = Utils.getRssminerDB();
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select summary from feed_data");
        while (rs.next()) {
            String str = rs.getString(1);
            if (str != null && !str.isEmpty()) {
                summarys.add(str);
            }
        }
    }

    @Test
    public void testJsoup() {
        int ok = 0;
        int totalLength = 0;
        for (String summary : summarys) {
            try {
                String r = HtmlUtils.summaryText(summary);
                totalLength += r.length();
                ok++;
            } catch (Exception e) {
            }
        }
        System.out.println("jsoup: " + ok + "\t" + totalLength);
    }
}
