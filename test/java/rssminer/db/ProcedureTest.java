package rssminer.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import me.shenfeng.dbcp.PerThreadDataSource;

public class ProcedureTest {

    public static void main(String[] args) throws SQLException {
        PerThreadDataSource ds = new PerThreadDataSource(
                "jdbc:mysql://localhost/rssminer_test", "root", "");
        
        Connection con = ds.getConnection();
        
        CallableStatement call = con.prepareCall("call get_voted(1)");
        
        ResultSet rs = call.executeQuery();
        while(rs.next()) {
            System.out.println(rs.getInt(1));
        }
    }

}
