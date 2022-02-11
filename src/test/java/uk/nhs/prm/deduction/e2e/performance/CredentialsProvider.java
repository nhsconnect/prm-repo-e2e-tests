package uk.nhs.prm.deduction.e2e.performance;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;


@Component
public class CredentialsProvider {

    private final String roleArn;

    public CredentialsProvider() {
        this.roleArn = getRequiredRoleArn();
    }

    public AwsCredentialsProvider loadCredentials() {

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

    private String getRequiredRoleArn() {
        String requiredRoleArn = System.getenv("REQUIRED_ROLE_ARN");
        String role = requiredRoleArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }


}
