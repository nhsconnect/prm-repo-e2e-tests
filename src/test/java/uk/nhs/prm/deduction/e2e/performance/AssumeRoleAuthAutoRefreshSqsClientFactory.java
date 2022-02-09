package uk.nhs.prm.deduction.e2e.performance;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

public class AssumeRoleAuthAutoRefreshSqsClientFactory {

    private final StsClient stsClient;
    private final String currentRoleArn;

    public AssumeRoleAuthAutoRefreshSqsClientFactory() {
        this.stsClient = StsClient.builder()
                .region(EU_WEST_2)
                .build();
        this.currentRoleArn = stsClient.getCallerIdentity().arn();
    }

    public SqsClient createAssumeRoleAutoRefreshSqsClient() {
        return SqsClient.builder().credentialsProvider(StsAssumeRoleCredentialsProvider
                .builder()
                .stsClient(stsClient)
                .refreshRequest(() -> {
                    System.out.println("Creating refresh session token request");
                    return refreshAssumeRoleRequest("performance-test");
                })
                .build()).build();
    }

    private AssumeRoleRequest refreshAssumeRoleRequest(String roleSessionName) {
        return AssumeRoleRequest.builder()
                .roleArn(extractRoleArnFromCurrentRoleArn(currentRoleArn))
                .roleSessionName(roleSessionName)
                .build();

    }

    private String extractRoleArnFromCurrentRoleArn(String currentRoleArn) {
        String role = currentRoleArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }
}
