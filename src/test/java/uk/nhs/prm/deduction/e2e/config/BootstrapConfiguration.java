package uk.nhs.prm.deduction.e2e.config;

import software.amazon.awssdk.services.sts.StsClient;

import static java.lang.System.getenv;

public class BootstrapConfiguration {
    private static ExampleAssumedRoleArn exampleArn = determineExampleAssumedRoleArn();

    public static String assumeRoleTargetArn() {
        return exampleArn.assumeRoleTargetArn();
    }

    public static ExampleAssumedRoleArn exampleAssumeRoleArn() {
        return exampleArn;
    }

    private static ExampleAssumedRoleArn determineExampleAssumedRoleArn() {
        var exampleAssumedRoleArn = getenv("REQUIRED_ROLE_ARN");
        if (exampleAssumedRoleArn == null) {
            System.err.println("REQUIRED_ROLE_ARN not set, determining example assume role arn from current identity (expecting already assumed role)");
            exampleAssumedRoleArn = StsClient.create().getCallerIdentity().arn();
        }
        else {
            System.err.println("REQUIRED_ROLE_ARN is set and to be used to resolve target role and account for AWS auth");
        }
        return ExampleAssumedRoleArn.parse(exampleAssumedRoleArn);
    }
}
