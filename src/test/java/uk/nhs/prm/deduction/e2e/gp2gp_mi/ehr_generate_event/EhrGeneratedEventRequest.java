package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_generate_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class EhrGeneratedEventRequest extends CommonRequestFields {

    private PayloadEhrGeneratedEvent payload;

    public EhrGeneratedEventRequest() {
        super();
    }
}
