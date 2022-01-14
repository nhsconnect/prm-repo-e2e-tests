package uk.nhs.prm.deduction.e2e.suspensions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MofUpdateMessage {
    public static MofUpdateMessage parseMessage(String messageBody) throws JSONException {
        Map<String, String> fields = parseMofUpdateFields(messageBody);
        return new MofUpdateMessage(fields.get("nhsNumber"));
    }

    private String nhsNumber;

    public MofUpdateMessage(String nhsNumber) {
        this.nhsNumber = nhsNumber;
    }

    private static Map<String, String> parseMofUpdateFields(String responseBody) throws JSONException {
        Map<String, String> response = new HashMap<>();
        JSONObject jsonObject = new JSONObject(responseBody);
        response.put("nhsNumber",jsonObject.get("nhsNumber").toString());
        response.put("managingOrganisationOdsCode",jsonObject.get("managingOrganisationOdsCode").toString());

        return response;
    }

    public String nhsNumber() {
        return nhsNumber;
    }
}
