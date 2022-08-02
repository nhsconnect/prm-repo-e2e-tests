package uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.Registration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadErrorEvent {
    private Registration registration;
    private Error error;
}
