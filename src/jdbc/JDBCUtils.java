package jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class JDBCUtils {

	static DbConnector db = DbConnector.getInstance();
	static Manager manager = Manager.getInstance();
	
	static final String TABLE_NAME_IDENTIFIER = "j_d_b_c_";
	
	public static int saveObject(Object o) {
		if(!doesTableExistForType(o.getClass())) {
			createTableForType(o.getClass());
		}
		boolean isRegistered = manager.isObjectRegistered(o);
		if(isRegistered) {
			db.execute(getUpdateStringForObject(o));
		} else {
			int id = db.executeInsert(getInsertStringForObject(o));
			manager.registerObject(o, id);
		}
		saveObjectsListFields(o);
		return manager.getIdFromObject(o);
	}
	
	static boolean saveObjectsListFields(Object o) {
		if(!manager.isObjectRegistered(o)) {
			saveObject(o);
		}
		for (Field listField : getListFieldsFromType(o.getClass())) {
			List<Integer> idList  = new ArrayList<>();
			try {
				List<?> listContent = (List<?>) listField.get(o);
				for (Object listElem : listContent) {
					int id = saveObject(listElem);
					idList.add(id);
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
			db.execute("DELETE FROM " + getLinkTableName(o.getClass(), getGenericClassFromListField(listField)) +
					" WHERE field_name LIKE \"" + listField.getName() + "\" AND " + getTableNameForType(o.getClass()) + "=" + manager.getIdFromObject(o) + ";");
			String insertString = "INSERT INTO " + getLinkTableName(o.getClass(), getGenericClassFromListField(listField)) + "(field_name, " + getTableNameForType(getGenericClassFromListField(listField)) + ", " + getTableNameForType(o.getClass()) + ") VALUES ";
			boolean first = true;
			for (Integer id : idList) {
				insertString += (first ? "" : ", ") + "(\"" + listField.getName() + "\", " + id + ", " + manager.getIdFromObject(o) + ")";
				first = false;
			}
			db.executeInsert(insertString + ";");
		}
		return false;
	}
	
	static String getInsertStringForObject(Object o) {
		String out = "INSERT INTO " + getTableNameForType(o.getClass()) + "(";
		boolean firstField = true;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			out += (firstField ? "" : ", ") + field.getName();
			firstField = false;
		}
		out += ") VALUES (";
		firstField = true;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			try {
				out += (firstField ? "" : ", ") + SupportedTypes.getMysqlValueFromField(field, o);
				firstField = false;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + ");";
	}
	
	/**
	 * Create The table for this entity
	 */
	private static boolean createTableForType(Class<?> type) {
		if(doesTableExistForType(type)) {
			return true;
		}
		List<Field> simpleFields = getValidSimpleColumnTypesForClass(type);
		String createString = "CREATE TABLE " + getTableNameForType(type) + " (identity INT PRIMARY KEY AUTO_INCREMENT";
		for (Field field : simpleFields) {
			createString += ", `" + field.getName() + "` " + SupportedTypes.FieldToMysqlType(field);
		}
		createString += ");";
		return db.execute(createString) && createTablesForTypesFields(type);
	}
	
	private static boolean createTablesForTypesFields(Class<?> type) {
		boolean succsess = true;
		for (Field listField : getListFieldsFromType(type)) {
			ParameterizedType listType = (ParameterizedType) listField.getGenericType();
			Class<?> listGeneric = (Class<?>) listType.getActualTypeArguments()[0];
			if(!createTableForType(listGeneric) || !createLinkTable(type, listGeneric)) {
				succsess = false;
			}
		}
		return succsess;
	}
	
	private static boolean createLinkTable(Class<?> a, Class<?> b) {
		if(db.doesTableExist(getLinkTableName(a, b))) {
			return true;
		}
		String createString = "CREATE TABLE " + getLinkTableName(a, b) + "("+
				"field_name varchar(75) NOT NULL," +
				getTableNameForType(a) + " INT NOT NULL," +
				getTableNameForType(b) + " INT NOT NULL," +
				"PRIMARY KEY(field_name, " + getTableNameForType(a) + ", " + getTableNameForType(b) + ")," +
				"FOREIGN KEY(" + getTableNameForType(b) + ") REFERENCES " + getTableNameForType(b) + "(identity) ON DELETE CASCADE);";
		db.execute(createString);
		return false;
	}
	
	static List<Field> getListFieldsFromType(Class<?> type){
		List<Field> fields = new ArrayList<Field>();
		for (Field field : type.getDeclaredFields()) {
			if(field.getType() == List.class) {
				fields.add(field);
			}
		}
		return fields;
	}
	
	static String getUpdateStringForObject(Object o) {
		String out = "UPDATE " + getInsertStringForObject(o) + " ";
		boolean firstField = true;
		firstField = true;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			try {
				out += firstField ? "" : ", " + "SET `" + field.getName() + "` = " + SupportedTypes.getMysqlValueFromField(field, o);
				firstField = false;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		int id = manager.getIdFromObject(o);
		return out + " WHERE identity=" + id + ";";
	}
	
	public static <T> List<T> getAllFromType(Class<T> type, List<Integer> allowedIds) {
		if(!doesTableExistForType(type)) {
			createTableForType(type);
		}
		DbConnector db = DbConnector.getInstance();
		List<T> out = new ArrayList<T>();
		List<Map<String, Object>> rsList = db.executeQuery("SELECT * FROM " + getTableNameForType(type) + ";");
		for (Map<String, Object> row : rsList) {
			int identity = (int) row.get("identity");
//			Skip ids
			if(allowedIds != null) {
				if(!allowedIds.contains(identity)) {
					continue;
				}
			}
//			is already registered
			if(manager.doesIdExistsForType(type, identity)) {
				T registered = (T) manager.getObjectFromClassId(type, identity);
				out.add(registered);
//			Is not registered Yet
			} else {
				Iterator<Map.Entry<String, Object>> it = row.entrySet().iterator();
				T entity = (T) getInstance((type));
				Field[] fields = entity.getClass().getDeclaredFields();
				while (it.hasNext()) {
					Map.Entry<String, Object> pair = (Map.Entry<String, Object>)it.next();
					String key = pair.getKey();
					Object value = pair.getValue();
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
				}
//				list relations
				for (Field listField : getListFieldsFromType(type)) {
					listField.setAccessible(true);
					try {
						listField.set(entity, getAllFromType(getGenericClassFromListField(listField), getLinkIdsFromField(listField, type)));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
				manager.registerObject(entity, identity);
				out.add(entity);
			}
		}
		return out;
	}
	
	static Class<?> getGenericClassFromListField(Field listField){
		ParameterizedType listType = (ParameterizedType) listField.getGenericType();
		return (Class<?>) listType.getActualTypeArguments()[0];
	}
	
	static List<Integer> getLinkIdsFromField(Field listField, Class<?> type){
		Class<?> linktype = getGenericClassFromListField(listField);
		String sql = "SELECT " + getTableNameForType(linktype) + " FROM " + getLinkTableName(type, linktype) + 
				" WHERE field_name LIKE \"" + listField.getName() + "\";";
		List<Integer> list = new ArrayList<>();
		List<Map<String, Object>> result = db.executeQuery(sql);
		for (Map<String, Object> row : result) {
			list.add((int) row.get(getTableNameForType(linktype)));
		}
		return list;
	}
	
	/**
	 * Returns all own (primitive) fields wich are not of a special type and supported
	 * @return List of fields
	 */
	static List<Field> getValidSimpleColumnTypesForClass(Class<?> clazz){
		List<Field> out = new ArrayList<Field>();
		for (Field field : fieldsDeclaredDirectlyIn(clazz)) {
			if(SupportedTypes.isFieldSupported(field) && field.getName() != "identity") {
				out.add(field);
			}
		}
		return out;
	}
	
	public static<T> T getInstance(Class<T> clazz) {
		try {
			return clazz.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Does the table for this entity exist?
	 * @return does it?
	 */
	static boolean doesTableExistForType(Class<?> type) {
		return DbConnector.getInstance().doesTableExist(getTableNameForType(type));
	}
	
	public static List<Field> fieldsDeclaredDirectlyIn(Class<?> c) {
	    final List<Field> l = new ArrayList<Field>();
	    for (Field f : c.getDeclaredFields())
	        if (f.getDeclaringClass().equals(c))
	            l.add(f);
	    return l;
	}
	
	/**
	 * naming
	 */
	
	/**
	 * get the hopyfully unique name of this table
	 * @return
	 */
	static String getTableNameForType(Class<?> type) {
		return parseTableName(type.getName());
	}
	
	static String getLinkTableName(Class<?> a, Class<?> b) {
		return getTableNameForType(a) + "_has_" + getTableNameForType(b);
	}
	
	/**
	 * Used for naming convention
	 * @param name of Table
	 * @return the parsed Name
	 */
	static String parseTableName(String tableName) {
		String cpy = tableName + "";
//		if(cpy.contains(TABLE_NAME_IDENTIFIER)) {
//			cpy.replaceAll(TABLE_NAME_IDENTIFIER, "");
//			cpy.replaceAll("(.*)___(.*)", ".");
//		} else {
			cpy = cpy.replaceAll("\\.", "___");
//			cpy = TABLE_NAME_IDENTIFIER + cpy;
//		}
		return cpy;
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the type of the specified Field is a subtype of Entity but not an Entity itself
	 */
	public static boolean isFieldSubClass(Field f) {
		return Entity.class.isAssignableFrom(f.getType()) && f.getType() != Entity.class;
	}
}