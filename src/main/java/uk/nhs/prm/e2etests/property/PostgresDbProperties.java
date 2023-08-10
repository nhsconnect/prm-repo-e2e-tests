package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.service.SsmService;

@Component
public class PostgresDbProperties {
    @Value("${aws.configuration.ssm.parameters.ehrOut.database.host}")
    private String ehrOutDatabaseHost;

    @Value("${aws.configuration.ssm.parameters.ehrOut.database.name}")
    private String ehrOutDatabaseName;

    @Value("${aws.configuration.ssm.parameters.ehrOut.database.username}")
    private String ehrOutDatabaseUsername;

    @Value("${aws.configuration.ssm.parameters.ehrOut.database.password}")
    private String ehrOutDatabasePassword;

    private final SsmService ssmService;
    
    @Autowired
    public PostgresDbProperties(SsmService ssmService) {
        this.ssmService = ssmService;
    }

    public String getEhrOutDatabaseHost() {
        return this.ssmService.getSsmParameterValue(this.ehrOutDatabaseHost);
    }

    public String getEhrOutDatabaseName() {
        return this.ssmService.getSsmParameterValue(this.ehrOutDatabaseName);
    }

    public String getEhrOutDatabaseUsername() {
        return this.ssmService.getSsmParameterValue(this.ehrOutDatabaseUsername);
    }

    public String getEhrOutDatabasePassword() {
        return this.ssmService.getSsmParameterValue(this.ehrOutDatabasePassword);
    }
}