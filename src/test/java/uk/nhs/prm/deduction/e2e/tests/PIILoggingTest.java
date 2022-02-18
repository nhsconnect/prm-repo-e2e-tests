package uk.nhs.prm.deduction.e2e.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.client.CloudWatchClient;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;

@SpringBootTest(classes = {
        PIILoggingTest.class,
        TestConfiguration.class,
        AssumeRoleCredentialsProviderFactory.class,
        AutoRefreshingRoleAssumingSqsClient.class,
        CloudWatchClient.class
})
public class PIILoggingTest {

    @Autowired
    CloudWatchClient cloudWatchClient;

    @Test
    void checkThatCloudWatchLogsDontContainPIIInfo(){
        cloudWatchClient.getLogs();
    }
}
