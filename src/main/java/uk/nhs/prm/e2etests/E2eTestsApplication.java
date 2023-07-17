package uk.nhs.prm.e2etests;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

// TODO: Temporarily disabled Hibernate / Spring Data JPA AutoConfiguration.
@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	DataSourceTransactionManagerAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class
})
public class E2eTestsApplication {
	public static void main(String[] args) {
		SpringApplication.run(E2eTestsApplication.class, args);
	}
}
