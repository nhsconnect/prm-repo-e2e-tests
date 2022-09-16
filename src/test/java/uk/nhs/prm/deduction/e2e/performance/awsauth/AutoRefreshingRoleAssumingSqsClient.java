package uk.nhs.prm.deduction.e2e.performance.awsauth;

import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.TestSqsClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Primary
public class AutoRefreshingRoleAssumingSqsClient implements TestSqsClient {

    private volatile TestSqsClient client;
    private AssumeRoleCredentialsProviderFactory credentialsProviderFactory;

    public AutoRefreshingRoleAssumingSqsClient(AssumeRoleCredentialsProviderFactory credentialsProviderFactory) {
        this.credentialsProviderFactory = credentialsProviderFactory;
        warnIfUsingStaticEnvironmentAuth();
        this.client = createReauthenticatedSqsClient();
    }

    @Scheduled(fixedRate = 30, initialDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void refreshSqsClient() {
        System.out.println("Refreshing SQS client in " + getClass());
        this.client = createReauthenticatedSqsClient();
    }

    private TestSqsClient createReauthenticatedSqsClient() {
        AwsCredentialsProvider credentialsProvider = credentialsProviderFactory.createProvider();
        SqsClient awsSqsClient = SqsClient.builder()
                .credentialsProvider(credentialsProvider)
                .build();
        return new BasicSqsClient(awsSqsClient);
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

    @Override
    public void deleteAllMessagesFrom(String queueUrl) {
        client.deleteAllMessagesFrom(queueUrl);
    }
}
