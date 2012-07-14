package rssminer.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
