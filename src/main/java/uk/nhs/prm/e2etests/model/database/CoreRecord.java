package uk.nhs.prm.e2etests.model.database;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class CoreRecord extends CommonAttributes {
    private String receivedAt;
    private String acknowledgementReceivedAt;
    private String acknowledgementTypeCode;
    private String acknowledgementDetail;

    @DynamoDbAttribute("ReceivedAt")
    public void setReceivedAt(String receivedAt) {
        this.receivedAt = receivedAt;
    }

    @DynamoDbAttribute("AcknowledgementReceivedAt")
    public void setAcknowledgementReceivedAt(String acknowledgementReceivedAt) {
        this.acknowledgementReceivedAt = acknowledgementReceivedAt;
    }

    @DynamoDbAttribute("AcknowledgementTypeCode")
    public void setAcknowledgementTypeCode(String acknowledgementTypeCode) {
        this.acknowledgementTypeCode = acknowledgementTypeCode;
    }

    @DynamoDbAttribute("AcknowledgementDetail")
    public void setAcknowledgementDetail(String acknowledgementDetail) {
        this.acknowledgementDetail = acknowledgementDetail;
    }
}