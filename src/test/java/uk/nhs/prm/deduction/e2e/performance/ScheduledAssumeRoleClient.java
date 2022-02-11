package uk.nhs.prm.deduction.e2e.performance;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.util.concurrent.TimeUnit;

import static software.amazon.awssdk.regions.Region.EU_WEST_2;

@Component
public class ScheduledAssumeRoleClient {

    private final StsClient stsClient;
    private final String currentRoleArn;

    public ScheduledAssumeRoleClient() {
        this.stsClient = StsClient.builder()
                .region(EU_WEST_2)
                .build();
        this.currentRoleArn = stsClient.getCallerIdentity().arn();
    }

    @Scheduled(fixedRate = 30, initialDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void refreshAssumeRole() {
        stsClient.assumeRole(refreshAssumeRoleRequest("performance-test"));
        System.out.println("Assumed Role to refresh credentials");
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
