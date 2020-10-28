package jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author timo
 * Usage esample:
 * 
 *  	DbConnector db = DbConnector.getInstance();
		db.setDbName("test5");
		db.execute("CREATE TABLE IF NOT EXISTS test1(id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, name varchar(100));");
		System.out.println("auto id: " + db.executeInsert("INSERT INTO test1(name) VALUES('1'), ('2')"));
		System.out.println(db.executeQuery("SELECT * FROM test1"));
 */

public class DbConnector {
	
	static final String TIME_ZONE = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	static final String JSBC_URL = "jdbc:mysql://";
	String dbUrl = "localhost";
	String dbUser = "root";
	String dbPassowrd = "";
	String dbName = "test";
	int dbPort = 3306;
	
	private Connection conn = null;
	
	private static DbConnector instance = new DbConnector();
	
	private DbConnector() {
		super();
	}
	
	public static DbConnector getInstance() {
		return instance;
	}
	
	boolean openConnection() {
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", dbUser);
	    connectionProps.put("password", dbPassowrd);
		try {
			conn =  DriverManager.getConnection("jdbc:mysql://" + dbUrl + ":" + dbPort + "/" + TIME_ZONE, connectionProps);
			System.out.println("connected to database");
			boolean dbExists = false;
			try {
				ResultSet resultSet = conn.getMetaData().getCatalogs();
				while (resultSet.next()) {
					String name = resultSet.getString(1);
					if(name.contentEquals(dbName.toLowerCase())) {
						dbExists = true;
						break;
				  	}
				}
				resultSet.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
			Statement stmt = conn.createStatement();
			if(!dbExists) {
				try {
					System.out.println("Creating DATABASE: \"" + dbName + "\"");
					stmt.execute("CREATE DATABASE " + dbName + ";");
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
			}
			stmt.execute("USE " + dbName + ";");
			stmt.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("could not connected to database!");
			return false;
		}
	}
	
	boolean closeConnection() {
		if(conn != null) {
			try {
				conn.close();
				return true;
			} catch (SQLException e) {
				return false;
			}
		}
		return true;
	}
	
	public int executeInsert(String sql) {
		System.out.println("executeInsert: " + sql);
		if(openConnection()) {
			Statement stmt = null;
			try {
				stmt = conn.createStatement();
				int id = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
				stmt.close();
				return id;
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection();
			}
		}
		return -1;
	}
	
	public List<Map<String, Object>> executeQuery(String sql){
		System.out.println("executeQuery: " + sql);
		List<Map<String, Object>> rsList = new ArrayList<Map<String, Object>>();
		if(openConnection()) {
			try {
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {
					Map<String, Object> rsMap = new HashMap<String, Object>();
					for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {
						rsMap.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
					}
					rsList.add(rsMap);
				}
				rs.close();
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}finally {
				closeConnection();
			}
		} else {
			System.out.println("No Open Connection!");
		}
		return rsList;
	}
	
	public boolean execute(String sql) {
		System.out.println("execute: " + sql);
		if(openConnection()) {
			try {
				Statement stmt = conn.createStatement();
				stmt.execute(sql);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeConnection();
			}
			return true;
		}
		return false;
	}


	public boolean doesTableExist(String tableName) {
		if(openConnection()) {
			try {
				DatabaseMetaData dbm = conn.getMetaData();
				ResultSet rs = dbm.getTables(null, null,  tableName.toLowerCase(), null);
				boolean success = false;
			    if (rs.next()) {
//			    	for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {
//			    		System.out.println(rs.getMetaData().getColumnName(i) + ": " + rs.getString(i));
//					}
			    	if(rs.getString("TABLE_CAT").contentEquals(dbName.toLowerCase())) {
			    		success = true;
			    	}
			    }
			    rs.close();
			    return success;
			} catch (SQLException e) {
				e.printStackTrace();
			}finally {
				closeConnection();
			}
			
		}
		return false;
	}
	
	/*
	 * Getters /  Setters
	 */
	
	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassowrd() {
		return dbPassowrd;
	}

	public void setDbPassowrd(String dbPassowrd) {
		this.dbPassowrd = dbPassowrd;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public int getDbPort() {
		return dbPort;
	}

	public void setDbPort(int dbPort) {
		this.dbPort = dbPort;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
}