package uk.nhs.prm.e2etests;

import org.junit.jupiter.api.Test;
import uk.nhs.prm.e2etests.configuration.ActiveRoleArn;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveRoleArnTest {
    private static final String EXAMPLE_ARN = "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788";

    // TODO: PRMT-3488 Test name no longer matches logic!
    @Test
    void shouldParseATargetRoleForAssumeRoleFromACurrentAssumedRoleExampleArn() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(EXAMPLE_ARN);

        // Then
        assertThat(activeRoleArn.getTargetArn()).isEqualTo("arn:aws:sts::123456789154:role/RepoAdmin");
    }

    // TODO: PRMT-3488 Test name no longer matches logic!
    @Test
    void shouldParseAssumedRoleAccountNumberFromACurrentAssumedRoleExampleArn() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(EXAMPLE_ARN);

        // While
        assertThat(activeRoleArn.getAccountNo()).isEqualTo("123456789154");
    }
}