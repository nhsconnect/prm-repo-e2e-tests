package uk.nhs.prm.deduction.e2e.gp2gp_mi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CommonRequestFields {
    private String eventGeneratedDateTime;
    private String conversationId;
    private String transferEventDateTime;
    private String reportingPracticeOdsCode;
    private String reportingSystemSupplier;

    public CommonRequestFields() {
        this.eventGeneratedDateTime = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        this.conversationId = UUID.randomUUID().toString();
        this.transferEventDateTime = ZonedDateTime.now(ZoneOffset.ofHours(0)).toString();
        this.reportingPracticeOdsCode = "reporting";
        this.reportingSystemSupplier = "randomsupp";
    }
}
