package uk.nhs.prm.e2etests.utility;

import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.ElementSelectors;
import org.json.JSONException;
import org.xmlunit.diff.Diff;
import org.json.JSONObject;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.List;

import static uk.nhs.prm.e2etests.utility.ValidationUtility.isValidUUID;

public final class XmlComparisonUtility {
    private XmlComparisonUtility() { }

    public static boolean excludeComparisons(Node node) {
        List<String> oidExcludeList = List.of(
                "1.2.826.0.1285.0.1.10", // oid for ODS code
                "1.2.826.0.1285.0.2.0.107" // oid for ASID code
        );

        if (node.hasAttributes() && node.getAttributes().getNamedItem("root") != null) {
            String idRootValue = node.getAttributes().getNamedItem("root").getNodeValue();
            // return false to skip comparison in case when id root value itself is a message id
            if (isValidUUID(idRootValue)) {
                return false;
            }
            // return false to skip comparison when the type of compared value is in the excludedList
            return !(node.getNodeName().equals("id") && oidExcludeList.contains(idRootValue));
        }

        // return false to skip comparison in case when the node name is "message-id"
        return !node.getNodeName().equals("message-id");
    }

    public static String getPayload(String gp2gpMessageBody) throws JSONException {
        JSONObject jsonObject = new JSONObject(gp2gpMessageBody);
        String jsonKey = "payload";

        if (jsonObject.has(jsonKey) ) {
            return jsonObject.getString(jsonKey);
        } else {
            return jsonObject.getJSONObject("request").getJSONObject("body").getString(jsonKey);
        }
    }

    public static Optional<String> getPayloadOptional(String gp2gpMessageBody) {
        try {
            String payload = getPayload(gp2gpMessageBody);
            return Optional.of(payload);
        } catch (JSONException exception) {
            return Optional.empty();
        }
    }

    public static Diff comparePayloads(String message1, String message2) {
        return DiffBuilder.compare(message1).withTest(message2)
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                .withNodeFilter(XmlComparisonUtility::excludeComparisons)
                .withDifferenceEvaluator(DifferenceEvaluators.chain(DifferenceEvaluators.Default,
                        DifferenceEvaluators.downgradeDifferencesToEqual(ComparisonType.XML_STANDALONE)))
                .checkForSimilar().build();
    }
}