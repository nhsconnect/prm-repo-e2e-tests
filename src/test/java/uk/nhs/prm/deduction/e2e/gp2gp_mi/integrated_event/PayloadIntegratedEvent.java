package uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.Registration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadIntegratedEvent {
    private Registration registration;
    private Integration integration;
}
