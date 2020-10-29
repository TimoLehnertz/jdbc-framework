package jdbc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Manager {

	private Manager() {
		super();
	}
	
	private static Manager instance;
	
	private Map<Object, Integer> registeredObjects = new HashMap<Object, Integer>();
	
	public static Manager getInstance() {
		if(instance == null) {
			instance = new Manager();
		}
		return instance;
	}
	
	public boolean registerObject(Object o, int id) {
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
	
	public int getIdFromObject(Object o) {
		System.out.println(registeredObjects);
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
		Iterator<Entry<Object, Integer>> it = registeredObjects.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Object, Integer> pair = it.next();
	       
	        if(pair.getKey().getClass() == clazz && pair.getValue() == id) {
	        	return true;
	        } else {
	        }
	    }
	    return false;
	}
	
	public Object getObjectFromClassId(Class<?> clazz, int id) {
		Iterator<Entry<Object, Integer>> it = registeredObjects.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry<Object, Integer> pair = it.next();
	        if(pair.getKey().getClass() == clazz && pair.getValue() == id) {
	        	return pair.getKey();
	        }
	    }
	    return null;
	}
}
