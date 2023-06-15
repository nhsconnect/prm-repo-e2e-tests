package uk.nhs.prm.deduction.e2e.models;

import uk.nhs.prm.deduction.e2e.utility.Resources;

import java.util.UUID;

import static uk.nhs.prm.deduction.e2e.utility.TestUtils.getUuidAsUpperCaseString;

public class EhrRequestMessage {
    private final String nhsNumber;
    private final String sourceGpOds;
    private final String destinationGpOds;
    private final String sourceGpAsid;
    private final String destinationGpAsid;
    private final UUID conversationId;
    private final UUID messageId;

    public EhrRequestMessage(String nhsNumber, String sourceGpOds, String destinationGpOds, String sourceGpAsid, String destinationGpAsid, UUID conversationId, UUID messageId) {
        this.nhsNumber = nhsNumber;
        this.sourceGpOds = sourceGpOds;
        this.destinationGpOds = destinationGpOds;
        this.sourceGpAsid = sourceGpAsid;
        this.destinationGpAsid = destinationGpAsid;
        this.conversationId = conversationId;
        this.messageId = messageId;
    }

    public String toJsonString() {
        return Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", nhsNumber)
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", conversationId())
                .replaceAll("C445C720-B0EB-4E36-AF8A-48CD1CA5DE4F", messageId())
                .replaceAll("B86041", sourceGpOds)
                .replaceAll("200000001161", sourceGpAsid)
                .replaceAll("A91720", destinationGpOds)
                .replaceAll("200000000631", destinationGpAsid);
    }

    public String conversationId() {
        return getUuidAsUpperCaseString(conversationId);
    }

    public String messageId() {
        return getUuidAsUpperCaseString(messageId);
    }
}
