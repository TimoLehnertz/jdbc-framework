package jdbc;

import java.util.List;

/**
 * Class for easy access to AutoDb features
 * Meant to be extended by any class to gain AutoDb features
 * 
 * @author Timo Lehnertz
 *
 */

public class Entity {
	
	/**
	 * Saves/updates this instance and all its content to the database
	 * @return
	 */
	public long save() {
		return AutoDb.save(this);
	}
	
	/**
	 * Deletes this object and its contents from the database
	 * @return true if succsessful
	 */
	public boolean delete() {
		return AutoDb.delete(this);
	}
	
	/**
	 * @return List of all Entity instances ever beeing saved to the database
	 */
	public List<Entity> getAll() {
		return (List<Entity>) AutoDb.getAllFromType(this.getClass(), null);
	}
	
	/**
	 * Overriden to String functionality to easier see relations between objects and its content
	 */
	@Override
	public String toString() {
		return AutoDbUtils.stringifyObject(this);
	}
}