package uk.nhs.prm.e2etests.model.database;

import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
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
    public final void setInboundConversationId(String inboundConversationId) {
        this.inboundConversationId = inboundConversationId;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("Layer")
    public final void setLayer(String layer) {
        this.layer = layer;
    }

    @DynamoDbAttribute("OutboundConversationId")
    @DynamoDbSecondaryPartitionKey(indexNames = "OutboundConversationIdSecondaryIndex")
    public final void setOutboundConversationId(String outboundConversationId) {
        this.outboundConversationId = outboundConversationId;
    }

    @DynamoDbAttribute("TransferStatus")
    public final void setTransferStatus(String transferStatus) {
        this.transferStatus = transferStatus;
    }

    @DynamoDbAttribute("FailureCode")
    public final void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    @DynamoDbAttribute("FailureReason")
    public final void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @DynamoDbAttribute("CreatedAt")
    public final void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @DynamoDbAttribute("UpdatedAt")
    public final void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @DynamoDbAttribute("DeletedAt")
    public final void setDeletedAt(Integer deletedAt) {
        this.deletedAt = deletedAt;
    }
}