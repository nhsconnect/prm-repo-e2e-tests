package uk.nhs.prm.e2etests.configuration;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.e2etests.property.PostgresDbProperties;
import org.springframework.context.annotation.Bean;
import lombok.AllArgsConstructor;

import javax.sql.DataSource;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Configuration
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class DatabaseConfiguration {
    private final PostgresDbProperties postgresDbProperties;
    private final AwsCredentialsProvider awsCredentialsProvider;

    // Registers the DataSource bean for the EHR Out PostgreSQL database.
    @Bean
    public DataSource ehrOutDatabase() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(org.postgresql.Driver.class.getName());
        dataSource.setUrl(postgresDbProperties.getJdbcUrl());
        dataSource.setUsername(postgresDbProperties.getEhrOutDatabaseUsername());
        dataSource.setPassword(postgresDbProperties.getEhrOutDatabasePassword());

        return dataSource;
    }

    // Registers the enhanced Dynamo DB client for AWS.
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient() {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(DynamoDbClient.builder()
                        .region(EU_WEST_2)
                        .credentialsProvider(awsCredentialsProvider).build())
                .build();
    }
}