package uk.nhs.prm.e2etests.model.database;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
public abstract class CommonAttributes {
    private String inboundConversationId;
    private String layer;
    private String outboundConversationId;
    private String transferStatus;
    private String failureCode;
    private String failureReason;
    private String createdAt;
    private String updatedAt;
    private Integer deletedAt;

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
}