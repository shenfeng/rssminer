/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rssminer.jsoup.HtmlUtils;

public class Compact {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    final static int STEP = 1000;
    final static int REPORT = STEP * 10;

    private static Logger logger = LoggerFactory.getLogger(Compact.class);

    public static void main(String[] args) throws SQLException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        String sql = "select d.id, d.summary, link from feed_data d join feeds f on f.id = d.id where f.id > ? and f.id <= ?";
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select max(id) from feed_data");

        PreparedStatement select = con.prepareStatement(sql);
        PreparedStatement update = con
                .prepareStatement("update feed_data set summary = ? where id = ?");
        if (rs.next()) {
            int count = rs.getInt(1);
            for (int i = 0; i <= count + STEP;) {
                if (i % REPORT == 0) {
                    logger.info("deal {}, max {}", i, count);
                }
                select.setInt(1, i);
                select.setInt(2, i + STEP);
                rs = select.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String summary = rs.getString(2);
                    String link = rs.getString(3);
                    String c = HtmlUtils.compact(summary, link);
                    if (!c.equals(summary)) {
                        update.setString(1, c);
                        update.setInt(2, id);
                        update.executeUpdate();
                    }
                }
                rs.close();
                i += STEP;
            }
        }
    }
}
