package jdbc;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JDBCUtils {

	static final List<Class<?>> supportedTypes = Arrays.asList(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Short.TYPE, Short.class, Integer.TYPE,
			Integer.class, Long.TYPE, Long.class, Float.TYPE, Float.class, Double.class, Double.TYPE, Double.class, Double.TYPE,
			Double.class, java.math.BigDecimal.class, java.math.BigInteger.class, java.util.Date.class, java.sql.Date.class, java.sql.Time.class,
			java.sql.Timestamp.class, String.class);
	
	public static boolean isFieldSupported(Field f) {
		return supportedTypes.contains(f.getType());
	}
	
	public static String FieldToMysqlType(Field f) {
		Class<?> c = f.getType();
		String out = "";
		/**
		 * Numeric
		 */
		if(c == Boolean.TYPE || c == Boolean.class) {
			out = "BIT(1)";
		}
		if(c == Byte.TYPE || c == Byte.class) {
			out = "TINYINT";
		}
		if(c == Short.TYPE || c == Short.class) {
			out = "BIT(16)";
		}
		if(c == Integer.TYPE || c == Integer.class) {
			out = "INT";
		}
		if(c == Long.TYPE || c == Long.class) {
			out = "BIGINT";
		}
		if(c == Float.TYPE || c == Float.class) {
			out = "FLOAT";
		}
		if(c == Double.TYPE || c == Double.class) {
			out = "DOUBLE";
		}
		if(c == java.math.BigDecimal.class) {
			out = "DECIMAL";
		}
		if(c == java.math.BigInteger.class) {
			out = "DECIMAL (precision = 0)";
		}
		
		/**
		 * Date
		 */
		if(c == java.util.Date.class) {
			out = "DATETIME";
		}
		if(c == java.sql.Date.class) {
			out = "DATE";
		}
		if(c == java.sql.Time.class) {
			out = "TIME";
		}
		if(c == java.sql.Timestamp.class) {
			out = "	DATETIME";
		}
		
		/**
		 * Variable-width types
		 */
		if(c == String.class) {
			out = "TEXT";
		}
		return out + " NOT NULL";
	}
	
	public static String getMysqlValueFromField(Field f, Object ctx) throws IllegalArgumentException, IllegalAccessException {
		Class<?> c = f.getType();
		String out = "";
		/**
		 * Numeric
		 */
		if(c == Boolean.TYPE || c == Boolean.class) {
			return f.getBoolean(ctx) ? "TRUE" : "FALSE";
		}
		if(c == Byte.TYPE || c == Byte.class) {
			return f.getByte(ctx) + "";
		}
		if(c == Short.TYPE || c == Short.class) {
			return f.getShort(ctx) + "";
		}
		if(c == Integer.TYPE || c == Integer.class) {
			return f.getInt(ctx) + "";
		}
		if(c == Long.TYPE || c == Long.class) {
			return f.getLong(ctx) + "";
		}
		if(c == Float.TYPE || c == Float.class) {
			return f.getFloat(ctx) + "";
		}
		if(c == Double.TYPE || c == Double.class) {
			return f.getDouble(ctx) + "";
		}
		if(c == java.math.BigDecimal.class) {
			return f.getDouble(ctx) + "";
		}
		if(c == java.math.BigInteger.class) {
			return f.getLong(ctx) + "";
		}
		
		/**
		 * Date
		 */
		if(c == java.util.Date.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) f.get(ctx)) + "'";
		}
		if(c == java.sql.Date.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) f.get(ctx)) + "'";
		}
		if(c == java.sql.Time.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Time) f.get(ctx)) + "'";
		}
		if(c == java.sql.Timestamp.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Timestamp) f.get(ctx)) + "'";
		}
		
		/**
		 * Variable-width types
		 */
		if(c == String.class) {
			return "\"" + f.get(ctx) + "\"";
		}
		return out;
	}
	
	public static List<Field> fieldsDeclaredDirectlyIn(Class<?> c) {
	    final List<Field> l = new ArrayList<Field>();
	    for (Field f : c.getDeclaredFields())
	        if (f.getDeclaringClass().equals(c))
	            l.add(f);
	    return l;
	}
	
	/**
	 * @param f Field to be checked
	 * @return true if the type of the specified Field is a subtype of Entity but not an Entity itself
	 */
	public static boolean isFieldSubClass(Field f) {
		return Entity.class.isAssignableFrom(f.getType()) && f.getType() != Entity.class;
	}
}