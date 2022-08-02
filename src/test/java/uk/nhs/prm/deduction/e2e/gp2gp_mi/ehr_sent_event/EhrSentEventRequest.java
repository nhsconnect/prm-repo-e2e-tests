package uk.nhs.prm.deduction.e2e.gp2gp_mi.ehr_sent_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class EhrSentEventRequest extends CommonRequestFields {

    private PayloadEhrSent payload;

    public EhrSentEventRequest() {
        super();
    }
}
