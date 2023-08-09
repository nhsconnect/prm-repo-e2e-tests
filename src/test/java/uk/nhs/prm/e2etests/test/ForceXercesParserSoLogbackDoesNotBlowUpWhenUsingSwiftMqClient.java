package uk.nhs.prm.e2etests.test;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The Xerces parser more efficiently uses memory, preventing an OutOfMemoryError when attempting to
 * process very large volumes of data on the message queues that the swiftMQ client is attempting to hook into
 */
@Log4j2
public class ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        log.warn("Forcing Xerces parser.");
        System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }
}