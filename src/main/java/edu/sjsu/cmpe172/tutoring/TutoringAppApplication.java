package edu.sjsu.cmpe172.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // scan compenents or sub folders: controller, service, repository, etc.
public class TutoringAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutoringAppApplication.class, args);
	}

}
