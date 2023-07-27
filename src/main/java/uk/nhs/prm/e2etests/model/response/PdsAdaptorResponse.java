package uk.nhs.prm.e2etests.model.response;

import lombok.Data;

@Data
public class PdsAdaptorResponse {
    private Boolean isSuspended;
    private String currentOdsCode;
    private String managingOrganisation;
    private String recordETag;
}
