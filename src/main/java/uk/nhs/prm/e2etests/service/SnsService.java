package uk.nhs.prm.e2etests.service;

import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;

public class SnsService {
    private final SnsClient snsCLient;

    @Autowired
    public SnsService(AwsCredentialsProvider awsCredentialsProvider) {
        this.snsCLient = SnsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    public String subscribeQueueToTopic(String queueUrl, String topicArn) {
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .topicArn(topicArn)
                .protocol("sqs")
                .endpoint(queueUrl)
                .returnSubscriptionArn(true)
                .build();
        SubscribeResponse subscribeResponse = snsCLient.subscribe(subscribeRequest);
        return subscribeResponse.subscriptionArn();
    }
}
