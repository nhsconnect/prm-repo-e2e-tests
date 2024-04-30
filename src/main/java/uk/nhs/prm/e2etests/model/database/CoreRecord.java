package uk.nhs.prm.e2etests.model.database;

import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@Getter
@DynamoDbBean
public final class CoreRecord extends CommonAttributes {
    private String inboundMessageId;
    private String outboundMessageId;
    private String receivedAt;

    // Setters
    @DynamoDbAttribute("ReceivedAt")
    public void setReceivedAt(String receivedAt) {
        this.receivedAt = receivedAt;
    }

    @DynamoDbAttribute("InboundMessageId")
    public void setInboundMessageId(String inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
    }

    @DynamoDbAttribute("OutboundMessageId")
    public void setOutboundMessageId(String outboundMessageId) {
        this.outboundMessageId = outboundMessageId;
    }
}