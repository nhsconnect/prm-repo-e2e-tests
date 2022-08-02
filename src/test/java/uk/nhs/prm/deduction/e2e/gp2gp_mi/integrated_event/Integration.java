package uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Integration {
    private String integrationStatus;
    private String reason;
}
