package uk.nhs.prm.e2etests.utility;
public final class ValidationUtility {
    private ValidationUtility() { }

    public static final String UUID_REGEX
            = "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$";
    public static final String ODS_CODE_REGEX = "[A-Z]\\d{5}";
    public static final String NHS_NUMBER_REGEX = "9\\d{9}";
}
