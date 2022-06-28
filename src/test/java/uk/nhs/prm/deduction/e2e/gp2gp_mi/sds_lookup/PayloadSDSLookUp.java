package uk.nhs.prm.deduction.e2e.gp2gp_mi.sds_lookup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadSDSLookUp {
    private TransferCompatibilityStatus transferCompatibilityStatus;
}
