package uk.nhs.prm.e2etests.utility;

import uk.nhs.prm.e2etests.model.nems.NemsEventMessage;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


public class NemsEventFactory {

    public static NemsEventMessage createNemsEventFromTemplate(String nemsEventFilename, String nhsNumber, String nemsMessageId,String timestamp) {
        return createNemsEventFromTemplate(nemsEventFilename, nhsNumber, nemsMessageId, "B85612", timestamp);
    }

    public static NemsEventMessage createNemsEventFromTemplate(String nemsEventFilename, String nhsNumber, String nemsMessageId, String previousGP, String timestamp) {
        return new NemsEventMessage(nemsMessageId, Resources.readTestResourceFileFromNemsEventTemplatesDirectory(nemsEventFilename)
                .replaceAll("__NHS_NUMBER__", nhsNumber)
                .replaceAll("__NEMS_MESSAGE_ID__", nemsMessageId)
                .replaceAll("__PREVIOUS_GP_ODS_CODE__", previousGP)
                .replaceAll("__LAST-UPDATED__",timestamp)
        );
    }

    public static Map<String, NemsEventMessage> getDLQNemsEventMessages() throws IOException {
        Timestamp now = Timestamp.from(Instant.now());
        NemsEventMessage nhsNumberVerification = createNemsEventFromTemplate("nhs-number-verification-fail.xml", NhsIdentityGenerator.randomNhsNumber(), NhsIdentityGenerator.randomNemsMessageId(),now.toString());
        NemsEventMessage uriForManagingOrganizationNotPresent = createNemsEventFromTemplate("no-reference-for-uri-for-managing-organization.xml", NhsIdentityGenerator.randomNhsNumber(), NhsIdentityGenerator.randomNemsMessageId(),now.toString());
        Map<String, NemsEventMessage> messages = new HashMap<>();
        messages.put("nhsNumberVerification", nhsNumberVerification);
        messages.put("uriForManagingOrganizationNotPresent", uriForManagingOrganizationNotPresent);
        return messages;
    }
}
