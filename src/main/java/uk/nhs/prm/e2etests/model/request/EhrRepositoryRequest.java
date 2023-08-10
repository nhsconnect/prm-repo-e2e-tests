package uk.nhs.prm.e2etests.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

@Getter
public class EhrRepositoryRequest {
    @JsonProperty("type")
    private static final String TYPE = "messages";

    private final UUID id;
    private final UUID conversationId;
    private final String nhsNumber;
    private final String messageType;
    private final List<UUID> fragmentMessageIds;

    public EhrRepositoryRequest(UUID messageId, UUID conversationId, String nhsNumber, String messageType, List<UUID> fragmentMessageIds) {
        this.id = messageId;
        this.conversationId = conversationId;
        this.nhsNumber = nhsNumber;
        this.messageType = messageType;
        this.fragmentMessageIds = fragmentMessageIds;
    }

    public String getJsonString() {
        return StringUtils.deleteWhitespace(String.format("""
        {
          "data": {
            "type": "messages",
            "id": "%s",
            "attributes": {
              "conversationId": "%s",
              "messageType": "%s",
              "nhsNumber": "%s",
              "fragmentMessageIds": %s
            }
          }
        }""", id.toString(), conversationId.toString(), messageType, nhsNumber, fragmentMessageIds.stream()
                .map(messageId -> String.format("\"%s\"", messageId))
                .toList()));
    }
}