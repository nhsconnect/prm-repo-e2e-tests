package uk.nhs.prm.e2etests.enumeration;

/**
 * @deprecated PRMT-4670 old Transfer Tracker DB logic - to be removed
 */
@Deprecated(since="2.0.0")
public enum OldTransferTrackerStatus {
    EHR_REQUEST_SENT("ACTION:EHR_REQUEST_SENT"),
    EHR_TRANSFER_TIMEOUT("ACTION:EHR_TRANSFER_TIMEOUT"),
    EHR_TRANSFER_FAILED("ACTION:EHR_TRANSFER_FAILED"),
    EHR_TRANSFER_TO_REPO_STARTED("ACTION:TRANSFER_TO_REPO_STARTED"),
    EHR_TRANSFER_TO_REPO_COMPLETE("ACTION:EHR_TRANSFER_TO_REPO_COMPLETE"),
    LARGE_EHR_CONTINUE_REQUEST_SENT("ACTION:LARGE_EHR_CONTINUE_REQUEST_SENT");

    public final String status;

    OldTransferTrackerStatus(String status) {
        this.status = status;
    }
}
