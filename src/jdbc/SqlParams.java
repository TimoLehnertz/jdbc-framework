package jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mysql.cj.exceptions.WrongArgumentException;

/**
 * Class fo
 * @author Timo Lehnertz
 *
 */

public class SqlParams {

	public String argTypes = "";
	public String sql = "";
	public List<Object> data = new ArrayList<>();
	
	public SqlParams() {
		super();
	}
	
	public SqlParams(String sql) {
		super();
		this.sql = sql;
	}
	
	public SqlParams(String argTypes, String sql, Object[] data) {
		this(argTypes, sql, Arrays.asList(data));
	}
	
	public SqlParams(String argTypes, String sql, List<Object> data) {
		super();
		this.argTypes = argTypes;
		this.sql = sql;
		this.data = data;
	}
	
	public boolean add(Object ... o) {
		for (Object object : o) {
			if(object == null) {
				argTypes += SupportedTypes.getCharRepresentationOftype(String.class);
				data.add(object);
				continue;
			}
			if(!SupportedTypes.isTypeSupported(object.getClass())) {
				continue;
			}
			try {
				argTypes += SupportedTypes.getCharRepresentationOftype(object.getClass());
				data.add(object);
			} catch(WrongArgumentException e) {
				System.err.println(e.getMessage());
			}
		}
		return true;
	}
	
	public PreparedStatement bindParams(PreparedStatement stmt) throws SQLException {
		return bindParams(stmt, argTypes, data.toArray());
	}
	
	static PreparedStatement bindParams(PreparedStatement stmt, String argTypes, Object[] objects) throws SQLException {
		if(argTypes != null) {
			if(argTypes.length() != objects.length) {
				throw new IllegalArgumentException("Number of given objects doesnt match argTypes String! given " + objects.length);
			} else {
				for (int i = 0; i < argTypes.length(); i++) {
					try {
						switch(argTypes.charAt(i)) {
						case SupportedTypes.BYTE: stmt.setByte(i + 1, (byte) objects[i]); break;
						case SupportedTypes.SHORT: stmt.setShort(i + 1, (short) objects[i]); break;
						case SupportedTypes.INT: stmt.setInt(i + 1, (int) objects[i]); break;
						case SupportedTypes.LONG: stmt.setLong(i + 1, (long) objects[i]); break;
						case SupportedTypes.FLOAT: stmt.setFloat(i + 1, (float) objects[i]); break;
						case SupportedTypes.DOUBLE: stmt.setDouble(i + 1, (double) objects[i]); break;
						case SupportedTypes.BOOLEAN: stmt.setBoolean(i + 1, (boolean) objects[i]); break;
						case SupportedTypes.STRING: stmt.setString(i + 1, (String) objects[i]); break;
						case SupportedTypes.DATE: stmt.setDate(i + 1, (java.sql.Date) objects[i]); break;
						case SupportedTypes.TIMESTAMP: stmt.setTimestamp(i + 1, (java.sql.Timestamp) objects[i]); break;
						case SupportedTypes.TIME: stmt.setTime(i + 1, (java.sql.Time) objects[i]); break;
						default: throw new IllegalArgumentException("Invalid use ofargTypes in Bind param!");
						}
					} catch(ClassCastException e) {
						System.out.println(argTypes.charAt(i));
						e.printStackTrace();
					}
				}
			}
		}
		return stmt;
	}
}