package uk.nhs.prm.e2etests.configuration;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.property.PostgresDbProperties;

import java.util.Properties;

@Configuration
public class EBeanConfiguration {
    private final PostgresDbProperties databaseProperties;

    public EBeanConfiguration(
            PostgresDbProperties databaseProperties
    ) {
        this.databaseProperties = databaseProperties;
    }

//    /**
//     * Simple Spring bean factory for creating the EbeanServer.
//     */
//    public class MyEbeanServerFactory implements FactoryBean<EbeanServer> {
//
//        public EbeanServer getObject() throws Exception {
//
//            return createEbeanServer();
//        }
//
//        public Class<?> getObjectType() {
//            return EbeanServer.class;
//        }
//
//        public boolean isSingleton() {
//            return true;
//        }
//
//        /**
//         * Create a EbeanServer instance.
//         */
//        private EbeanServer createEbeanServer() {
//
//            ServerConfig config = new ServerConfig();
//            config.setName("pg");
//
//            // load configuration from ebean.properties
//            config.loadFromProperties();
//            config.setDefaultServer(true);
//    ...
//            // other programmatic configuration
//
//            return EbeanServerFactory.create(config);
//        }
//    }




    @Bean
    public Database ehrOutDatabase() {
        DatabaseConfig configuration = new DatabaseConfig();

        Properties properties = new Properties();
        properties.put("ebean.db.ddl.generate", "true");
        properties.put("ebean.db.ddl.run", "true");
        properties.put("datasource.db.databaseDriver", "org.postgresql.Driver");
        properties.put("datasource.db.username", databaseProperties.getEhrOutDatabaseUsername());
        properties.put("datasource.db.password", databaseProperties.getEhrOutDatabasePassword());
        properties.put("datasource.db.databaseUrl", String.format("jdbc:postgresql://%s/%s",
                databaseProperties.getEhrOutDatabaseHost(),
                databaseProperties.getEhrOutDatabaseName()));

        configuration.loadFromProperties(properties);
        return DatabaseFactory.create(configuration);
    }
}