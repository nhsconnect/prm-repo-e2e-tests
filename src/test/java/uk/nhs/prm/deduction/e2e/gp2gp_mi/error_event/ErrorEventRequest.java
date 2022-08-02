package uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class ErrorEventRequest extends CommonRequestFields {

    private PayloadErrorEvent payload;

    public ErrorEventRequest() {
        super();
    }
}
