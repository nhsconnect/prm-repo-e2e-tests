package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.deduction.e2e.utility.Resources;

import java.util.UUID;

public class ContinueRequestMessage {
    private UUID conversationId;
    private UUID messageId;
    private String sourceGpOds;
    private String destinationGpOds;
    private String sourceGpAsid;
    private String destinationGpAsid;

    public ContinueRequestMessage(UUID conversationId, UUID messageId, String sourceGpOds, String destinationGpOds, String sourceGpAsid, String destinationGpAsid) {
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.sourceGpOds = sourceGpOds;
        this.destinationGpOds = destinationGpOds;
        this.sourceGpAsid = sourceGpAsid;
        this.destinationGpAsid = destinationGpAsid;
    }

    public String toJsonString() {
        return Resources.readTestResourceFile("COPC_IN000001UK01")
                .replaceAll("DBC31D30-F984-11ED-A4C4-956AA80C6B4E", conversationId())
                .replaceAll("DE304CA0-F984-11ED-808B-AC162D1F16F0", messageId())
                .replaceAll("B85002", sourceGpOds)
                .replaceAll("200000001613", sourceGpAsid)
                .replaceAll("M85019", destinationGpOds)
                .replaceAll("200000000149", destinationGpAsid);
    }

    public String conversationId() {
        return conversationId.toString().toUpperCase();
    }

    public String messageId() {
        return messageId.toString().toUpperCase();
    }
}