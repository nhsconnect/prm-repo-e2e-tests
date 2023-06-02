package uk.nhs.prm.deduction.e2e.utility;

public class LargeEhrTestFiles {
    public final String largeEhrCore;
    public final String largeEhrFragment1;
    public final String largeEhrFragment2;
    public final String ehrRequest;
    public final String continueRequest;

    public LargeEhrTestFiles(String largeEhrCore, String largeEhrFragment1, String largeEhrFragment2, String ehrRequest, String continueRequest) {
        this.largeEhrCore = largeEhrCore;
        this.largeEhrFragment1 = largeEhrFragment1;
        this.largeEhrFragment2 = largeEhrFragment2;
        this.ehrRequest = ehrRequest;
        this.continueRequest = continueRequest;
    }
}