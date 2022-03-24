package uk.nhs.prm.deduction.e2e.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResolutionMessage {
    String nemsMessageId;
    String status;
}
