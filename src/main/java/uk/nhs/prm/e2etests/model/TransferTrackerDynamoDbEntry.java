package uk.nhs.prm.e2etests.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
// TODO PRMT-3744 refine this class so it's a proper entity, right now it's used in one place and can be made more concise
public class TransferTrackerDynamoDbEntry {
    private String conversationId;
    private String largeEhrCoreMessageId;
    private String nemsMessageId;
    private String nhsNumber;
    private String sourceGp;
    private String state;
    private String nemsEventLastUpdated;
    private String createdAt;
    private String lastUpdatedAt;

    public TransferTrackerDynamoDbEntry(String conversationId,
                                        String largeEhrCoreMessageId,
                                        String nemsMessageId,
                                        String nhsNumber,
                                        String sourceGp,
                                        String state,
                                        String nemsEventLastUpdated,
                                        String createdAt,
                                        String lastUpdatedAt){
        this.conversationId = conversationId;
        this.largeEhrCoreMessageId = largeEhrCoreMessageId;
        this.nemsMessageId = nemsMessageId;
        this.nhsNumber = nhsNumber;
        this.sourceGp = sourceGp;
        this.state = state;
        this.nemsEventLastUpdated = nemsEventLastUpdated;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String toJsonString() {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(this);
    }
}
