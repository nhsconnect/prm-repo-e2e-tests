package uk.nhs.prm.deduction.e2e.utility;

import org.springframework.stereotype.Component;
import uk.nhs.prm.deduction.e2e.nems.NemsEventMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Helper {

    public String randomNhsNumber() {
        return "9691234567" ;
    }

    public NemsEventMessage createNemsEventFromTemplate(String nemsEventFilename, String nhsNumber) throws IOException {
        return new NemsEventMessage(readTestResourceFile(nemsEventFilename).replaceAll("__NHS_NUMBER__", nhsNumber));
    }

    public String readTestResourceFile(String nemsEvent) throws IOException {
        File file = new File(String.format("src/test/resources/%s", nemsEvent));
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder sb = new StringBuilder();

        while((line=br.readLine())!= null){
            sb.append(line.trim());
        }
        return sb.toString();
    }

    public Map<String,NemsEventMessage> getDLQNemsEventMessages() throws IOException {
        NemsEventMessage nhsNumberVerification = createNemsEventFromTemplate("nhs-number-verification-fail.xml", randomNhsNumber());
        NemsEventMessage nhsNumberFieldNotPresent = createNemsEventFromTemplate("nhs-number-field-not-present.xml", randomNhsNumber());
        NemsEventMessage nhsNumberVerificationFieldNotPresent = createNemsEventFromTemplate("nhs-number-verification-field-not-present.xml", randomNhsNumber());
        NemsEventMessage episodeOfCareFieldNotPresent = createNemsEventFromTemplate("no-finished-episode-of-care.xml", randomNhsNumber());
        NemsEventMessage managingOrganizationFieldNotPresent = createNemsEventFromTemplate("no-managing-organization.xml", randomNhsNumber());
        NemsEventMessage odsCodeForFinishedPractiseNotPresent = createNemsEventFromTemplate("no-ods-code-for-finished-practise.xml", randomNhsNumber());
        NemsEventMessage odsCodeIdentifierForManagingOrganizationNotPresent = createNemsEventFromTemplate("no-ods-code-identifier-for-managing-organization.xml", randomNhsNumber());
        NemsEventMessage referenceForManagingOrganizationNotPresent = createNemsEventFromTemplate("no-reference-for-managing-organization.xml", randomNhsNumber());
        NemsEventMessage uriForManagingOrganizationNotPresent = createNemsEventFromTemplate("no-reference-for-uri-for-managing-organization.xml", randomNhsNumber());
        Map<String,NemsEventMessage> messages = new HashMap<>();
        messages.put("nhsNumberVerification",nhsNumberVerification);//p
        messages.put("nhsNumberFieldNotPresent",nhsNumberFieldNotPresent);//p
        messages.put("nhsNumberVerificationFieldNotPresent",nhsNumberVerificationFieldNotPresent);
        messages.put("episodeOfCareFieldNotPresent",episodeOfCareFieldNotPresent);//p
        messages.put("managingOrganizationFieldNotPresent",managingOrganizationFieldNotPresent);//p
        messages.put("odsCodeForFinishedPractiseNotPresent",odsCodeForFinishedPractiseNotPresent);
        messages.put("odsCodeIdentifierForManagingOrganizationNotPresent",odsCodeIdentifierForManagingOrganizationNotPresent);//p
        messages.put("referenceForManagingOrganizationNotPresent",referenceForManagingOrganizationNotPresent);
        messages.put("uriForManagingOrganizationNotPresent",uriForManagingOrganizationNotPresent);
        return messages;
    }
}
