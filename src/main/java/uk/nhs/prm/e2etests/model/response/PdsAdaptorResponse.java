package uk.nhs.prm.e2etests.model.response;

import java.util.Objects;

public record PdsAdaptorResponse(Boolean isSuspended,
                                 String currentOdsCode,
                                 String managingOrganisation,
                                 String recordETag) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdsAdaptorResponse that = (PdsAdaptorResponse) o;
        return Objects.equals(isSuspended, that.isSuspended) && Objects.equals(currentOdsCode, that.currentOdsCode) && Objects.equals(managingOrganisation, that.managingOrganisation) && Objects.equals(recordETag, that.recordETag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSuspended, currentOdsCode, managingOrganisation, recordETag);
    }
}