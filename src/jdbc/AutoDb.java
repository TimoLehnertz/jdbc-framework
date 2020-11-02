package jdbc;

import java.io.IOException;
import java.util.List;

/**
 * Class for easy interaction
 * 
 * @author Timo Lehnertz
 *
 */

public class AutoDb {

	/**
	 * Simplified acces to DbConnector Object
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Private Constructor as never needed
	 */
	private AutoDb() {
		super();
	}
	
	/**
	 * 
	 * @param host URL to database server default: localhost
	 * @param username to database server default: root
	 * @param password to database server default: <empty>
	 * @param port 	   to database server default: 3306
	 */
	public static void setup(String host, String username, String password, String dbName,  int port) {
		setDbUrl(host);
		setDbUser(username);
		setDbPassowrd(password);
		setDbName(dbName);
		setDbPort(port);
	}
	
	public static void setup(String host, String username, String password, String dbName) {
		setup(host, username, password, dbName,  3306);
	}
	
	public static void setup(String host, String username, String password) {
		setup(host, username, password, "test");
	}
	
	/**
	 * Save an object into the satabase
	 * @param entity Entity to be saved
	 * @return id from this object or -1 in case of error
	 */
	public static long save(Entity entity) {
		return AutoDbUtils.saveObject(entity);
	}
	
	
	/**
	 * Deletes an Entity and its contents
	 * @param entity to be deleted
	 * @return succsess
	 */
	public static boolean delete(Entity entity) {
		return AutoDbUtils.deleteObject(entity);
	}
	
	/**
	 * Returns a List of all saved objects in the database from type T
	 * Note this method will return sub objects too
	 * @param <T> Type of objects to search for
	 * @param Type of objects to search for
	 * @param allowedIds All objects with different ids will be skipped. Neccessary for recursive calls
	 * @return List of Matching Objects
	 */
	protected static <T> List<T> getAllFromType(Class<T> type, List<Integer> allowedIds){
		return AutoDbUtils.getAllFromType(type, allowedIds);
	}
	
	/**
	 * Simplified version to get all entities from class
	 * @param <T>
	 * @param type
	 * @return
	 */
	public static <T> List<T> getAll(Class<T> type){
		return getAllFromType(type, null);
	}
	
	/**
	 * Drops the database forever! (A long time!)
	 * @return
	 */
	public static boolean dropDatabase() {
		System.out.println("Do you really want to delete \"" + getDbName() + "\"? y/n");
		int i = 0;
		try {
			i = System.in.read();
			System.out.println(i);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String c = (i == 121 ? "y" : "n");
		if(c.equals("y") || c.equals("Y")) {
			return db.execute("DROP DATABASE IF EXISTS `" + db.getDbName() +"`;");
		} else {
			System.out.println("abort");
			return false;
		}
	}
	
	/**
	 * Getters /  Setters
	 */
	public static String getDbUser() {
		return db.getDbUser();
	}

	public static void setDbUser(String dbUser) {
		db.setDbUser(dbUser);
	}

	public static String getDbPassowrd() {
		return db.getDbPassowrd();
	}

	public static void setDbPassowrd(String dbPassowrd) {
		db.setDbPassowrd(dbPassowrd);
	}

	public static String getDbName() {
		return db.getDbName();
	}

	public static void setDbName(String dbName) {
		db.setDbName(dbName);
	}

	public static int getDbPort() {
		return db.getDbPort();
	}

	public static void setDbPort(int dbPort) {
		db.setDbPort(dbPort);
	}

	public static void setDbUrl(String dbUrl) {
		db.setDbUrl(dbUrl);
	}

	public static void setDebug(boolean degug) {
		db.setDebugMode(degug);
	}
}