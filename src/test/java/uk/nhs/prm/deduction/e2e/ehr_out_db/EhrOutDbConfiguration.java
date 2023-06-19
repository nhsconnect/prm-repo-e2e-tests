package uk.nhs.prm.deduction.e2e.ehr_out_db;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.nhs.prm.deduction.e2e.client.RoleAssumingAwsConfigurationClient;

import javax.sql.DataSource;
import java.util.Map;

@Slf4j
@Configuration
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class EhrOutDbConfiguration {
    @Value("${NHS_ENVIRONMENT}")
    private final String environment;
    private final RoleAssumingAwsConfigurationClient roleAssumingAwsConfigurationClient;
    private final Map<String, String> parameters = Map.of(
        "DATABASE_HOST", getAwsParameterStoreValue("/repo/%s/output/prm-repo-ehr-out-service/db-host"),
        "DATABASE_NAME", getAwsParameterStoreValue("/repo/%s/output/prm-repo-ehr-out-service/db-name"),
        "DATABASE_USERNAME", getAwsParameterStoreValue("/repo/%s/user-input/ehr-out-service-db-username"),
        "DATABASE_PASSWORD", getAwsParameterStoreValue("/repo/%s/user-input/ehr-out-service-db-password")
    );

    @Bean
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(getJdbcUrl())
                .username(parameters.get("DATABASE_USERNAME"))
                .password(parameters.get("DATABASE_PASSWORD"))
                .build();
    }

    private String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:5432/%s",
                parameters.get("DATABASE_HOST"),
                parameters.get("DATABASE_NAME"));
    }

    private String getAwsParameterStoreValue(String parameterName) {
        try {
            return roleAssumingAwsConfigurationClient.getParamValue(String.format(parameterName, environment));
        } catch (NullPointerException exception) {
            log.error(exception.getMessage());
            throw exception;
        }
    }
}
