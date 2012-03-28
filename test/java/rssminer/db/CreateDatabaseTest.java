package rssminer.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

public class CreateDatabaseTest {

	@Test
	public void testCreateDatabasePerformance() throws SQLException {
		// about 3s
		for (int i = 0; i < 1000; ++i) {
			Connection con = DriverManager.getConnection(
					"jdbc:mysql://localhost/mysql", "root", "rssminer.net");
			Statement s = con.createStatement();
			s.execute("create database test_" + i);
			s.execute("drop database test_" + i);
			s.close();
			con.close();
		}
	}
}
