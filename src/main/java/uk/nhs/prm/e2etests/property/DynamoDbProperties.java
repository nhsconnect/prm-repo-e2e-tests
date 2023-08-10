package uk.nhs.prm.e2etests.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DynamoDbProperties {
    @Value("${aws.configuration.databaseNames.transferTrackerDb}")
    private String transferTrackerDbName;

    @Value("${aws.configuration.databaseNames.activeSuspensionsDb}")
    private String activeSuspensionsDbName;
}
