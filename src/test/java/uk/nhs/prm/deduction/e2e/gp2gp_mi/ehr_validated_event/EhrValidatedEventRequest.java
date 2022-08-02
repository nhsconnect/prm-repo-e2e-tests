package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_validated_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class EhrValidatedEventRequest extends CommonRequestFields {

    private PayloadEhrValidatedEvent payload;

    public EhrValidatedEventRequest() {
        super();
    }
}
