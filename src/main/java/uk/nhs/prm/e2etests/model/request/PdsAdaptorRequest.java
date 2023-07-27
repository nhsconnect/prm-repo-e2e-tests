package uk.nhs.prm.e2etests.model.request;

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
