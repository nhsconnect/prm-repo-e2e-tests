package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ehr {
    Long ehrTotalSizeBytes;
    Long ehrStructuredSizeBytes;
    Placeholder[] placeholder;
    Attachment[] attachment;
    Degrade[] degrade;
}
