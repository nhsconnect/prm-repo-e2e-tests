package uk.nhs.prm.deduction.e2e.gp2gp_mi.registration_started;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.nhs.prm.deduction.e2e.gp2gp_mi.CommonRequestFields;

@Data
@AllArgsConstructor
public class RegistrationStartedRequest extends CommonRequestFields {

    private Payload payload;

    public RegistrationStartedRequest() {
        super();
    }
}
