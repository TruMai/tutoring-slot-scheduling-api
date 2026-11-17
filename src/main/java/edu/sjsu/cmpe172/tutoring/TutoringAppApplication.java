package edu.sjsu.cmpe172.tutoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication // scan components or sub folders: controller, service, repository, etc.
@EnableScheduling      // lets @Scheduled methods (ReminderAgent) run
public class TutoringAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutoringAppApplication.class, args);
	}

}