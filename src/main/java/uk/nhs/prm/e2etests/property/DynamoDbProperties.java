package uk.nhs.prm.e2etests.property;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0")
@Getter
@Component
public class DynamoDbProperties {
    @Deprecated(since="2.0.0")
    @Value("${aws.configuration.databaseNames.oldTransferTrackerDb}")
    private String oldTransferTrackerDbName;

    @Value("${aws.configuration.databaseNames.transferTrackerDb}")
    private String transferTrackerDbName;

    @Value("${aws.configuration.databaseNames.activeSuspensionsDb}")
    private String activeSuspensionsDbName;
}
