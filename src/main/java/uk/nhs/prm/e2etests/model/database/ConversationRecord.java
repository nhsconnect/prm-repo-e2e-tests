package uk.nhs.prm.e2etests.model.database;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
import uk.nhs.prm.e2etests.property.TestConstants;

@Getter
@Builder
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class ConversationRecord {
    private String inboundConversationId; // Partition Key
    private String layer; // Sort Key
    private String outboundConversationId;
    private String nhsNumber;
    private String transferStatus;
    private String failureCode;
    private String failureReason;
    private String sourceGp;
    private String destinationGp;
    private String nemsMessageId;
    private String createdAt;
    private String updatedAt;
    private Integer deletedAt;
    private String associatedTest;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("InboundConversationId")
    public void setInboundConversationId(String inboundConversationId) {
        this.inboundConversationId = inboundConversationId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("Layer")
    public void setLayer(String layer) {
        this.layer = layer;
    }

    @DynamoDbAttribute("OutboundConversationId")
    @DynamoDbSecondaryPartitionKey(indexNames = "OutboundConversationIdSecondaryIndex")
    public void setOutboundConversationId(String outboundConversationId) {
        this.outboundConversationId = outboundConversationId;
    }

    @DynamoDbAttribute("NhsNumber")
    @DynamoDbSecondaryPartitionKey(indexNames = "NhsNumberSecondaryIndex")
    public void setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    @DynamoDbAttribute("TransferStatus")
    public void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    @DynamoDbAttribute("FailureCode")
    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    @DynamoDbAttribute("FailureReason")
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @DynamoDbAttribute("SourceGp")
    public void setSourceGp(String sourceGp) {
        this.sourceGp = sourceGp;
    }

    @DynamoDbAttribute("DestinationGp")
    public void setDestinationGp(String destinationGp) {
        this.destinationGp = destinationGp;
    }

    @DynamoDbAttribute("NemsMessageId")
    public void setNemsMessageId(String nemsMessageId) {
        this.nemsMessageId = nemsMessageId;
    }

    @DynamoDbAttribute("CreatedAt")
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("UpdatedAt")
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("DeletedAt")
    public void setDeletedAt(Integer deletedAt) {
        this.deletedAt = deletedAt;
    }

    @DynamoDbAttribute("AssociatedTest")
    public void setAssociatedTest(String associatedTest) {
        this.associatedTest = associatedTest;
    }
}