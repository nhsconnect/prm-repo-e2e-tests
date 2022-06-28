package uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_trace;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class PDSTraceEventRequest extends CommonRequestFields {

    private PayloadTrace payload;

    public PDSTraceEventRequest() {
        super();
    }
}
