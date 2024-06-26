package uk.nhs.prm.e2etests.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

@Service
public class SnsService {
    private final SnsClient snsCLient;

    @Autowired
    public SnsService(AwsCredentialsProvider awsCredentialsProvider) {
        this.snsCLient = SnsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public String subscribeQueueToTopic(String queueArn, String topicArn) {
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(queueArn)
                .returnSubscriptionArn(true)
                .build();
        SubscribeResponse subscribeResponse = snsCLient.subscribe(subscribeRequest);
        return subscribeResponse.subscriptionArn();
    }
}
