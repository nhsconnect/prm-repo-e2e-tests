package uk.nhs.prm.e2etests.model.response;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PdsAdaptorResponse {
    private Boolean isSuspended;
    private String currentOdsCode;
    private String managingOrganisation;
    private String recordETag;
}