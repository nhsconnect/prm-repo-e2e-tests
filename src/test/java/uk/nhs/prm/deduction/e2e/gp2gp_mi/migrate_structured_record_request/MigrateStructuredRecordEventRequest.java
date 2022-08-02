package uk.nhs.prm.deduction.e2e.gp2gp_mi.migrate_structured_record_request;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class MigrateStructuredRecordEventRequest extends CommonRequestFields {

    private PayloadMigrateStructuredRecordEvent payload;

    public MigrateStructuredRecordEventRequest() {
        super();
    }
}
