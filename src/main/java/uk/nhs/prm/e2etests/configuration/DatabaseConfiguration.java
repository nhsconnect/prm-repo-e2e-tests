package uk.nhs.prm.e2etests.configuration;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.property.PostgresDbProperties;
import org.springframework.context.annotation.Bean;
import lombok.AllArgsConstructor;

import javax.sql.DataSource;

@Configuration
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DatabaseConfiguration {
    private final PostgresDbProperties postgresDbProperties;

    @Bean
    public DataSource ehrOutDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(org.postgresql.Driver.class.getName());
        dataSource.setUrl(postgresDbProperties.getJdbcUrl());
        dataSource.setUsername(postgresDbProperties.getEhrOutDatabaseUsername());
        dataSource.setPassword(postgresDbProperties.getEhrOutDatabasePassword());

        return dataSource;
    }
}
