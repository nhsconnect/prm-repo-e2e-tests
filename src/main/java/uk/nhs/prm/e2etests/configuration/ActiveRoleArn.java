package uk.nhs.prm.e2etests.configuration;

import uk.nhs.prm.e2etests.exception.AccountNumberParsingException;
import uk.nhs.prm.e2etests.exception.TargetArnParsingException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActiveRoleArn {
    private static final String ACCOUNT_NUMBER_REGEX = "arn:aws:sts::(\\d{12}):.*";
    private final String roleArn;

    public ActiveRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getTargetArn() {
        try {
            String role = roleArn.replace("assumed-role", "role");
            return role.substring(0, role.lastIndexOf("/"));
        } catch (IndexOutOfBoundsException exception) {
            throw new TargetArnParsingException(exception.getMessage());
        }
    }

    public String getAccountNo() {
        final Pattern awsAccountNumberPattern = Pattern.compile(ACCOUNT_NUMBER_REGEX);
        final Matcher match = awsAccountNumberPattern.matcher(this.roleArn);

        if (match.find()) return match.group(1);
        else throw new AccountNumberParsingException();
    }
}