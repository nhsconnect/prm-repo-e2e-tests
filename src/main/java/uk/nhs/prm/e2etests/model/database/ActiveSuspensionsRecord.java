package uk.nhs.prm.e2etests.model.database;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

@Setter
@Builder
@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ActiveSuspensionsRecord {
    private static final String DEFAULT_TIMESTAMP = LocalDateTime.now() + "Z";
    private String nhsNumber;
    private String previousGp;
    @Builder.Default
    private String nemsLastUpdatedDate = DEFAULT_TIMESTAMP;

    // GETTERS
    @DynamoDbPartitionKey
    @DynamoDbAttribute("nhs_number")
    public String getNhsNumber() {
        return nhsNumber;
    }

    @DynamoDbAttribute("previous_gp")
    public String getPreviousGp() {
        return previousGp;
    }

    @DynamoDbAttribute("nems_last_updated_date")
    public String getNemsLastUpdatedDate() {
        return nemsLastUpdatedDate;
    }

    // EQUALS AND HASHCODE
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActiveSuspensionsRecord that = (ActiveSuspensionsRecord) o;
        return Objects.equals(nhsNumber, that.nhsNumber) && Objects.equals(previousGp, that.previousGp) && Objects.equals(nemsLastUpdatedDate, that.nemsLastUpdatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nhsNumber, previousGp, nemsLastUpdatedDate);
    }
}