package jdbc;

import java.util.Arrays;
import java.util.List;

public class Student extends Entity<Student>{

	String name;
	int age;
	List<Book> books;
	
	public Student() {
		
	}
	
	public Student(String name, int age) {
		super();
		this.name = name;
		this.age = age;
		books = Arrays.asList(new Book("Herr der Ringe"), new Book("Harry Potter"));
	}
}