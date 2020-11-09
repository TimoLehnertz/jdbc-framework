package main;

import java.util.Arrays;

import jdbc.AutoDb;
import jdbc.Book;
import jdbc.Student;

public class Main {

	public static void main(String[] args) {
		AutoDb.setup("localhost", "root", "", "AutoDb");
//		AutoDb.setDebug(true);
		AutoDb.dropDatabase(); //for resetting
		
		Student s1 = new Student("Max", 11, Arrays.asList(new Book("Herr der Ringe")));
		Student s2 = new Student("Garfield", 12, Arrays.asList(new Book("Harry Potter")));
		
		s1.save();
		s2.save();
		
		System.out.println(AutoDb.getAll(Student.class));
	}
}