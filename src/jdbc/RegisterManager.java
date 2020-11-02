package jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Timo Lehnertz
 *
 */

public class RegisterManager {

	private RegisterManager() {
		super();
	}
	
	private static RegisterManager instance;
	
	private Map<Object, Long> registeredObjects = new HashMap<Object, Long>();
	
	public static RegisterManager getInstance() {
		if(instance == null) {
			instance = new RegisterManager();
		}
		return instance;
	}
	
	public boolean registerObject(Object o, long id) {
		if(!registeredObjects.containsKey(o)) {
			registeredObjects.put(o, id);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isObjectRegistered(Object o) {
		return registeredObjects.containsKey(o);
	}
	
	public long getIdFromObject(Object o) {
		if(registeredObjects.containsKey(o)) {
			return registeredObjects.get(o);
		} else {
			return -1;
		}
	}
	
	public boolean deleteObject(Object o) {
		if(registeredObjects.containsKey(o)) {
			registeredObjects.remove(o);
			return true;
		} else {
			return false;
		}
	}
	
	public boolean doesIdExistsForType(Class<?> clazz, int id) {
		Iterator<Entry<Object, Long>> it = registeredObjects.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Object, Long> pair = it.next();
	       
	        if(pair.getKey().getClass() == clazz && pair.getValue() == id) {
	        	return true;
	        } else {
	        }
	    }
	    return false;
	}
	
	public Object getObjectFromClassId(Class<?> clazz, int id) {
		Iterator<Entry<Object, Long>> it = registeredObjects.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Object, Long> pair = it.next();
	        if(pair.getKey().getClass() == clazz && pair.getValue() == id) {
	        	return pair.getKey();
	        }
	    }
	    return null;
	}
}
