package uk.nhs.prm.e2etests.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class NHSConfiguration {

    @Value("${nhs.environment:#{'dev'}")
    private String nhsEnvironment;

}
