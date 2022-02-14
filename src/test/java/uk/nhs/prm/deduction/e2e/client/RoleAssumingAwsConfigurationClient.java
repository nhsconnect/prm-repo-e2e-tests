package uk.nhs.prm.deduction.e2e.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;

@Component
@Slf4j
public class RoleAssumingAwsConfigurationClient extends BasicAwsConfigurationClient {
    public RoleAssumingAwsConfigurationClient(AssumeRoleCredentialsProviderFactory credentialsProviderFactory) {
        super();
        super.setSsmClient(SsmClient.builder()
                .credentialsProvider(credentialsProviderFactory.createProvider())
                .region(Region.EU_WEST_2)
                .build());
    }
}
