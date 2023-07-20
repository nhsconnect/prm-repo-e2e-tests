package uk.nhs.prm.e2etests;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleAssumedRoleArnTest {
    @Test
    void shouldParseATargetRoleForAssumeRoleFromACurrentAssumedRoleExampleArn() {
        // Given
        final ExampleAssumedRoleArn exampleAssumedRoleArn = new ExampleAssumedRoleArn(
    "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788"
        );

        // Then
        assertThat(exampleAssumedRoleArn.getTargetArn()).isEqualTo("arn:aws:sts::123456789154:role/RepoAdmin");
    }

    @Test
    void shouldParseAssumedRoleAccountNumberFromACurrentAssumedRoleExampleArn() {
        var currentAssumedRoleArn = "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788";

        var parsedRole = ExampleAssumedRoleArn.parse(currentAssumedRoleArn);

        assertThat(parsedRole.accountNo()).isEqualTo("123456789154");
    }
}