package jdbc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Book extends Entity{

	String name;
	List<Student> previousOwners = new ArrayList<Student>();
	List<Student> previousOwners2 = new ArrayList<Student>();
	
	Student currentOwner;
	
	List<List<Character>> pages  = new ArrayList<>();
	
	public Book() {
		super();
		this.name = "empty constructor";
	}
	
	public Book(String name) {
		super();
		this.name = name;
		previousOwners.add(new Student("Hans", 30, new ArrayList<>()));
		previousOwners.add(new Student("Peter", 40, new ArrayList<>()));
		previousOwners2.add(new Student("TEST", 30, new ArrayList<>()));
		previousOwners2.add(new Student("dsasadas", 40, new ArrayList<>()));
		pages.add(Arrays.asList('A', 'B', 'c'));
		pages.add(Arrays.asList('A', 'B', 'c'));
		pages.add(Arrays.asList('A', 'B', 'c'));
		pages.add(Arrays.asList('A', 'B', 'c'));
		currentOwner = new Student("My Owner", 50, null);
//		pages.addAll(Arrays.asList("Page1", "Page2"));
	}
}