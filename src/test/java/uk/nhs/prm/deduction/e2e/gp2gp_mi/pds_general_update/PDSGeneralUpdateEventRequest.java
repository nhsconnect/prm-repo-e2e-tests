package uk.nhs.prm.deduction.e2e.gp2gp_mi.pds_general_update;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class PDSGeneralUpdateEventRequest extends CommonRequestFields {

    private PdsGeneralUpdate payload;

    public PDSGeneralUpdateEventRequest() {
        super();
    }
}
