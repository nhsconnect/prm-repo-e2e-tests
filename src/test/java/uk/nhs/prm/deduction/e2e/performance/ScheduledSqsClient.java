package uk.nhs.prm.deduction.e2e.performance;

import org.springframework.scheduling.annotation.Scheduled;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;

import java.util.concurrent.TimeUnit;

public class ScheduledSqsClient extends BasicSqsClient {

    private CredentialsProvider credentialsProvider;

    public ScheduledSqsClient(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
        setNewSqsClient();
    }

    @Scheduled(fixedRate = 30, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void setNewSqsClient() {
        AwsCredentialsProvider awsCredentialsProvider = credentialsProvider.loadCredentials();
        SqsClient sqsClient = SqsClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .build();
        this.setSqsClient(sqsClient);
    }
}
