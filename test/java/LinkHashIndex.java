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

public class LinkHashIndex {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer?cachePrepStmts=true&useServerPrepStmts=true";
    static Logger logger = LoggerFactory.getLogger(LinkHashIndex.class);

    public static void main(String[] args) throws SQLException {
        Connection db = DriverManager.getConnection(JDBC_URL, "feng", "");
        Statement stat = db.createStatement();
        ResultSet rs = stat.executeQuery("select id, link from feeds");
        PreparedStatement ps = db
                .prepareStatement("update feeds set link_hash = ? where id = ?");
        int count = 0;
        while (rs.next()) {
            count++;
            String link = rs.getString(2);
            int id = rs.getInt(1);
            ps.setInt(1, link.hashCode());
            ps.setInt(2, id);
            ps.executeUpdate();

            if (count % 40000 == 0) {
                logger.info("dealed {}", count);
            }
        }
    }
}
