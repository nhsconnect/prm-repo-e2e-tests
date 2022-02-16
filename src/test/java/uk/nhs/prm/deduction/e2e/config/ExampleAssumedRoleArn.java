package uk.nhs.prm.deduction.e2e.config;

public class ExampleAssumedRoleArn {
    private String exampleArn;

    public static ExampleAssumedRoleArn parse(String exampleArn) {
        return new ExampleAssumedRoleArn(exampleArn);
    }

    public ExampleAssumedRoleArn(String exampleArn) {
        this.exampleArn = exampleArn;
    }

    public String assumeRoleTargetArn() {
        String role = exampleArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }

    public String accountNo() {
        String accountAndRoleId = exampleArn.replace("arn:aws:sts::", "");
        return accountAndRoleId.split(":")[0];
    }
}
