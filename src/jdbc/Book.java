package jdbc;

public class Book extends Entity<Book>{

	String name;
	
	public Book() {
		super();
		this.name = "empty";
	}
	
	public Book(String name) {
		super();
		this.name = name;
	}
}