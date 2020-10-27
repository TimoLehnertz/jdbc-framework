package jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class Entity<T extends Entity<?>> {

	static final String TABLE_NAME_IDENTIFIER = "j_d_b_c_";
	private int identity = -1;
	
	public Entity() {
		super();
	}
	
	public boolean save() {
		if(!doesTableExist()) {
			createTable();
		}
		DbConnector db = DbConnector.getInstance();
		if(identity < 0) {
			identity = db.executeInsert(getInsertString());
		} else{
			db.execute(getUpdateString());
		}
		return false;
	}
	
	public List<T> getAll() {
		DbConnector db = DbConnector.getInstance();
		List<T> out = new ArrayList<T>();
		List<Map<String, Object>> rsList = db.executeQuery("SELECT * FROM " + getTableName() + ";");
		for (Map<String, Object> row : rsList) {
			Iterator<Map.Entry<String, Object>> it = row.entrySet().iterator();
			T entity = getInstance((Class<T>) getClass());
			Field[] fields = entity.getClass().getDeclaredFields();
			while (it.hasNext()) {
				Map.Entry<String, Object> pair = (Map.Entry<String, Object>)it.next();
				String key = pair.getKey();
				Object value = pair.getValue();
				if(!key.contentEquals("identity")) {
					for (Field field : fields) {
						try {
							if(field.getName().toLowerCase().contentEquals(key.toLowerCase())) {
								field.setAccessible(true);
								field.set(entity, value);
							}
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				} else {
					try {
						Field idField = getIdFieldFromSubClass(entity.getClass());
						if(idField != null) {
							idField.setAccessible(true);
							idField.set(entity, value);
						}
					} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			out.add(entity);
		}
		return out;
	}
	
	Field getIdFieldFromSubClass(Class<?> class1) {
		for (Field field : class1.getSuperclass().getDeclaredFields()) {
			if(field.getName().contentEquals("identity")) {
				return field;
			}
		}
		return null;
	}
	
	T getInstance(Class<T> clazz) {
		try {
			return clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	String getInsertString() {
		String out = "INSERT INTO " + getTableName() + "(";
		boolean firstField = true;
		for (Field field : getValidSimpleColumnTypes()) {
			out += (firstField ? "" : ", ") + field.getName();
			firstField = false;
		}
		out += ") VALUES (";
		firstField = true;
		for (Field field : getValidSimpleColumnTypes()) {
			try {
				out += (firstField ? "" : ", ") + JDBCUtils.getMysqlValueFromField(field, this);
				firstField = false;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + ");";
	}
	
	String getUpdateString() {
		String out = "UPDATE " + getTableName() + " ";
		boolean firstField = true;
		firstField = true;
		for (Field field : getValidSimpleColumnTypes()) {
			try {
				out += firstField ? "" : ", " + "SET `" + field.getName() + "` = " + JDBCUtils.getMysqlValueFromField(field, this);
				firstField = false;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + " WHERE identity=" + identity + ";";
	}
	
	/**
	 * Create The table for this entity
	 */
	private boolean createTable() {
		if(doesTableExist()) {
			return true;
		}
		DbConnector db = DbConnector.getInstance();
		List<Field> simpleFields = getValidSimpleColumnTypes();
		String createString = "CREATE TABLE " + getTableName() + " (identity INT PRIMARY KEY AUTO_INCREMENT";
		for (Field field : simpleFields) {
			createString += ", `" + field.getName() + "` " + JDBCUtils.FieldToMysqlType(field);
		}
		createString += ");";
		identity = db.executeInsert(createString);
		return identity > -1;
	}

	/**
	 * Returns all own (primitive) fields wich are not of a special type and supported
	 * @return List of fields
	 */
	List<Field> getValidSimpleColumnTypes(Class<?> clazz){
		List<Field> out = new ArrayList<Field>();
		for (Field field : JDBCUtils.fieldsDeclaredDirectlyIn(clazz)) {
			if(JDBCUtils.isFieldSupported(field) && field.getName() != "identity") {
				out.add(field);
			}
		}
		return out;
	}
	
	List<Field> getValidSimpleColumnTypes(){
		return getValidSimpleColumnTypes(this.getClass());
	}
	
	/**
	 * Does the table for this entity exist?
	 * @return does it?
	 */
	boolean doesTableExist() {
		return DbConnector.getInstance().doesTableExist(getTableName());
	}
	
	/**
	 * Naming
	 */
	
	/**
	 * get the hopyfully unique name of this table
	 * @return
	 */
	String getTableName() {
		return parseTableName(getClass().getName());
	}
	
	/**
	 * Used for naming convention
	 * @param name of Table
	 * @return the parsed Name
	 */
	static String parseTableName(String tableName) {
		String cpy = tableName + "";
		if(cpy.contains(TABLE_NAME_IDENTIFIER)) {
			cpy.replaceAll(TABLE_NAME_IDENTIFIER, "");
			cpy.replaceAll("(.*)___(.*)", ".");
		} else {
			cpy = cpy.replaceAll("\\.", "___");
			cpy = TABLE_NAME_IDENTIFIER + cpy;
		}
		return cpy;
	}
	/**
	 * getters / Setters
	 */

	public int getId() {
		return identity;
	}
	
	/**
	 * 
	 * @param n
	 * @return n tabs as String
	 */
	String getnTabs(int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += "\t";
		}
		return out;
	}
	
	/**
	 * 
	 * @param tabs initial tabs used for recursive calls
	 * @return
	 */
	public String toStringJson(int tabs) {
		tabs++;
		String out = "\"" + this.getClass().getSimpleName() + "\": {\n " + getnTabs(tabs) + " \"ID\":\"" + identity + "\"";
		Field[] fields = this.getClass().getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				out +="\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": " + (JDBCUtils.isFieldSubClass(field) && field.get(this) != null ? ((Entity<?>) field.get(this)).toStringJson(tabs) : "\"" + field.get(this) + "\"") + ",";
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + "\n" + getnTabs(tabs - 1) + "}";
	}
	
	@Override
	public String toString() {
		return toStringJson(0);
	}
}