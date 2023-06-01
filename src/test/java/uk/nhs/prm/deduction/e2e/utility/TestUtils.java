package uk.nhs.prm.deduction.e2e.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.List;
import java.util.regex.Pattern;

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

    public static String getPayload(String gp2gpMessageBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(gp2gpMessageBody);
        if (jsonObject.has("payload") ) {
            return jsonObject.getString("payload");
        } else {
            return jsonObject.getJSONObject("request").getJSONObject("body").getString("payload");
        }
    }

}
