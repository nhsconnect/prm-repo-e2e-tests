package uk.nhs.prm.e2etests.performance.awsauth;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.sqs.model.Message;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.nhs.prm.e2etests.queue.BasicSqsClient;
import org.springframework.stereotype.Component;
import uk.nhs.prm.e2etests.queue.TestSqsClient;
import software.amazon.awssdk.regions.Region;
import uk.nhs.prm.e2etests.queue.SqsMessage;

import java.util.List;

@Component
@Primary
public class AutoRefreshingRoleAssumingSqsClient implements TestSqsClient {
    private final TestSqsClient client;
    private final AwsCredentialsProvider awsCredentialsProvider;

    public AutoRefreshingRoleAssumingSqsClient(
            AwsCredentialsProvider awsCredentialsProvider
    ) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.client = initialiseSqsClient();
    }

    private TestSqsClient initialiseSqsClient() {
        final SqsClient awsSqsClient = SqsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider)
                .build();

        return new BasicSqsClient(awsSqsClient);
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
