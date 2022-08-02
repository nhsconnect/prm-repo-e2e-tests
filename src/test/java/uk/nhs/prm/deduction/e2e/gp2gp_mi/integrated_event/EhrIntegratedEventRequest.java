package uk.nhs.prm.deduction.e2e.gp2gp_mi.integrated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class EhrIntegratedEventRequest extends CommonRequestFields {

    private PayloadIntegratedEvent payload;

    public EhrIntegratedEventRequest() {
        super();
    }
}
