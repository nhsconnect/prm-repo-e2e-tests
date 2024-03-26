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

    // TODO - PRMT-4663 - USE @Value WHERE IT IS REQUIRED,
    // TODO - PRMT-4663 - LET'S NOT COUPLE THESE NAMES!
    @Value("${aws.configuration.databaseNames.transferTrackerDb}")
    private String transferTrackerDbName;

    // TODO - PRMT-4663 - USE @Value WHERE IT IS REQUIRED,
    // TODO - PRMT-4663 - LET'S NOT COUPLE THESE NAMES!
    @Value("${aws.configuration.databaseNames.activeSuspensionsDb}")
    private String activeSuspensionsDbName;
}
