package uk.nhs.prm.e2etests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class E2eTestsApplication {
	public static void main(String[] args) {
		SpringApplication.run(E2eTestsApplication.class, args);
	}
}