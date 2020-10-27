package main;

import jdbc.DbConnector;
import jdbc.Student;

public class Main {

	public static void main(String[] args) {
		DbConnector.getInstance().setDbName("progress");
		System.out.println(new Student("max1", 10).getAll());
	}
}