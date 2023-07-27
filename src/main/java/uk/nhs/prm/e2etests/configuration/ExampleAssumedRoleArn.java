package uk.nhs.prm.e2etests.configuration;

// TODO: PRMT-3488 Example? What does that mean!?
public class ExampleAssumedRoleArn {
    private final String awsArn;

    public ExampleAssumedRoleArn(String awsArn) {
        this.awsArn = awsArn;
    }

    public String getTargetArn() {
        String role = awsArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }

    public String getAccountNo() {
        String accountAndRoleId = awsArn.replace("arn:aws:sts::", "");
        return accountAndRoleId.split(":")[0];
    }
}
