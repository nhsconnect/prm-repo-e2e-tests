package uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DemographicTraceStatus {

    private String status ;
    private String reason ;
}
