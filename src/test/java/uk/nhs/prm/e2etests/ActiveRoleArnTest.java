package uk.nhs.prm.e2etests;

import uk.nhs.prm.e2etests.exception.AccountNumberParsingException;
import uk.nhs.prm.e2etests.exception.TargetArnParsingException;
import uk.nhs.prm.e2etests.configuration.ActiveRoleArn;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActiveRoleArnTest {
    private static final String EXAMPLE_ARN = "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788";
    private static final String INVALID_EXAMPLE_ARN = "invalid-arn";

    @Test
    void Given_ExampleArn_When_GetTargetArnIsCalled_Then_ExpectSuccessfulReturn() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(EXAMPLE_ARN);

        // Then
        assertThat(activeRoleArn.getTargetArn()).isEqualTo("arn:aws:sts::123456789154:role/RepoAdmin");
    }

    @Test
    void Given_ExampleArn_When_GetTargetArnIsCalled_Then_ExpectToThrowTargetArnParsingException() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(INVALID_EXAMPLE_ARN);

        // Then
        assertThrows(TargetArnParsingException.class, activeRoleArn::getTargetArn);
    }


    @Test
    void Given_ExampleArn_When_GetAccountNo_Then_ExpectSuccessfulReturn() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(EXAMPLE_ARN);

        // Then
        assertThat(activeRoleArn.getAccountNo()).isEqualTo("123456789154");
    }

    @Test
    void Given_ExampleArn_When_GetAccountNo_Then_ExpectToThrowAccountNumberParsingException() {
        // Given
        final ActiveRoleArn activeRoleArn = new ActiveRoleArn(INVALID_EXAMPLE_ARN);

        // Then
        assertThrows(AccountNumberParsingException.class, activeRoleArn::getAccountNo);
    }
}