package uk.nhs.prm.e2etests.model.request;

import java.util.Objects;

public record PdsAdaptorRequest(String previousGp,
                                String recordETag) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PdsAdaptorRequest that = (PdsAdaptorRequest) o;
        return Objects.equals(previousGp, that.previousGp) && Objects.equals(recordETag, that.recordETag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(previousGp, recordETag);
    }
}