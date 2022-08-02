package uk.nhs.prm.deduction.e2e.gp2gp_mi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Registration {
    private String requestingPracticeOdsCode;
    private String sendingPracticeOdsCode;
}
