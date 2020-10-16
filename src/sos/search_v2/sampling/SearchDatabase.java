package sos.search_v2.sampling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import sos.search_v2.tools.RegionAttribute;
/**
 * 
 * @author Salim
 *
 */
public class SearchDatabase {
	private Connection connection;

	public SearchDatabase(String url, String uname, String pass) throws ClassNotFoundException, SQLException {
		@SuppressWarnings("unused")
		String dbClass = "com.mysql.jdbc.Driver";
		Class.forName("com.mysql.jdbc.Driver");
		connection = DriverManager.getConnection(url, uname, pass);
	}

	public void addRegionInfo(ArrayList<RegionAttribute> atts) throws SQLException {
		Statement stmt = connection.createStatement();
		String query = "INSERT INTO region_samples(";
		for (int i = 0; i < atts.size(); i++) {
			if (i > 0)
				query += ",";
			query += "" + atts.get(i).getAttributeName();
		}
		query += ") VALUES(";
		for (int i = 0; i < atts.size(); i++) {
			if (i > 0)
				query += ",";
			query += "" + atts.get(i).getValue();
		}
		query += ")";
		stmt.execute(query);
	}

	public static void main(String[] args) {

	}

	
}
