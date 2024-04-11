package uk.nhs.prm.e2etests.property;

import lombok.extern.log4j.Log4j2;

import static uk.nhs.prm.e2etests.utility.TestDataUtility.*;

@Log4j2
public final class TestConstants {
    public static String testName;
    public static String inboundConversationId;
    public static String outboundConversationId;
    public static String messageId;
    public static String largeEhrCoreMessageId;
    public static String fragment1MessageId;
    public static String fragment2MessageId;
    public static String nemsMessageId;
    public static String nhsNumber;
    public static String senderOdsCode;
    public static String recipientOdsCode;
    public static String asidCode;

    private TestConstants() {}

    public static void generateTestConstants(String testName) {
        TestConstants.testName = testName;

        inboundConversationId = randomUppercaseUuidAsString();
        outboundConversationId = randomUppercaseUuidAsString();
        messageId = randomUppercaseUuidAsString();
        largeEhrCoreMessageId = randomUppercaseUuidAsString();
        fragment1MessageId = randomUppercaseUuidAsString();
        fragment2MessageId = randomUppercaseUuidAsString();
        nemsMessageId = randomNemsMessageId();
        nhsNumber = randomNhsNumber();
        senderOdsCode = "M85019";
        recipientOdsCode = "M85020";
        asidCode = "200000000149";

        log.info("==========TEST CONSTANTS==========");
        log.info("testName: " + testName);
        log.info("inboundConversationId: " + inboundConversationId);
        log.info("outboundConversationId: " + outboundConversationId);
        log.info("messageId: " + messageId);
        log.info("largeEhrCoreMessageId: " + largeEhrCoreMessageId);
        log.info("fragment1MessageId: " + fragment1MessageId);
        log.info("fragment2MessageId: " + fragment2MessageId);
        log.info("nhsNumber: " + nhsNumber);
        log.info("senderOdsCode: " + senderOdsCode);
        log.info("recipientOdsCode: " + recipientOdsCode);
        log.info("asidCode: " + asidCode);
        log.info("==========TEST CONSTANTS==========");
    }
}
