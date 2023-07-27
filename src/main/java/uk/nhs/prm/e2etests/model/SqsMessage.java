package uk.nhs.prm.e2etests.model;

import org.json.JSONException;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.nhs.prm.e2etests.exception.SqsMessageException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

public class SqsMessage {
    private final String body;
    private final LocalDateTime queuedAt;
    private final Message message;
    private final Map<String, MessageAttributeValue> attributes;

    public SqsMessage(Message message) {
        this.message = message;
        this.attributes = message.messageAttributes();
        this.body = message.body();
        this.queuedAt = dateFromMillisecondsAsString(message.attributesAsStrings().get("SentTimestamp"));
    }

    public LocalDateTime queuedAt() {
        return queuedAt;
    }

    public boolean contains(String substring) {
        return body.toLowerCase().contains(substring.toLowerCase());
    }

    private LocalDateTime dateFromMillisecondsAsString(String millisecondsAsString) {
        if (millisecondsAsString == null) {
            return null;
        }
        var milliseconds = Long.parseLong(millisecondsAsString);
        var instant = Instant.ofEpochMilli(milliseconds);
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public String id() {
        return message.messageId();
    }

    public String body() {
        return body;
    }
    public Map<String, MessageAttributeValue> attributes() {
        return attributes;
    }

    public String nemsMessageId() {
        return getAttribute("nemsMessageId");
    }

    public String previousGp() {
        return getAttribute("previousOdsCode");
    }

    private String getAttribute(String key) {
        try {
            return asJsonObject().get(key).toString();
        } catch (JSONException exception) {
            throw new SqsMessageException(exception.getMessage());
        }
    }

    private JSONObject asJsonObject() {
        try {
            return new JSONObject(body());
        } catch (JSONException exception) {
            throw new SqsMessageException(exception.getMessage());
        }
    }

    public Message getMessage() {
        return message;
    }
}
