package uk.nhs.prm.e2etests.queue;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * The Xerces parser more efficiently uses memory, preventing an OutOfMemoryError when attempting to
 * process very large volumes of data on the message queues that the swiftMQ client is attempting to hook into
 */
public class ForceXercesParserSoLogbackDoesNotBlowUpWhenUsingSwiftMqClient implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        System.out.println("FORCING XERCES PARSER");
        System.setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
    }
}