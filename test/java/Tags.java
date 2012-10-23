/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class Tags {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    final static int STEP = 1000;
    final static int REPORT = STEP * 10;

    private static Logger logger = LoggerFactory.getLogger(Tags.class);

    private static final String join(String[] tags) {
        StringBuilder sb = new StringBuilder();
        for (String t : tags) {
            if (!t.isEmpty()) {
                if (t.length() > 1 || t.charAt(0) > 256)
                    sb.append(t).append(";");
            }
        }
        if (sb.length() > 0)
            sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static void main(String[] args) throws SQLException {

        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");
        String sql = "select id, tags from feeds where id > ? and id <= ?";
        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select max(id) from feed_data");

        PreparedStatement select = con.prepareStatement(sql);
        PreparedStatement update = con
                .prepareStatement("update feeds set tags = ? where id = ?");
        if (rs.next()) {
            int count = rs.getInt(1);
            for (int i = 0; i <= count + STEP; ) {
                if (i % REPORT == 0) {
                    logger.info("deal {}, max {}", i, count);
                }
                select.setInt(1, i);
                select.setInt(2, i + STEP);
                rs = select.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt(1);
                    String tag = rs.getString(2);
                    if (tag != null && !tag.isEmpty()) {
                        String[] tags = tag.split(";\\s?");
                        if (tags.length > 0) {
                            String n = join(tags);
                            if (!n.equals(tags)) {
                                update.setString(1, n);
                                update.setInt(2, id);
                                update.executeUpdate();
                            }
                        }
                    }
                }
                rs.close();
                i += STEP;
            }
        }
    }
}
