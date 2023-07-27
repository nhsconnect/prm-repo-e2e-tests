package uk.nhs.prm.e2etests.property;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SyntheticPatientProperties {

    @Value("${nhs.syntheticPatient.nhsNumber.patientWithCurrentGp.dev}")
    private String syntheticPatientWithCurrentGpDev;

    @Value("${nhs.syntheticPatient.nhsNumber.patientWithCurrentGp.preProd}")
    private String syntheticPatientWithCurrentGpPreProd;

    @Value("${nhs.syntheticPatient.nhsNumber.patientWithoutGp.dev}")
    private String syntheticPatientWithoutGpDev;

    @Value("${nhs.syntheticPatient.nhsNumber.patientWithoutGp.preProd}")
    private String syntheticPatientWithoutGpPreProd;

    @Value("${nhs.syntheticPatient.nhsNumber.deceasedPatient.dev}")
    private String syntheticPatientDeceasedDev;

    @Value("${nhs.syntheticPatient.nhsNumber.deceasedPatient.preProd}")
    private String syntheticPatientDeceasedPreProd;

    @Value("${nhs.syntheticPatient.nhsNumber.syntheticPatientInPreProd}")
    private String syntheticPatientInPreProd;

    @Value("${nhs.syntheticPatient.nhsNumber.NonSyntheticPatientWithoutGp}")
    private String NonSyntheticPatientWithoutGp;

    private String nhsEnvironment;

    @Autowired
    public SyntheticPatientProperties(NhsProperties nhsProperties) {
        nhsEnvironment = nhsProperties.getNhsEnvironment();
    }

    public String getPatientWithCurrentGp() {
        return nhsEnvironment.equals("dev")
                ? syntheticPatientWithCurrentGpDev
                : syntheticPatientWithCurrentGpPreProd;
    }

    public String getPatientWithoutGp() {
        return nhsEnvironment.equals("dev")
                ? syntheticPatientWithoutGpDev
                : syntheticPatientWithoutGpPreProd;
    }

    public String getDeceasedPatient() {
        return nhsEnvironment.equals("dev")
                ? syntheticPatientDeceasedDev
                : syntheticPatientDeceasedPreProd;
    }

    public String getSyntheticPatientInPreProd() {
        return syntheticPatientInPreProd;
    }

    public String getNonSyntheticPatientWithoutGp() {
        return NonSyntheticPatientWithoutGp;
    }
}
