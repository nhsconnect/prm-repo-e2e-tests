package uk.nhs.prm.e2etests.configuration;

public class ActiveRoleArn {
    private final String roleArn;

    public ActiveRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getTargetArn() {
        String role = roleArn.replace("assumed-role", "role");
        return role.substring(0, role.lastIndexOf("/"));
    }

    public String getAccountNo() {
        String accountAndRoleId = roleArn.replace("arn:aws:sts::", "");
        return accountAndRoleId.split(":")[0];
    }
}