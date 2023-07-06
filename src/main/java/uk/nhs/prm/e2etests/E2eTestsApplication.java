package uk.nhs.prm.e2etests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class E2eTestsApplication {

	/*
	 No point attempting to run this, it doesn't do anything
	 We just need a main method annotated with @SpringBootApplication to make the spring application context load neatly
	 */
	public static void main(String[] args) {
		SpringApplication.run(E2eTestsApplication.class, args);
	}

}
