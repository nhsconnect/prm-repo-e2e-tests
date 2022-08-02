package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnsupportedDataItem {

    String type;
    String uniqueIdentifier;
    String reason;
}
