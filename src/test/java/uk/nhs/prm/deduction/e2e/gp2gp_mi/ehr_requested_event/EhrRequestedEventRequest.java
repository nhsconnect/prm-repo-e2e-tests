package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_requested_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class EhrRequestedEventRequest extends CommonRequestFields {

    private PayloadEhrRequested payload;

    public EhrRequestedEventRequest() {
        super();
    }
}
