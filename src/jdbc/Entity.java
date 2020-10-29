package jdbc;

import java.lang.reflect.Field;
import java.util.List;


public class Entity<T extends Entity<?>> {
	
	public Entity() {
		super();
	}
	
	public int save() {
		return JDBCUtils.saveObject(this);
	}
	
	public List<T> getAll() {
		return (List<T>) JDBCUtils.getAllFromType(getClass(), null);
	}

	
	/**
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
	 * @param tabs initial tabs used for recursive calls
	 * @return
	 */
	public String toStringJson(int tabs) {
		tabs++;
		String out = "\"" + this.getClass().getSimpleName() + "\": {\n " + getnTabs(tabs) + " \"ID\":\"" + Manager.getInstance().getIdFromObject(this) + "\"";
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
	
//	@Override
//	public String toString() {
//		return toStringJson(0);
//	}
}