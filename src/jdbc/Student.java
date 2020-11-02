package jdbc;

import java.util.List;

/**
 * 
 * @author Timo Lehnertz
 *
 */

public class Student extends Entity{

	String name;
	int age;
	List<Book> books;
	
	public Student() {
		
	}
	
	public Student(String name, int age, List<Book> books) {
		super();
		this.name = name;
		this.age = age;
		this.books = books;
	}
}