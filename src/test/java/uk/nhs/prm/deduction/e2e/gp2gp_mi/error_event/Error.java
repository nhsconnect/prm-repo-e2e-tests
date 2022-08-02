package uk.nhs.prm.deduction.e2e.gp2gp_mi.error_event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Error {
    private String errorCode;
    private String errorDescription;
}
