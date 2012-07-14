package rssminer.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Utils {

    static String JDBC_URL = "jdbc:mysql://localhost/rssminer";

    public static Connection getRssminerDB() throws SQLException {
        Connection con = DriverManager.getConnection(JDBC_URL, "feng", "");

        return con;
    }

}
