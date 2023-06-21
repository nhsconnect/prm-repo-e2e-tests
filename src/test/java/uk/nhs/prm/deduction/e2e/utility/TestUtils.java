package uk.nhs.prm.deduction.e2e.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.*;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TransferTrackerDbMessage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static uk.nhs.prm.deduction.e2e.nhs.NhsIdentityGenerator.randomNemsMessageId;

public final class TestUtils {
    private static final Logger LOGGER = LogManager.getLogger(TestUtils.class);
    private static final Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");

    public static boolean isValidUUID(String uuid) {
        try {
            return UUID_REGEX_PATTERN.matcher(uuid).matches();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Exception occurred while testing UUID validity: {}", exception.getMessage());
            return false;
        }
    }

    public static String getSmallEhrWithoutLinebreaks(String newConversationId, String newMessageId) {
        return Resources.readTestResourceFileFromEhrDirectory("small-ehr-without-linebreaks")
                .replaceAll("1632CD65-FD8F-4914-B62A-9763B50FC04A", newConversationId.toUpperCase())
                .replaceAll("0206C270-E9A0-11ED-808B-AC162D1F16F0", newMessageId);
    }

    public static String getEhrRequest(
            String newNhsNumber,
            String odsCodeOfRequestSender,
            String asidCodeOfRequestSender,
            String newConversationId)
    {
        return Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", newNhsNumber)
                .replaceAll("A91720", odsCodeOfRequestSender)
                .replaceAll("200000000631", asidCodeOfRequestSender)
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", newConversationId);
    }

    public static boolean excludeComparisons(Node node) {
        List<String> excludeList = List.of(
                "1.2.826.0.1285.0.1.10", // ODS code
                "1.2.826.0.1285.0.2.0.107" // ASID code
        );

        if (node.hasAttributes() && node.getAttributes().getNamedItem("root") != null) {
            String idRootValue = node.getAttributes().getNamedItem("root").getNodeValue();
            // return false to skip comparison in case when id root value itself is a message id
            if (isValidUUID(idRootValue)) {
                return false;
            }
            // return false to skip comparison when the type of compared value is in the excludedList
            return !(node.getNodeName().equals("id") && excludeList.contains(idRootValue));
        }

        // return false to skip comparison in case when the node name is "message-id"
        if (node.getNodeName().equals("message-id")) {
            return false;
        }

        return true;
    }

    public static void addRecordToTrackerDb(TrackerDb trackerDb, String inboundConversationId, String largeEhrCoreMessageId, String nhsNumberForTestPatient, String previousGpForTestPatient, String state) {
        String timeNow = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();

        trackerDb.save(new TransferTrackerDbMessage(
                inboundConversationId,
                largeEhrCoreMessageId,
                randomNemsMessageId(),
                nhsNumberForTestPatient,
                previousGpForTestPatient,
                state,
                timeNow,
                timeNow,
                timeNow
        ));
    }

    public static String getPayload(String gp2gpMessageBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(gp2gpMessageBody);
        if (jsonObject.has("payload") ) {
            return jsonObject.getString("payload");
        } else {
            return jsonObject.getJSONObject("request").getJSONObject("body").getString("payload");
        }
    }

    public static Optional<String> getPayloadOptional(String gp2gpMessageBody) {
        try {
            String payload = getPayload(gp2gpMessageBody);
            return Optional.of(payload);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static Diff comparePayloads(String message1, String message2) {
        return DiffBuilder.compare(message1).withTest(message2)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withNodeFilter(TestUtils::excludeComparisons)
                .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                        DifferenceEvaluators.downgradeDifferencesToEqual(ComparisonType.XML_STANDALONE)))
                .checkForSimilar().build();
    }

    public static LargeEhrTestFiles prepareTestFilesForLargeEhr(
            String inboundConversationId,
            String outboundConversationId,
            String largeEhrCoreMessageId,
            String fragment1MessageId,
            String fragment2MessageId,
            String newGpForTestPatient,
            String nhsNumberForTestPatient) {

        inboundConversationId = inboundConversationId.toUpperCase();
        outboundConversationId = outboundConversationId.toUpperCase();
        largeEhrCoreMessageId = largeEhrCoreMessageId.toUpperCase();
        fragment1MessageId = fragment1MessageId.toUpperCase();
        fragment2MessageId = fragment2MessageId.toUpperCase();

        String largeEhrCore = Resources.readTestResourceFileFromEhrDirectory("large-ehr-core")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId)
                .replaceAll("B8DC074D-C039-4FD2-8BBB-D4BFBBBF9AFA", largeEhrCoreMessageId)
                .replaceAll("3DBFC9EB-32FA-444F-B996-AB680D64148E", fragment1MessageId);

        String largeEhrFragment1 = Resources.readTestResourceFileFromEhrDirectory("large-ehr-fragment-1")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId)
                .replaceAll("3DBFC9EB-32FA-444F-B996-AB680D64148E", fragment1MessageId)
                .replaceAll("03CBFB18-0F7E-4BB6-B9EF-46AF564D3B9C", fragment2MessageId)
                .replaceAll("<Recipient>B85002</Recipient>", "<Recipient>" + newGpForTestPatient + "</Recipient>")
                .replaceAll("<From>N82668</From>", "<From>B85002</From>");

        String largeEhrFragment2 = Resources.readTestResourceFileFromEhrDirectory("large-ehr-fragment-2")
                .replaceAll("71118F7D-59CE-4552-B7AE-45A6801F4334", inboundConversationId.toUpperCase())
                .replaceAll("03CBFB18-0F7E-4BB6-B9EF-46AF564D3B9C", fragment2MessageId)
                .replaceAll("<Recipient>B85002</Recipient>", "<Recipient>" + newGpForTestPatient + "</Recipient>")
                .replaceAll("<From>N82668</From>", "<From>B85002</From>");

        String ehrRequest = Resources.readTestResourceFile("RCMR_IN010000UK05")
                .replaceAll("9692842304", nhsNumberForTestPatient)
                .replaceAll("A91720", newGpForTestPatient)
                .replaceAll("17a757f2-f4d2-444e-a246-9cb77bef7f22", outboundConversationId);

        String continueRequest = Resources.readTestResourceFile("COPC_IN000001UK01")
                .replaceAll("DBC31D30-F984-11ED-A4C4-956AA80C6B4E", outboundConversationId);

        return new LargeEhrTestFiles(largeEhrCore, largeEhrFragment1, largeEhrFragment2, ehrRequest, continueRequest);
    }

    public static Connection getRemoteConnection(TestConfiguration config) throws SQLException {
        return DriverManager.getConnection(config.getEhrOutPostgresJdbcUrl());
    }
}
