package uk.nhs.prm.deduction.e2e.services.gp2gp_messenger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HealthRecordRequest {
    String repositoryOdsCode;
    String repositoryAsid;
    String practiceOdsCode;
    String conversationId;

}
