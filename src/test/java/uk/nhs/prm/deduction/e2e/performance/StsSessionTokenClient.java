package uk.nhs.prm.deduction.e2e.performance;

import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.StsException;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

public class StsSessionTokenClient {

    private final StsClient stsClient;
    private final String currentRoleArn;

    public StsSessionTokenClient() {
        this.stsClient = StsClient.builder()
                .region(EU_WEST_2)
                .build();
        this.currentRoleArn = stsClient.getCallerIdentity().arn();
    }

    public void refreshSessionToken(String roleSessionName) {

        try {
            AssumeRoleRequest roleRequest = AssumeRoleRequest.builder()
                    .roleArn(extractRoleArnFromCurrentRoleArn(currentRoleArn))
                    .roleSessionName(roleSessionName)
                    .build();

            stsClient.assumeRole(roleRequest);

            System.out.println("Requested new client token for role");

        } catch (StsException e) {
            System.out.println(e.getMessage());
        }
    }

    private String extractRoleArnFromCurrentRoleArn(String currentRoleArn) {
        String role = currentRoleArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }
}
