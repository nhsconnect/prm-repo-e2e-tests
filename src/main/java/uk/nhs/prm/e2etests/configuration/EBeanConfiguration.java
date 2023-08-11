package uk.nhs.prm.e2etests.configuration;

import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.property.PostgresDbProperties;
import org.springframework.context.annotation.Bean;
import io.ebean.config.DatabaseConfig;
import io.ebean.DatabaseFactory;
import io.ebean.Database;

import java.util.Properties;

@Configuration
public class EBeanConfiguration {
    private final PostgresDbProperties databaseProperties;

    public EBeanConfiguration(
            PostgresDbProperties databaseProperties
    ) {
        this.databaseProperties = databaseProperties;
    }

    @Bean
    public Database ehrOutDatabase() {
        DatabaseConfig configuration = new DatabaseConfig();

        Properties properties = new Properties();
        properties.put("ebean.db.ddl.generate", "true");
        properties.put("ebean.db.ddl.run", "true");
        properties.put("datasource.db.databaseDriver", "org.postgresql.Driver");
        properties.put("datasource.db.username", databaseProperties.getEhrOutDatabaseUsername());
        properties.put("datasource.db.password", databaseProperties.getEhrOutDatabasePassword());
        properties.put("datasource.db.databaseUrl", String.format("jdbc:postgresql://%s:5432/%s",
                databaseProperties.getEhrOutDatabaseHost(),
                databaseProperties.getEhrOutDatabaseName()));

        configuration.loadFromProperties(properties);

        return DatabaseFactory.create(configuration);
    }
}