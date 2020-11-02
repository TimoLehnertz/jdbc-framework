package jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class for interactions with the database
 * 
 * @author Timo Lehnertz
 *
 */

public class DbConnector {
	
	static final String DB_ARGS = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	static final String JSBC_URL = "jdbc:mysql://";
	String dbUrl = "localhost";
	String dbUser = "root";
	String dbPassowrd = "";
	String dbName = "test";
	int dbPort = 3306;
	
	private Connection conn = null;
	
	private boolean debug = false;
	
	private static DbConnector instance = new DbConnector();
	
	private DbConnector() {
		super();
	}
	
	protected static DbConnector getInstance() {
		return instance;
	}
	
	public boolean isOperatable() {
		Connection conn = getConnection();
		boolean operatable = conn != null;
		try {
			conn.close();
		} catch (Exception e) {
			operatable = false;
		}
		return operatable;
	}
	
	private boolean openConnection() {
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", dbUser);
	    connectionProps.put("password", dbPassowrd);
		try {
			conn =  DriverManager.getConnection("jdbc:mysql://" + dbUrl + ":" + dbPort + DB_ARGS, connectionProps);
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
				System.err.println(e.getMessage());
				return false;
			}
			Statement stmt = conn.createStatement();
			if(!dbExists) {
				try {
					if(debug) {
						System.out.println("Creating DATABASE: \"" + dbName + "\"");
					}
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
			System.err.println(e.getMessage());
			return false;
		}
	} 
	
	private Connection getConnection() {
		if(openConnection()) {
			return conn;
		} else {
			return null;
		}
	}
	
	protected long executeInsert(String sql, String argTypes, Object... objects) {
		return executeInsert(new SqlParams(argTypes, sql, objects));
	}
	
	protected long executeInsert(SqlParams params) {
		if(debug) {
			System.out.println("executingInsert: " + params.sql);
		}
		if(!isOperatable()) {
			return -1;
		}
		try(Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(params.sql, Statement.RETURN_GENERATED_KEYS)){
				params.bindParams(stmt);
				stmt.execute();
				ResultSet rs = stmt.getGeneratedKeys();
				long id = -1;
				if(rs.next()) {
					id = rs.getLong(1);
				}
				rs.close();
				return id;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			if(debug) {
				System.out.println("Done");
			}
		}
		return -1;
	}
	
	protected List<Map<String, Object>> executeQuery(String sql, String argTypes, Object... objects) {
		return executeQuery(new SqlParams(argTypes, sql, objects));
	}
	
	protected List<Map<String, Object>> executeQuery(SqlParams params){
		if(!isOperatable()) {
			return new ArrayList<Map<String, Object>>();
		}
		if(debug) {
			System.out.println("executeQuery: " + params.sql);
		}
		List<Map<String, Object>> rsList = new ArrayList<Map<String, Object>>();
		try(Connection conn = getConnection()) {
			try(PreparedStatement stmt = conn.prepareStatement(params.sql)){
				params.bindParams(stmt);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					Map<String, Object> rsMap = new HashMap<String, Object>();
					for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {
						rsMap.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
					}
					rsList.add(rsMap);
				}
				rs.close();
			}catch(SQLException e) {
				e.printStackTrace();
			}
		}catch(SQLException e) {
			e.printStackTrace();
		} finally {
			if(debug) {
				System.out.println("Done");
			}
		}
		return rsList;
	}
	
	protected boolean execute(String sql) {
		return execute(sql, null);
	}
	
	protected boolean execute(String sql, String argTypes, Object... objects) {
		return execute(new SqlParams(argTypes, sql, objects));
	}
	
	protected boolean execute(SqlParams params) {
		if(params == null || !isOperatable()) {
			return false;
		}
		if(debug) {
			System.out.println("execute: " + params.sql);
		}
		try(Connection conn = getConnection()) {
			try(PreparedStatement stmt = conn.prepareStatement(params.sql)) {
				params.bindParams(stmt);
				stmt.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		} catch (SQLException e1) {
			e1.printStackTrace();
		} finally {
			if(debug) {
				System.out.println("Done!");
			}
		}
		return false;
	}


	protected boolean doesTableExist(String tableName) {
		try (Connection conn = getConnection()){
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getTables(getDbName(), null,  tableName.toLowerCase(), null);
		    if (rs.next()) {
	    		rs.close();
	    		return true;
		    }
		    rs.close();
		    return false;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * Getters /  Setters
	 */
	
	protected String getDbUser() {
		return dbUser;
	}

	protected void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	protected String getDbPassowrd() {
		return dbPassowrd;
	}

	protected void setDbPassowrd(String dbPassowrd) {
		this.dbPassowrd = dbPassowrd;
	}

	protected String getDbName() {
		return dbName;
	}

	protected void setDbName(String dbName) {
		this.dbName = dbName;
	}

	protected int getDbPort() {
		return dbPort;
	}

	protected void setDbPort(int dbPort) {
		this.dbPort = dbPort;
	}

	protected void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	protected void setDebugMode(boolean degug) {
		this.debug = degug;
	}
}