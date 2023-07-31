package uk.nhs.prm.e2etests.model;

import lombok.Getter;
import org.json.JSONException;
import org.json.JSONObject;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import uk.nhs.prm.e2etests.exception.SqsMessageException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Getter
public class SqsMessage {
    private final Message message;
    private final String id;
    private final Map<String, MessageAttributeValue> attributes;
    private final String body;
    private final LocalDateTime queuedAt;

    public SqsMessage(Message message) {
        this.message = message;
        this.id = message.messageId();
        this.attributes = message.messageAttributes();
        this.body = message.body();
        this.queuedAt = getDateFromMillisecondsAsString(message.attributesAsStrings().get("SentTimestamp"));
    }

    public String getNemsMessageId() {
        return getAttributeByJsonKey("nemsMessageId");
    }

    public String getPreviousGp() {
        return getAttributeByJsonKey("previousOdsCode");
    }

    public boolean contains(String substring) {
        return body.toLowerCase().contains(substring.toLowerCase());
    }

    private LocalDateTime getDateFromMillisecondsAsString(String millisecondsAsString) {
        if (millisecondsAsString == null) {
            return null;
        }
        long milliseconds = Long.parseLong(millisecondsAsString);
        Instant instant = Instant.ofEpochMilli(milliseconds);
        return instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String getAttributeByJsonKey(String key) {
        try {
            return new JSONObject(getBody())
                    .get(key)
                    .toString();
        } catch (JSONException exception) {
            throw new SqsMessageException(exception.getMessage());
        }
    }
}
