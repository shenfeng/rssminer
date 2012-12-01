/*
 * Copyright (c) Feng Shen<shenedu@gmail.com>. All rights reserved.
 * You must not remove this notice, or any other, from this software.
 */

package rssminer.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rssminer.tools.Utils;

class Counter {

    private HashMap<Object, Integer> counter = new HashMap<Object, Integer>();

    public void add(Object key) {
        Integer i = counter.get(key);
        if (i == null) {
            i = 0;
        }
        i++;
        counter.put(key, i);
    }

    public String toString() {
        return counter.toString();
    }
}

public class CompressUrl {

    public static List<String> split(String link) {

        List<String> strs = new ArrayList<String>();

        StringBuilder sb = new StringBuilder();

        boolean is = false;
        for (int i = 0; i < link.length(); i++) {
            char ch = link.charAt(i);
            if (ch == '.' || Character.isSpaceChar(ch) || (i > 8 && ch == '/')) {
                strs.add(sb.toString());
                sb.setLength(0);
                sb.append(ch);
            } else {
                sb.append(ch);
            }
        }
        sb.append(sb.toString());
        return strs;

    }

    public static void main(String[] args) throws SQLException {

        Connection db = Utils.getRssminerDB();

        Statement stat = db.createStatement();
        ResultSet rs = stat.executeQuery("select id, link from feeds");

        PreparedStatement ps = db
                .prepareStatement("update feeds set link_hash = ? where id = ?");

        // Counter

        int totalLength = 0;
        int count = 0;
        while (rs.next()) {

            String link = rs.getString(2);
            int id = rs.getInt(1);

            ps.setInt(1, link.hashCode());
            ps.setInt(2, id);
            ps.executeUpdate();

        }

    }
}
