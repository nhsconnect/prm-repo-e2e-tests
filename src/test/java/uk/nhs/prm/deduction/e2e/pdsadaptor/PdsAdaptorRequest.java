package uk.nhs.prm.deduction.e2e.pdsadaptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PdsAdaptorRequest {
    private String previousGp;
    private String recordETag;
}
