package uk.nhs.prm.e2etests.performance.awsauth;

import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.e2etests.queue.BasicSqsClient;
import uk.nhs.prm.e2etests.queue.SqsMessage;
import uk.nhs.prm.e2etests.queue.TestSqsClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableScheduling
@Component
@Primary
public class AutoRefreshingRoleAssumingSqsClient implements TestSqsClient {
    private TestSqsClient client;
    private final AssumeRoleCredentialsProviderFactory credentialsProviderFactory;

    public AutoRefreshingRoleAssumingSqsClient(
            AssumeRoleCredentialsProviderFactory credentialsProviderFactory
    ) {
        warnIfUsingStaticEnvironmentAuth();
        this.credentialsProviderFactory = credentialsProviderFactory;
        this.client = reAuthenticateSqsClient();
    }

    @Scheduled(fixedRate = 30, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void refreshSqsClient() {
        System.out.println("Refreshing SQS client in " + getClass());
        this.client = reAuthenticateSqsClient();
    }

    private TestSqsClient reAuthenticateSqsClient() {
        final AwsCredentialsProvider credentialsProvider = credentialsProviderFactory
                .createProvider();

        try(final SqsClient awsSqsClient = SqsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(credentialsProvider)
                .build()) {

            return new BasicSqsClient(awsSqsClient);
        }
    }

    private void warnIfUsingStaticEnvironmentAuth() {
        if (System.getenv("AWS_ACCESS_KEY_ID") != null) {
            System.err.println("NB: Setup to assume-role on auto-refresh (creating " + getClass() +
                    "), but currently using time-limited env auth (AWS_ACCESS_KEY_ID is set) " +
                    "so this will fail after the base credentials expire...");
        }
    }

    @Override
    public List<SqsMessage> readMessagesFrom(String queueUrl) {
        return client.readMessagesFrom(queueUrl);
    }

    @Override
    public List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout) {
        return client.readThroughMessages(queueUrl, visibilityTimeout);
    }
    @Override
    public void deleteMessageFrom(String queueUrl, Message message) {
        client.deleteMessageFrom(queueUrl, message);
    }

    public void postAMessage(String queueUrl, String message) {
        client.postAMessage(queueUrl, message);
    }

    public void postAMessage(String queueUrl, String message, String attributeKey, String attributeValue) {
        client.postAMessage(queueUrl, message, attributeKey, attributeValue);
    }

    @Override
    public void deleteAllMessagesFrom(String queueUrl) {
        client.deleteAllMessagesFrom(queueUrl);
    }
}
