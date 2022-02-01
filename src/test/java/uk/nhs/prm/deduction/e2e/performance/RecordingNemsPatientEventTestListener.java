package uk.nhs.prm.deduction.e2e.performance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;

import java.util.Date;
import java.util.Hashtable;

public class RecordingNemsPatientEventTestListener implements NemsPatientEventTestListener {
    public int testItemCount() {
        return nemsMessageIdToNhsNumberPairs.size();
    }

    final Hashtable nemsMessageIdToNhsNumberPairs = new Hashtable<>();

    @Override
    public void onStartingTestItem(NemsTestEvent testEvent) {
        nemsMessageIdToNhsNumberPairs.put(testEvent.nemsMessageId(), testEvent);
    }

    @Override
    public void onStartedTestItem(NemsTestEvent testEvent) {
        System.out.println("Started test on " + new Date() + " " + testEvent.nemsMessageId() + " " + testEvent.nhsNumber());
    }

    public boolean hasMessage(SqsMessage sqsMessage) throws JsonProcessingException {
        String nemsMessageIdFromBody = extractNemsMessageIdFromBody(sqsMessage);
        if (nemsMessageIdToNhsNumberPairs.containsKey(nemsMessageIdFromBody)) {
            var testEvent = (NemsTestEvent) nemsMessageIdToNhsNumberPairs.get(nemsMessageIdFromBody);
            testEvent.finished(sqsMessage);
            nemsMessageIdToNhsNumberPairs.remove(nemsMessageIdFromBody);
            return true;
        } else {
            System.out.println("Nems message ID " + nemsMessageIdFromBody + " not sent by performance test");
            return false;
        }
    }

    private String extractNemsMessageIdFromBody(SqsMessage sqsMessage) throws JsonProcessingException {
        JsonNode parent = new ObjectMapper().readTree(sqsMessage.body());
        String nemsMessageId = parent.get("nemsMessageId").asText();
        System.out.println("extracted nems message id from message: " + nemsMessageId);
        return nemsMessageId;
    }
}
