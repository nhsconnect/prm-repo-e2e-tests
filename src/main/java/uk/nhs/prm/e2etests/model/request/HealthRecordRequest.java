package uk.nhs.prm.e2etests.model.request;

import java.util.Objects;

public record HealthRecordRequest(String repositoryOdsCode,
                                  String repositoryAsid,
                                  String practiceOdsCode,
                                  String conversationId) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HealthRecordRequest that = (HealthRecordRequest) o;
        return Objects.equals(conversationId, that.conversationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
}