package jdbc;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The heart of AutoDbs logic
 * 
 * @author Timo Lehnertz
 *
 */

public class AutoDbUtils {

	static DbConnector db = DbConnector.getInstance();
	static RegisterManager manager = RegisterManager.getInstance();
	
	static final String TABLE_NAME_IDENTIFIER = "j_d_b_c_";
	static final String FOREIGN_KEY_IDENTIFIED = "_FK";
	
	/**
	 * Saves an object and its content into the database
	 * @param o Object to be saved
	 * @return -1 in case of failure or the id of this object
	 */
	protected static long saveObject(Object o) {
		if(!db.isOperatable()) {
			return -1;
		}
		if(!doesTableExistForType(o.getClass())) {
			createTableForType(o.getClass());
		}
		boolean isRegistered = manager.isObjectRegistered(o);
		if(isRegistered) {
			db.execute(getUpdateParamsForObject(o));
		} else {
			long id = db.executeInsert(getInsertParamsForObject(o));
			manager.registerObject(o, id);
		}
		saveObjectsFields(o);
		return manager.getIdFromObject(o);
	}
	
	/**
	 * Deletes an object and all of its contents
	 * @param o Object to be deleted
	 * @return true for success
	 */
	protected static boolean deleteObject(Object o) {
//		Pre checking
		if(!manager.isObjectRegistered(o)) {
			return true;
		} else if(!db.isOperatable()) {
			return false;
		} else {
			long ownId = manager.getIdFromObject(o);
//			Deleting lists
			for (Field listField : getListFieldsFromType(o.getClass())) {
				List<Integer> ids = getLinkIdsFromField(listField, o.getClass(), ownId);
				Class<?> generigType = getGenericClassFromListField(listField);
				for (Object childObject : getAllFromType(generigType, ids)) {
					deleteObject(childObject);
				}
			}
//			deleting sub contents
			for (Field field : getSubclassFieldsForType(o.getClass())) {
				field.setAccessible(true);
				Object subContent;
				try {
					subContent = field.get(o);
					deleteObject(subContent);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			return db.execute(getDeleteParamsForObject(o));
		}
	}
	
	/**
	 * Returns a List of all saved objects in the database from type T
	 * Note this method will return sub objects too
	 * @param <T> Type of objects to search for
	 * @param Type of objects to search for
	 * @param allowedIds All objects with different ids will be skipped. Neccessary for recursive calls. null to be ignored
	 * @return List of Matching Objects
	 */
	public static <T> List<T> getAllFromType(Class<T> type, List<Integer> allowedIds) {
		if(!db.isOperatable()) {
			return new ArrayList<T>();
		}
		if(!doesTableExistForType(type)) {
			createTableForType(type);
		}
		List<T> out = new ArrayList<T>();
		SqlParams selectParams = new SqlParams("Select * FROM `" + getTableNameForType(type) + "`;");
		List<Map<String, Object>> rsList = db.executeQuery(selectParams);
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
				Object registeredObj = manager.getObjectFromClassId(type, identity);
				if(type.isAssignableFrom(registeredObj.getClass())) {
					T registered = type.cast(manager.getObjectFromClassId(type, identity));
					out.add(registered);
				} else {
					System.err.println("Registered object doesnt match");
					continue;
				}
				
//			Is not registered Yet
			} else {
				T entity = (T) getInstance((type));
				
				if(entity == null) {
					continue;
				}
				
//				Simple Fields
				for (Field field : entity.getClass().getDeclaredFields()) {
					if(row.containsKey(field.getName())) {
						field.setAccessible(true);
						try {
							field.set(entity, row.get(field.getName()));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
				
//				Special Types
				for (Field field : getSupportedSpecialFieldsFromType(type)) {
					field.setAccessible(true);
					
//					List Field
					if(isSupportedListField(field)) {
						Class<?> listType = getGenericClassFromListField(field);
						if(listType == null) {
							continue;
						}
						try {
							
		//					list content is primitive
							if(SupportedTypes.isTypeSupported(listType)) {
								field.set(entity, getAllPrimitivesForType(type, identity, field.getName()));
								
		//					list content is object
							} else if(isObjectTypeSupported(listType)){
								field.set(entity, getAllFromType(listType, getLinkIdsFromField(field, type, identity)));
							} else {
								System.err.println(field + " is not supported");
							}
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					} else if(isTypeSubClass(field.getType())){
						if(row.containsKey(field.getName() + FOREIGN_KEY_IDENTIFIED)) {
							try {
								if(row.get(field.getName() + FOREIGN_KEY_IDENTIFIED) != null) {
									Object content = getObject(field.getType(), (int) row.get(field.getName() + FOREIGN_KEY_IDENTIFIED));
									if(content != null) {
										field.set(entity, content);
									}
								}
							} catch (IllegalArgumentException | IllegalAccessException e) {
								e.printStackTrace();
							}
						}else {
							Thread.dumpStack();
						}
					} else {
						System.err.println(field.getName() + " is not supported Yet");
					}
				}
				manager.registerObject(entity, identity);
				out.add(entity);
			}
		}
		return out;
	}
	
	/**
	 * get the SqlParams to delete a given object
	 * Note: this sql doesn not delete any content of this object except from its many to many entries
	 * @param o
	 * @return
	 */
	private static SqlParams getDeleteParamsForObject(Object o) {
		if(!manager.isObjectRegistered(o)) {
			return null;
		}
		long ownId = manager.getIdFromObject(o);
		SqlParams params = new SqlParams("DELETE FROM `" + getTableNameForType(o.getClass()) + "` WHERE identity = ?;");
		params.add(ownId);
		return params;
	}
	
	/**
	 * Stringifies an object to somewhat of a json format
	 * Used for improved toString() functionality
	 * 
	 * @param o Object to be strinified
	 * @return String representation
	 */
	public static String stringifyObject(Object o) {
		return stringifyObject(o, 0);
	}
	
	/**
	 * Creates a table for an entities List/Array to contain the list
	 * 
	 * idprimitive  | from | fieldname
	 * 
	 * @param forType
	 * @param primitiveType
	 * @param fieldName
	 * @return
	 */
	private static boolean createPrimitiveTable(Class<?> forType, Class<?> primitiveType, String fieldName) {
		String tableName = getPrimitivaTableName(forType, fieldName);
		if(db.doesTableExist(tableName)) {
			return true;
		}
		String sql = "CREATE TABLE " + tableName + " (idprimitive INT AUTO_INCREMENT, " + getTableNameForType(forType) + " INT, " +
				fieldName + " " + SupportedTypes.javaTypeToMysqlType(primitiveType) + ", " +
				"PRIMARY KEY(idprimitive, " + getTableNameForType(forType) + "), " +
				"FOREIGN KEY(" + getTableNameForType(forType) + ") REFERENCES " + getTableNameForType(forType) + "(identity));";
		return db.execute(sql);
	}
	
	private static String getPrimitivaTableName(Class<?> forType, String fieldName) {
		return getTableNameForType(forType) + "_has_" + fieldName;
	}
	
	/**
	 * Saves all contents of an object
	 * @param o Object to save its fields
	 * @return succsess
	 */
	private static boolean saveObjectsFields(Object o) {
		if(!manager.isObjectRegistered(o)) {
			saveObject(o);
		}
		for (Field field : getSupportedSpecialFieldsFromType(o.getClass())) {
			if(isSupportedListField(field)) {
				Field listField = field;
				Class<?> listType = getGenericClassFromListField(listField);
				List<?> listContent;
				
				try {
					listContent = (List<?>) listField.get(o);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
					return false;
				}
				if(listContent == null) {
					continue;
				}
//				Is primitive
				if(SupportedTypes.isTypeSupported(listType)) {
					long ownId = manager.getIdFromObject(o);
					db.execute("DELETE FROM " + getPrimitivaTableName(o.getClass(), listField.getName()) +
							" WHERE " + getTableNameForType(o.getClass()) + " = " + ownId);
					
					SqlParams insertParams = new SqlParams( "INSERT INTO `" + getPrimitivaTableName(o.getClass(), listField.getName()) + "`(`" + getTableNameForType(o.getClass()) + "`, `" + listField.getName() + "`) VALUES ");
					boolean first = true;
					for (Object elem :listContent) {
						insertParams.sql += (first ? "" : ", ") + "(?, ?)";
						insertParams.add(ownId);
						insertParams.add(SupportedTypes.getMysqlValueFromObject(elem));
						first = false;
					}
					insertParams.sql += ";";
					if(listContent.size() > 0) {
						db.executeInsert(insertParams);
					}
					
//				Subclass
				} else if(isTypeSubClass(listType)){
					List<Long> idList = new ArrayList<>();
					for (Object listElem : listContent) {
						long id = saveObject(listElem);
						idList.add(id);
					}
					if(idList.size() == 0) {
						continue;
					}
					createLinkTable(o.getClass(), getGenericClassFromListField(listField));
					SqlParams deleteParams = new SqlParams( "DELETE FROM `" + (getLinkTableName(o.getClass(), getGenericClassFromListField(listField))) + "` ");
					deleteParams.sql += "WHERE field_name LIKE ? AND `" + getTableNameForType(o.getClass()) + "` = ?;";
					deleteParams.add(listField.getName());
					deleteParams.add(manager.getIdFromObject(o));
					db.execute(deleteParams);
					SqlParams insertParams = new SqlParams( "INSERT INTO `" + getLinkTableName(o.getClass(), getGenericClassFromListField(listField)) + "` (field_name, `" + getTableNameForType(getGenericClassFromListField(listField)) + "`, `" + getTableNameForType(o.getClass()) + "`) VALUES ");
					boolean first = true;
					for (Long id : idList) {
						insertParams.sql += (first ? "" : ", ") + "(?, ?, ?)";
						insertParams.add(listField.getName());
						insertParams.add(id);
						insertParams.add(manager.getIdFromObject(o));
						first = false;
					}
					db.executeInsert(insertParams);
				} else {
					System.err.println(listField + " Is not supported");
				}
			} else if(isTypeSubClass(field.getType())){
				try {
					Object subClass = field.get(o);
					if(subClass != null) {
						SqlParams updateParams = new SqlParams("Update `" + getTableNameForType(o.getClass()) + "` SET `" + field.getName() + FOREIGN_KEY_IDENTIFIED + "` = ?;");
						updateParams.add(saveObject(subClass));
						db.execute(updateParams);
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the type of the specified Field is a subtype of Entity
	 */
	private static boolean isTypeSubClass(Class<?> type) {
		return Entity.class.isAssignableFrom(type) && type != Entity.class;
	}
	
	private static boolean isSupportedListField(Field field) {
		return getGenericClassFromListField(field) != null;
	}
	
	/**
	 * get all supported List fields
	 * @param from type
	 * @return List of Fields
	 */
	private static List<Field> getListFieldsFromType(Class<?> type){
		List<Field> out = new ArrayList<>();
		for (Field field : getSupportedSpecialFieldsFromType(type)) {
			field.setAccessible(true);
			if(isSupportedListField(field)) {
				out.add(field);
			}
		}
		return out;
	}

	/**
	 * gets the insert sql in SqlParams form for inserting a new object into the database
	 * @param o
	 * @return SqlParam
	 */
	private static SqlParams getInsertParamsForObject(Object o) {
		SqlParams params = new SqlParams("INSERT INTO `" + getTableNameForType(o.getClass()) + "`(");
		boolean firstField = true;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			params.sql += (firstField ? "" : ", ") + "`" + field.getName() + "`";
			firstField = false;
		}
		params.sql += ") VALUES (";
		firstField = true;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			try {
				params.add(field.get(o));
				params.sql += (firstField ? "" : ", ") + "?";
				firstField = false;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		params.sql += ");";
		return params;
	}
	
	/**
	 * Creates the table for this type of objects
	 * @param type to cerate table for
	 * @return succsess
	 */
	private static boolean createTableForType(Class<?> type) {
		if(doesTableExistForType(type)) {
			return true;
		}
		String createString = "CREATE TABLE `" + getTableNameForType(type) + "` (identity INT PRIMARY KEY AUTO_INCREMENT";
		for (Field field : getValidSimpleColumnTypesForClass(type)) {
			createString += ", `" + field.getName() + "` " + SupportedTypes.javaTypeToMysqlType(field.getType());
		}
		for (Field field : getSubclassFieldsForType(type)) {
			createString += ", `" + field.getName() + FOREIGN_KEY_IDENTIFIED + "` INT, FOREIGN KEY(`" + field.getName() + FOREIGN_KEY_IDENTIFIED + "`) REFERENCES `" + getTableNameForType(field.getType()) + "`(identity) ON DELETE CASCADE ON UPDATE CASCADE";
		}
		createString += ");";
		return db.execute(createString) && createTablesForTypesFields(type);
	}
	
	private static List<Field> getSubclassFieldsForType(Class<?> type){
		List<Field> out = new ArrayList<>();
		for (Field field : fieldsDeclaredDirectlyIn(type)) {
			if(isTypeSubClass(field.getType())) {
				out.add(field);
			}
		}
		return out;
	}
	
	/**
	 * creates the tables neccessary for object types content
	 * @param type to create tables for
	 * @return succsess
	 */
	private static boolean createTablesForTypesFields(Class<?> type) {
		boolean succsess = true;
//		List Fields
		for (Field field : getSupportedSpecialFieldsFromType(type)) {
			Class<?> listType = getGenericClassFromListField(field);
			if(listType == null) {
				continue;
			}
//			Is primitive
			if(SupportedTypes.isTypeSupported(listType)) {
				if(!createPrimitiveTable(type, listType, field.getName())) {
					succsess = false;
				}
				
//			Should be a saveable Object
			} else {
				if(!createTableForType(listType) || !createLinkTable(type, listType)) {
					succsess = false;
				}
			}
		}
		return succsess;
	}
	
	/**
	 * Creates the link table between two types
	 * @param a Type a
	 * @param b Type b
	 * @return succsess
	 */
	private static boolean createLinkTable(Class<?> a, Class<?> b) {
		if(db.doesTableExist(getLinkTableName(a, b))) {
			return true;
		}
		String createString = "CREATE TABLE `" + getLinkTableName(a, b) + "`("+
				"field_name varchar(75) NOT NULL,`" +
				getTableNameForType(a) + "` INT NOT NULL,`" +
				getTableNameForType(b) + "` INT NOT NULL," +
				"PRIMARY KEY(field_name, `" + getTableNameForType(a) + "`, `" + getTableNameForType(b) + "`)," +
				"FOREIGN KEY(`" + getTableNameForType(b) + "`) REFERENCES `" + getTableNameForType(b) + "`(identity) ON DELETE CASCADE ON UPDATE CASCADE," +
				"FOREIGN KEY(`" + getTableNameForType(a) + "`) REFERENCES `" + getTableNameForType(a) + "`(identity) ON DELETE CASCADE ON UPDATE CASCADE);";
		db.execute(createString);
		return false;
	}
	
	/**
	 * Gets the supported special types in form of a list of fields supported special fields are
	 * 		Lists wich have Entity contents or primitives as content
	 * 		Subclasses
	 * 
	 * @param type
	 * @return
	 */
	private static List<Field> getSupportedSpecialFieldsFromType(Class<?> type){
		List<Field> fields = new ArrayList<Field>();

		for (Field field : type.getDeclaredFields()) {
//			Lists
			if(isSupportedListField(field)) {
				fields.add(field);
//			Subclasses
			} else if(isTypeSubClass(field.getType())) {
				fields.add(field);
			}
		}
		return fields;
	}
	
	private static SqlParams getUpdateParamsForObject(Object o) {
		SqlParams updateParams = new SqlParams("Update `" + getTableNameForType(o.getClass()) + "` SET");
		boolean firstField = true;
		firstField = true;
		boolean impact = false;
		for (Field field : getValidSimpleColumnTypesForClass(o.getClass())) {
			try {
				updateParams.sql += (firstField ? "" : ", ") + " `" + field.getName() + "` = ?";
				updateParams.add(SupportedTypes.getMysqlValueFromField(field, o));
				firstField = false;
				impact = true;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		for (Field field : getSubclassFieldsForType(o.getClass())) {
			updateParams.sql += (firstField ? "" : ", ") + " `" + field.getName() + FOREIGN_KEY_IDENTIFIED + "` = ?";
			field.setAccessible(true);
			try {
				Entity fieldEntity = (Entity) field.get(o);
				updateParams.add(saveObject(fieldEntity));
				impact = true;
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
			updateParams.add();
			firstField = false;
		}
		updateParams.sql += " WHERE identity = ?;";
		updateParams.add(manager.getIdFromObject(o));
		System.out.println(updateParams.sql);
		if(impact) {
			return updateParams;
		} else {
			return new SqlParams("");
		}
	}
	
	/**
	 * get an object by type an id
	 * @param type to search for
	 * @param id to search for
	 * @return Object or null
	 */
	private static Object getObject(Class<?> type, int id) {
		List<?> list = getAllFromType(type, Arrays.asList(id));
		if(list.size() > 0) {
			return list.get(0);
		}
		return null;
	}
	
	private static List<?> getAllPrimitivesForType(Class<?> forType, long typeId, String fieldName){
		List<Object> list = new ArrayList<>();
		SqlParams selectParams = new SqlParams("SELECT ? FROM `" + getPrimitivaTableName(forType, getPrimitivaTableName(forType, fieldName)) + "` WHERE ? = ?;");
		selectParams.add(fieldName, typeId);
		
		List<Map<String, Object>> result = db.executeQuery(selectParams);
		for (Map<String, Object> row : result) {
			list.add(SupportedTypes.javaObjectFromSql(row.get(fieldName)));
		}
		return list;
	}
	
	private static boolean isObjectTypeSupported(Class<?> type) {
		return Entity.class.isAssignableFrom(type);
	}
	
	/**
	 * gets the first generic parameter from list field
	 * used to find out what type a list ist of
	 * @param listField to get from
	 * @return Class type of generic content
	 */
	private static Class<?> getGenericClassFromListField(Field listField){
		if(listField.getType() != List.class) {
			return null;
		}
		Type genericType = listField.getGenericType();
		if(genericType instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) genericType;
			Type[] types = pt.getActualTypeArguments();
            if(types[0] instanceof Class) {
            	 return (Class<?>) pt.getActualTypeArguments()[0];
            } else {
            	System.err.println("Only Lists with primitive or Entities are valid! Issued field: " + listField);
            	return null;
            }
		} else{
			System.err.println("not parameterized: " + genericType);
			return null;
		}
	}
	
	private static List<Integer> getLinkIdsFromField(Field listField, Class<?> type, long ownId){
		Class<?> linktype = getGenericClassFromListField(listField);
		SqlParams selectParams = new SqlParams("SELECT `" + getTableNameForType(linktype) + "` FROM `" + getLinkTableName(type, linktype) + "` WHERE field_name LIKE ? AND `" + getTableNameForType(type) + "` = ?;");
		selectParams.add(listField.getName(), ownId);
		
		List<Integer> list = new ArrayList<>();
		List<Map<String, Object>> result = db.executeQuery(selectParams);
		for (Map<String, Object> row : result) {
			list.add((int) row.get(getTableNameForType(linktype)));
		}
		return list;
	}
	
	/**
	 * Returns all own (primitive) fields wich are not of a special type and supported
	 * @return List of fields
	 */
	private static List<Field> getValidSimpleColumnTypesForClass(Class<?> clazz){
		List<Field> out = new ArrayList<Field>();
		for (Field field : fieldsDeclaredDirectlyIn(clazz)) {
			if(SupportedTypes.isTypeSupported(field.getType()) && field.getName() != "identity") {
				out.add(field);
			}
		}
		return out;
	}
	
	/**
	 * get an new instance from a class
	 * @param <T> type
	 * @param clazz to instantiate from
	 * @return new instance of type T
	 */
	private static<T> T getInstance(Class<T> clazz) {
		try {
			return clazz.getConstructor().newInstance();
		} catch (Exception e) {
			System.err.println("Please provide a default Constructor with no arguments for Class \"" + clazz + "\"");
			return null;
		}
	}
	
	/**
	 * Does the table for this entity exist?
	 * @return does it?
	 */
	private static boolean doesTableExistForType(Class<?> type) {
		return DbConnector.getInstance().doesTableExist(getTableNameForType(type));
	}
	
	private static List<Field> fieldsDeclaredDirectlyIn(Class<?> c) {
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
	private static String getTableNameForType(Class<?> type) {
		return parseTableName(type.getName());
	}
	
	private static String getLinkTableName(Class<?> a, Class<?> b) {
		return getTableNameForType(a) + "_has_" + getTableNameForType(b);
	}
	
	/**
	 * Used for naming convention
	 * @param name of Table
	 * @return the parsed Name
	 */
	private static String parseTableName(String tableName) {
		return tableName.replaceAll("\\.", "___");
	}
	
	/**
	 * @param n
	 * @return n tabs as String
	 */
	private static String getnTabs(int n) {
		String out = "";
		for (int i = 0; i < n; i++) {
			out += "\t";
		}
		return out;
	}
	
	private static String stringifyObject(Object o, int tabs) {
		tabs++;
		if(SupportedTypes.isTypeSupported(o.getClass())) {
			if(o instanceof String) {
				return "\"" + o.toString() + "\"";
			} else {
				return o.toString();
			}
		}
		if(o instanceof List) {
			List<?> list = (List<?>) o;
			String out = "[";
			boolean first = true;
			for (Object elem : list) {
				out += (first ? "" : ", ") + stringifyObject(elem, tabs);
				first = false;
			}
			return out + "]";
		}
		
		String out = "\"" + o.getClass().getSimpleName() + "\": {\n " + getnTabs(tabs) + " \"ID\":\"" + RegisterManager.getInstance().getIdFromObject(o) + "\"";
		Field[] fields = o.getClass().getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				Object fieldContent = field.get(o);
				if(fieldContent != null) {
					out +=",\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": " + stringifyObject(fieldContent, tabs);
				} else {
					out +=",\n" + getnTabs(tabs) +  " \"" + field.getName() + "\": NULL" ;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return out + "\n" + getnTabs(tabs - 1) + "}";
	}
}