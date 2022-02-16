package uk.nhs.prm.deduction.e2e.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExampleAssumedRoleArnTest {

    @Test
    public void shouldParseATargetRoleForAssumeRoleFromACurrentAssumedRoleExampleArn() {
        var currentAssumedRoleArn = "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788";

        var parsedRole = ExampleAssumedRoleArn.parse(currentAssumedRoleArn);

        assertThat(parsedRole.assumeRoleTargetArn()).isEqualTo("arn:aws:sts::123456789154:role/RepoAdmin");
    }

    @Test
    public void shouldParseAssumedRoleAccountNumberFromACurrentAssumedRoleExampleArn() {
        var currentAssumedRoleArn = "arn:aws:sts::123456789154:assumed-role/RepoAdmin/1644843111269401788";

        var parsedRole = ExampleAssumedRoleArn.parse(currentAssumedRoleArn);

        assertThat(parsedRole.accountNo()).isEqualTo("123456789154");
    }
}