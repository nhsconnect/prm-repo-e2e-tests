package uk.nhs.prm.e2etests.performance.awsauth;

import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import uk.nhs.prm.e2etests.ExampleAssumedRoleArn;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.StsClient;
import org.springframework.stereotype.Component;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Component
public class AssumeRoleCredentialsProviderFactory {

    private final String roleArn;

    @Autowired
    public AssumeRoleCredentialsProviderFactory(
            ExampleAssumedRoleArn exampleAssumedRoleArn
    ) {
        this.roleArn = exampleAssumedRoleArn.getTargetArn();
    }

    public AwsCredentialsProvider createProvider() {
        StsClient stsClient = StsClient.builder()
                .region(EU_WEST_2)
                .build();

        AssumeRoleRequest assumeRoleRequest = AssumeRoleRequest.builder()
                .roleArn(roleArn)
                .roleSessionName("perf-test")
                .build();
        AssumeRoleResponse assumeRoleResponse = stsClient.assumeRole(assumeRoleRequest);

        Credentials creds = assumeRoleResponse.credentials();

        return StaticCredentialsProvider.create(AwsSessionCredentials.create(creds.accessKeyId(), creds.secretAccessKey(), creds.sessionToken()));
    }
}
