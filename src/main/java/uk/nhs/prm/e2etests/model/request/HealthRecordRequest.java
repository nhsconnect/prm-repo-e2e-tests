package uk.nhs.prm.e2etests.model.request;

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
