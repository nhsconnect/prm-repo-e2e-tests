package uk.nhs.prm.e2etests.model.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PdsAdaptorResponse {
    private Boolean isSuspended;
    private String currentOdsCode;
    private String managingOrganisation;
    private String recordETag;

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