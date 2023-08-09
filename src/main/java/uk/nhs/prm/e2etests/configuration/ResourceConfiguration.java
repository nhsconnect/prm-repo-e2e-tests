package uk.nhs.prm.e2etests.configuration;

import lombok.SneakyThrows;
import uk.nhs.prm.e2etests.exception.InvalidNhsEnvironmentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.prm.e2etests.model.NhsNumberTestData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import uk.nhs.prm.e2etests.property.NhsProperties;
import org.springframework.core.io.Resource;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import uk.nhs.prm.e2etests.utility.NhsIdentityUtility;

import java.util.stream.Stream;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Configuration
public class ResourceConfiguration {
    private static final String SYNTHETIC_PATIENT_NUMBER_PREFIX_DEV = "969";

    @Value("classpath:static/dev-nhs-numbers.json")
    private Resource devNhsNumbers;

    @Value("classpath:static/preprod-nhs-numbers.json")
    private Resource preProdNhsNumbers;

    @Value("${test.numberOfPerfNhsNumbers}")
    private int numberOfPerfNhsNumbers;

    private final String nhsEnvironment;

    private final Gson gson;

    @Autowired
    public ResourceConfiguration(NhsProperties nhsProperties) {
        nhsEnvironment = nhsProperties.getNhsEnvironment();
        gson = new Gson();
    }

    @Bean @Lazy @SneakyThrows
    public NhsNumberTestData nhsNumbers() {
        List<String> nhsNumbers = switch (nhsEnvironment) {
            case "dev" -> mapJsonToStringList(devNhsNumbers.getContentAsString(UTF_8));
            case "preprod" -> mapJsonToStringList(preProdNhsNumbers.getContentAsString(UTF_8));
            case "perf" -> generateRandomNhsNumbers(this.numberOfPerfNhsNumbers);
            default -> throw new InvalidNhsEnvironmentException();
        };

        return new NhsNumberTestData(nhsNumbers);
    }

    private List<String> mapJsonToStringList(String json) {
        return gson.fromJson(json, new TypeToken<List<String>>() {}.getType());
    }

    private List<String> generateRandomNhsNumbers(int count) {
        return Stream.generate(NhsIdentityUtility::randomNhsNumber)
                .limit(count)
                .toList();
    }
}
