package jdbc;

import java.sql.Date;
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
//	Book favBook;
//	Date geburtstag;
	String surname;
	
	public Student() {
		
	}
	
	public Student(String name, int age, List<Book> books) {
		super();
		this.name = name;
		this.age = age;
		this.books = books;
//		favBook = new Book();
//		this.geburtstag = new Date(System.nanoTime());
	}
}