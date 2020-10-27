package jdbc;

public class Student extends Entity<Student>{

	String name;
	int age;
	
	public Student() {
		
	}
	
	public Student(String name, int age) {
		super();
		this.name = name;
		this.age = age;
	}
}