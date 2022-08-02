package uk.nhs.prm.deduction.e2e.gp2gp_mi.migrate_structured_record_request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.Registration;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayloadMigrateStructuredRecordEvent {
    private Registration registration;
}
