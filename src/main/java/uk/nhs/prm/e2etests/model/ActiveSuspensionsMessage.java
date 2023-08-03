package uk.nhs.prm.e2etests.model;

import java.util.Objects;

public record ActiveSuspensionsMessage(String nhsNumber,
                                       String previousOdsCode,
                                       String nemsLastUpdatedDate) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveSuspensionsMessage that = (ActiveSuspensionsMessage) o;
        return Objects.equals(nhsNumber, that.nhsNumber) && Objects.equals(previousOdsCode, that.previousOdsCode) && Objects.equals(nemsLastUpdatedDate, that.nemsLastUpdatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nhsNumber, previousOdsCode, nemsLastUpdatedDate);
    }
}