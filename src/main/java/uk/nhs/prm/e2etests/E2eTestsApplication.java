package uk.nhs.prm.e2etests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	DataSourceTransactionManagerAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class
})
public class E2eTestsApplication {

	/*
	 No point attempting to run this, it doesn't do anything
	 We just need a main method annotated with @SpringBootApplication to make the spring application context load neatly
	 */
	public static void main(String[] args) {
		SpringApplication.run(E2eTestsApplication.class, args);
	}

}
