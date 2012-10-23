/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.db;

import me.shenfeng.dbcp.PerThreadDataSource;

import java.sql.*;

public class ProcedureTest {

    public static void main(String[] args) throws SQLException {
        PerThreadDataSource ds = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer_test", "root", "");

        Connection con = ds.getConnection();

        CallableStatement call = con.prepareCall("call get_voted(1)");

        ResultSet rs = call.executeQuery();
        while (rs.next()) {
            System.out.println(rs.getInt(1));
        }
    }

}
