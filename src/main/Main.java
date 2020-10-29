package main;

import jdbc.DbConnector;
import jdbc.Student;

public class Main {

	public static void main(String[] args) {
		DbConnector.getInstance().setDbName("progress7");
		DbConnector.getInstance().execute("DROP DATABASE IF EXISTS " + DbConnector.getInstance().getDbName() + ";");
		new Student("max1", 11).save();
		new Student("max2", 12).save();
		new Student("max3", 13).save();
		System.out.println(new Student("max1", 10).getAll());
		System.out.println(new Student("max1", 10).getAll());
	}
}