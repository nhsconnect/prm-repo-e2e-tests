package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Placeholder {

    String placeholderId;
    String attachmentId;
    String generatedBy;
    int reason;
    String originalMimeType;
}
