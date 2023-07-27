package uk.nhs.prm.e2etests.queue.suspensions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SuspensionMessage {
    public static SuspensionMessage parseMessage(String messageBody) throws JSONException {
        Map<String, String> fields = parseSuspensionFields(messageBody);
        return new SuspensionMessage(fields.get("nhsNumber"));
    }

    private String nhsNumber;

    public SuspensionMessage(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    private static Map<String, String> parseSuspensionFields(String responseBody) throws JSONException {
        Map<String, String> response = new HashMap<>();
        JSONObject jsonObject = new JSONObject(responseBody);
        response.put("nhsNumber",jsonObject.get("nhsNumber").toString());
        response.put("eventType",jsonObject.get("eventType").toString());
        response.put("previousOdsCode",jsonObject.get("previousOdsCode").toString());
        response.put("lastUpdated",jsonObject.get("lastUpdated").toString());
        return response;
    }

    public String nhsNumber() {
        return nhsNumber;
    }
}
