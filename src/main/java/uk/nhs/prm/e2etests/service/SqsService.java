package uk.nhs.prm.e2etests.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import uk.nhs.prm.e2etests.enumeration.MessageType;
import uk.nhs.prm.e2etests.exception.MaxQueueEmptyResponseException;
import uk.nhs.prm.e2etests.exception.ServiceException;
import uk.nhs.prm.e2etests.model.SqsMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.nhs.prm.e2etests.utility.ThreadUtility.sleepFor;

@Log4j2
@Service
public class SqsService {
    // Constants
    private static final int SQS_EMPTY_RESPONSE_LIMIT = 10;
    private static final int MAX_VISIBILITY_TIMEOUT = 43200;
    private static final int INITIAL_DELAY_MILLISECONDS = 30000;

    // Beans
    private final SqsClient sqsClient;

    @Autowired
    public SqsService(AwsCredentialsProvider awsCredentialsProvider) {
        this.sqsClient = SqsClient.builder()
                .region(Region.EU_WEST_2)
                .credentialsProvider(awsCredentialsProvider).build();
    }

    /**
     * This method has a complexity of O(N), where N is
     * expectedNumberOfEhrCores + expectedNumberOfFragments,
     * the total number of messages to be received and filtered.
     * @param queueUri The URI of the Amazon SQS Queue.
     * @param expectedNumberOfEhrCores The number of cores we are expecting.
     * @param expectedNumberOfEhrFragments The number of fragments we are expecting.
     * @param outboundConversationId The outboundConversationId.
     * @return If all the messages were found successfully.
     */
    public boolean getAllMessagesFromQueue(int expectedNumberOfEhrCores,
                                           int expectedNumberOfEhrFragments,
                                           String outboundConversationId,
                                           String queueUri) {
        int totalNumberOfMessages = expectedNumberOfEhrCores + expectedNumberOfEhrFragments;
        final List<Message> allMessages = new ArrayList<>();
        boolean allMessagesFound = false;

        int emptyResponseCount = 0;
        int ehrCoreCount = 0;
        int ehrFragmentCount = 0;

        log.info("Waiting for {} seconds for message(s) to hit queue {}.", (INITIAL_DELAY_MILLISECONDS / 1000), queueUri);

        sleepFor(INITIAL_DELAY_MILLISECONDS);

        try {
            while(!allMessagesFound) {
                final List<Message> foundMessages = this.sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUri).maxNumberOfMessages(10).build()).messages().stream()
                        .filter(message -> message.body().contains(outboundConversationId))
                        .toList();

                if(!foundMessages.isEmpty()) {
                    allMessages.addAll(foundMessages);
                    deleteMessages(foundMessages, queueUri);
                    emptyResponseCount = 0;

                    log.info("{} of {} message(s) found with Outbound Conversation ID {}, and deleted them.",
                            allMessages.size(), totalNumberOfMessages, outboundConversationId);

                    if(allMessages.size() == totalNumberOfMessages) allMessagesFound = true;
                } else {
                    ++emptyResponseCount;
                    if(emptyResponseCount >= SQS_EMPTY_RESPONSE_LIMIT) throw new MaxQueueEmptyResponseException();
                }
            }
        } catch (SqsException exception) {
            throw new ServiceException(this.getClass().getName(), exception.getMessage());
        }

        ehrCoreCount += allMessages.stream().filter(message -> message.body().contains(MessageType.EHR_CORE.interactionId)).count();
        ehrFragmentCount += allMessages.stream().filter(message -> message.body().contains(MessageType.EHR_FRAGMENT.interactionId)).count();

        log.info("Operation summary: {} EHR core(s), {} fragment(s) found.", ehrCoreCount, ehrFragmentCount);

        return (ehrCoreCount + ehrFragmentCount) == totalNumberOfMessages;
    }

    /**
     * This method deletes n given messages.
     * @param messages A List of messages to delete.
     * @param queueUri The URI of the queue to delete the messages from.
     */
    private void deleteMessages(List<Message> messages, String queueUri) {
        final List<DeleteMessageBatchRequestEntry> deleteMessageBatchRequestEntries = messages
                .stream()
                .map(message -> DeleteMessageBatchRequestEntry.builder()
                        .id(message.messageId())
                        .receiptHandle(message.receiptHandle())
                        .build())
                .toList();

        final DeleteMessageBatchRequest deleteMessageBatchRequest = DeleteMessageBatchRequest.builder()
                .entries(deleteMessageBatchRequestEntries)
                .queueUrl(queueUri).build();

        this.sqsClient.deleteMessageBatch(deleteMessageBatchRequest);
    }

    public List<SqsMessage> readMessagesFrom(String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .visibilityTimeout(0)
                .queueUrl(queueUrl)
                .waitTimeSeconds(5)
                .maxNumberOfMessages(10)
                .messageAttributeNames("All")
                .attributeNames(QueueAttributeName.ALL)
                .build();

        return receiveMessages(receiveMessageRequest)
                .messages()
                .stream()
                .map(SqsMessage::new)
                .toList();
    }

    public List<SqsMessage> readThroughMessages(String queueUrl, int visibilityTimeout) {
        int safeVisibilityTimeout = Math.min(visibilityTimeout, MAX_VISIBILITY_TIMEOUT);

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
            .visibilityTimeout(safeVisibilityTimeout)
            .queueUrl(queueUrl)
            .waitTimeSeconds(10)
            .maxNumberOfMessages(10)
            .messageAttributeNames("All")
            .attributeNames(QueueAttributeName.ALL)
            .build();

        return receiveMessages(receiveMessageRequest).messages()
                .stream()
                .map(SqsMessage::new)
                .toList();
    }

    public void deleteMessageFrom(String queueUrl, Message message) {
        sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
    }

    public void deleteAllMessagesFrom(String queueUrl) {
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());
    }

    public void postAMessage(String queueUrl, String message, Map<String, String> attributes) {
        final Map<String, MessageAttributeValue> messageAttributes = attributes.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        element -> getMessageAttributeValue(element.getValue())
                ));

        final SendMessageRequest messageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .messageAttributes(messageAttributes)
                .build();

        sqsClient.sendMessage(messageRequest);
    }

    private ReceiveMessageResponse receiveMessages(ReceiveMessageRequest receiveMessageRequest) {
        try {
            return sqsClient.receiveMessage(receiveMessageRequest);
        } catch (Exception exception) {
            throw new ServiceException(getClass().getName(), exception.getMessage());
        }
    }

    private MessageAttributeValue getMessageAttributeValue(String attributeValue) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(attributeValue)
                .build();
    }
}
