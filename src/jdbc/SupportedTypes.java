package jdbc;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import com.mysql.cj.exceptions.WrongArgumentException;


/**
 * Class for parsing of types between Java and Mysql
 * @author Timo Lehnertz
 *
 */

public class SupportedTypes {

	private static final List<Class<?>> supportedTypes = Arrays.asList(Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Short.TYPE, Short.class, Integer.TYPE,
			Integer.class, Long.TYPE, Long.class, Float.TYPE, Float.class, Double.class, Double.TYPE, Double.class, Double.TYPE,
			Double.class, java.math.BigDecimal.class, java.math.BigInteger.class, java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class,
			java.sql.Timestamp.class, String.class, Character.TYPE, Character.class);
	
	protected static final char BYTE = 'y';
	protected static final char SHORT = 'h';
	protected static final char INT = 'i';
	protected static final char LONG = 'l';
	protected static final char FLOAT = 'f';
	protected static final char DOUBLE = 'd';
	protected static final char BOOLEAN = 'b';
	protected static final char STRING = 's';
	protected static final char DATE = 'a';
	protected static final char TIME = 't';
	protected static final char TIMESTAMP = 'm';
	
	protected static boolean isTypeSupported(Class<?> type) {
		return supportedTypes.contains(type);
	}
	
	protected static String javaTypeToMysqlType(Class<?> c) {
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
		if(c == java.sql.Date.class) {
			out = "DATE";
		}
		if(c == java.sql.Time.class) {
			out = "TIME";
		}
		if(c == java.sql.Timestamp.class) {
			out = "	TIMESTAMP";
		}
		
		/**
		 * Variable-width types
		 */
		if(c == String.class) {
			out = "TEXT";
		}
		return out + "";
	}
	
	protected static char getCharRepresentationOftype(Class<?> c) {
		/**
		 * Numeric
		 */
		if(c == Boolean.TYPE || c == Boolean.class) {
			return BOOLEAN;
		}
		if(c == Byte.TYPE || c == Byte.class) {
			return BYTE;
		}
		if(c == Short.TYPE || c == Short.class) {
			return SHORT;
		}
		if(c == Integer.TYPE || c == Integer.class) {
			return INT;
		}
		if(c == Long.TYPE || c == Long.class) {
			return LONG;
		}
		if(c == Float.TYPE || c == Float.class) {
			return FLOAT;
		}
		if(c == Double.TYPE || c == Double.class) {
			return DOUBLE;
		}
		if(c == java.math.BigDecimal.class) {
			return FLOAT;
		}
		if(c == java.math.BigInteger.class) {
			return LONG;
		}
		
		/**
		 * Date
		 */
		if(c == java.sql.Timestamp.class) {
			return TIMESTAMP;
		}
		if(c == java.sql.Date.class) {
			return DATE;
		}
		if(c == java.sql.Time.class) {
			return TIME;
		}
		
		/**
		 * Variable-width types
		 */
		if(c == String.class) {
			return STRING;
		}
		
		if(c == java.util.Date.class) {
			throw new WrongArgumentException("Type \"" + c + "\" is Not suported maby Use java.sql.Date instead :(");
		}
		throw new WrongArgumentException("Type \"" + c + "\" is Not suported :(");
	}
	
	protected static String getMysqlValueFromField(Field f, Object ctx) throws IllegalArgumentException, IllegalAccessException {
		f.setAccessible(true);
		Class<?> c = f.getType();
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
		return null;
	}
	
	protected static String getMysqlValueFromObject(Object o){
		Class<?> c = o.getClass();
		String out = "";
		/**
		 * Numeric
		 */
		if(c == Boolean.TYPE || c == Boolean.class) {
			return (boolean) o ? "TRUE" : "FALSE";
		}
		if(c == Byte.TYPE || c == Byte.class) {
			return (byte) o + "";
		}
		if(c == Short.TYPE || c == Short.class) {
			return (short) o + "";
		}
		if(c == Integer.TYPE || c == Integer.class) {
			return (int) o + "";
		}
		if(c == Long.TYPE || c == Long.class) {
			return (long) o + "";
		}
		if(c == Float.TYPE || c == Float.class) {
			return (float) o + "";
		}
		if(c == Double.TYPE || c == Double.class) {
			return (double) o + "";
		}
		if(c == java.math.BigDecimal.class) {
			return (java.math.BigDecimal) o + "";
		}
		if(c == java.math.BigInteger.class) {
			return (java.math.BigInteger) o + "";
		}
		
		/**
		 * Date
		 */
		if(c == java.util.Date.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.util.Date) o) + "'";
		}
		if(c == java.sql.Date.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Date) o) + "'";
		}
		if(c == java.sql.Time.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Time) o) + "'";
		}
		if(c == java.sql.Timestamp.class) {
			return "'" + new SimpleDateFormat("yyyy-MM-dd").format((java.sql.Timestamp) o) + "'";
		}
		
		/**
		 * Variable-width types
		 */
		if(c == String.class) {
			return "\"" + (String) o + "\"";
		}
		return out;
	}

	/**
	 * @todo
	 * @param object
	 * @return object
	 */
	protected static Object javaObjectFromSql(Object object) {
		return object;
	}
}
