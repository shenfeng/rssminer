/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.tools;

import java.sql.*;
import java.util.*;

//delete some for refetch, to test the process

public class RandDelete {

    public static void main(String[] args) throws SQLException {
        Connection con = Utils.getRssminerDB();

        Statement stat = con.createStatement();
        ResultSet rs = stat.executeQuery("select feed_id from user_feed");

        Set<Integer> ids = new TreeSet<Integer>();
        while (rs.next()) {
            ids.add(rs.getInt(1));
        }
        rs.close();

        System.out.println(ids.size());

        List<Integer> toDelete = new ArrayList<Integer>();
        rs = stat
                .executeQuery("select id from feeds order by rand() limit 20000");
        while (rs.next()) {
            int id = rs.getInt(1);
            if (!ids.contains(id)) {
                toDelete.add(id);
            }
        }

        PreparedStatement ps = con
                .prepareStatement("delete from feeds where id = ?");

        for (Integer id : toDelete) {
            ps.setInt(1, id);
            ps.execute();
        }
    }
}
