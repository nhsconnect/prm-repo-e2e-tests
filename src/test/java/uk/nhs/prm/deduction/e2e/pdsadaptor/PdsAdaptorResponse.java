package uk.nhs.prm.deduction.e2e.pdsadaptor;

import lombok.Data;

@Data
public class PdsAdaptorResponse {
    private Boolean isSuspended;
    private String currentOdsCode;
    private String managingOrganisation;
    private String recordETag;
}
