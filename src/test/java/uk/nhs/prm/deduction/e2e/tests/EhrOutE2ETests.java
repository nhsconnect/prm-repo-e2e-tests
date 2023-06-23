package uk.nhs.prm.deduction.e2e.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.nhs.prm.deduction.e2e.TestConfiguration;
import uk.nhs.prm.deduction.e2e.client.RoleAssumingAwsConfigurationClient;
import uk.nhs.prm.deduction.e2e.ehr_transfer.*;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AssumeRoleCredentialsProviderFactory;
import uk.nhs.prm.deduction.e2e.performance.awsauth.AutoRefreshingRoleAssumingSqsClient;
import uk.nhs.prm.deduction.e2e.queue.BasicSqsClient;
import uk.nhs.prm.deduction.e2e.queue.SqsMessage;
import uk.nhs.prm.deduction.e2e.queue.ThinlyWrappedSqsClient;
import uk.nhs.prm.deduction.e2e.queue.activemq.SimpleAmqpQueue;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TrackerDb;
import uk.nhs.prm.deduction.e2e.transfer_tracker_db.TransferTrackerDbClient;
import uk.nhs.prm.deduction.e2e.utility.LargeEhrTestFiles;
import uk.nhs.prm.deduction.e2e.utility.Resources;
import uk.nhs.prm.deduction.e2e.utility.TestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.nhs.prm.deduction.e2e.ehroutdb.tables.Acknowledgements.ACKNOWLEDGEMENTS;
import static uk.nhs.prm.deduction.e2e.utility.TestUtils.*;

@SpringBootTest(classes = {
        EhrOutE2ETests.class,
        TestConfiguration.class,
        ThinlyWrappedSqsClient.class,
        BasicSqsClient.class,
        AssumeRoleCredentialsProviderFactory.class,
        AutoRefreshingRoleAssumingSqsClient.class,
        Resources.class,
        TrackerDb.class,
        Gp2gpMessengerQueue.class,
        TransferTrackerDbClient.class,
        RoleAssumingAwsConfigurationClient.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EhrOutE2ETests {
    private static final Logger LOGGER = LogManager.getLogger(EhrOutE2ETests.class);
    private final TrackerDb trackerDb;
    private final Gp2gpMessengerQueue gp2gpMessengerQueue;
    private final TestConfiguration config;

    @Autowired
    public EhrOutE2ETests(
            TrackerDb trackerDb,
            Gp2gpMessengerQueue gp2gpMessengerQueue,
            TestConfiguration config
    ) {
        this.trackerDb = trackerDb;
        this.gp2gpMessengerQueue = gp2gpMessengerQueue;
        this.config = config;
    }

    @BeforeAll
    void init() {
        gp2gpMessengerQueue.deleteAllMessages();
    }

    @Test
    void shouldPutASmallEHROntoRepoAndSendEHRToMHSOutboundWhenReceivingRequestFromGP() {
        // Given
        String inboundConversationId = UUID.randomUUID().toString();
        String smallEhrMessageId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        String nhsNumberForTestPatient = "9727018440";
        String previousGpForTestPatient = "M85019";
        String asidCodeForTestPatient = "200000000149";
        LOGGER.info(" ===============  outboundConversationId: {}", outboundConversationId);

        SimpleAmqpQueue inboundQueueFromMhs = new SimpleAmqpQueue(config);

        String smallEhr = getSmallEhrWithoutLinebreaks(inboundConversationId.toUpperCase(), smallEhrMessageId);
        String ehrRequest = getEhrRequest(nhsNumberForTestPatient, previousGpForTestPatient, asidCodeForTestPatient, outboundConversationId);

        // When
        // change transfer db status to ACTION:EHR_REQUEST_SENT before putting on inbound queue
        // Put the patient into inboundQueueFromMhs as a UK05 message

        addRecordToTrackerDb(trackerDb, inboundConversationId, "", nhsNumberForTestPatient, previousGpForTestPatient, "ACTION:EHR_REQUEST_SENT");
        inboundQueueFromMhs.sendMessage(smallEhr, inboundConversationId);

        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        String status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
        LOGGER.info("tracker db status: {}", status);

        // Send an EHR request from inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(ehrRequest, outboundConversationId);

        // Then
        SqsMessage gp2gpMessage = gp2gpMessengerQueue.getMessageContaining(outboundConversationId);

        String gp2gpMessengerPayload = getPayloadOptional(gp2gpMessage.body()).orElseThrow();
        String smallEhrPayload = getPayloadOptional(smallEhr).orElseThrow();
        LOGGER.info("Payload from gp2gpMessenger: {}", gp2gpMessengerPayload);
        LOGGER.info("Payload from smallEhr: {}", smallEhrPayload);

        assertThat(gp2gpMessage).isNotNull();
        assertTrue(gp2gpMessage.contains("RCMR_IN030000UK06"));
        assertTrue(gp2gpMessengerPayload.contains(nhsNumberForTestPatient));

        // clear up the queue after test in order not to interfere with other tests
        gp2gpMessengerQueue.deleteMessage(gp2gpMessage);
    }

    @Test
    void shouldPutALargeEHROntoRepoAndSendEHRToMHSOutboundWhenReceivingRequestFromGP() {
        // given
        String inboundConversationId = UUID.randomUUID().toString();
        String outboundConversationId = UUID.randomUUID().toString();
        LOGGER.info(" ===============  outboundConversationId: {}", outboundConversationId);

        String largeEhrCoreMessageId = UUID.randomUUID().toString();
        String fragment1MessageId = UUID.randomUUID().toString();
        String fragment2MessageId = UUID.randomUUID().toString();

        String nhsNumberForTestPatient = "9727018157";
        String previousGpForTestPatient = "N82668";
        String newGpForTestPatient = "M85019";

        SimpleAmqpQueue inboundQueueFromMhs = new SimpleAmqpQueue(config);

        LargeEhrTestFiles largeEhrTestFiles = TestUtils.prepareTestFilesForLargeEhr(
                inboundConversationId,
                outboundConversationId,
                largeEhrCoreMessageId,
                fragment1MessageId,
                fragment2MessageId,
                newGpForTestPatient,
                nhsNumberForTestPatient
        );

        String largeEhrCore = largeEhrTestFiles.largeEhrCore;
        String largeEhrFragment1 = largeEhrTestFiles.largeEhrFragment1;
        String largeEhrFragment2 = largeEhrTestFiles.largeEhrFragment2;
        String ehrRequest = largeEhrTestFiles.ehrRequest;
        String continueRequest = largeEhrTestFiles.continueRequest;

        addRecordToTrackerDb(trackerDb, inboundConversationId, largeEhrCoreMessageId, nhsNumberForTestPatient, previousGpForTestPatient, "ACTION:EHR_REQUEST_SENT");

        // when
        inboundQueueFromMhs.sendMessage(largeEhrCore, inboundConversationId);
        LOGGER.info("conversationIdExists: {}",trackerDb.conversationIdExists(inboundConversationId));
        String status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:LARGE_EHR_CONTINUE_REQUEST_SENT");
        LOGGER.info("tracker db status: {}", status);

        LOGGER.info("fragment 1 message id: {}", fragment1MessageId);
        LOGGER.info("fragment 2 message id: {}", fragment2MessageId);

        inboundQueueFromMhs.sendMessage(largeEhrFragment1, inboundConversationId);
        inboundQueueFromMhs.sendMessage(largeEhrFragment2, inboundConversationId);

        status = trackerDb.waitForStatusMatching(inboundConversationId, "ACTION:EHR_TRANSFER_TO_REPO_COMPLETE");
        LOGGER.info("tracker db status: {}", status);

        // Put a EHR request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(ehrRequest, outboundConversationId);

        // Then
        // assert gp2gpMessenger queue got a message of UK06
        SqsMessage gp2gpMessageUK06 = gp2gpMessengerQueue.getMessageContaining(outboundConversationId);

        assertThat(gp2gpMessageUK06).isNotNull();
        assertThat(gp2gpMessageUK06.contains("RCMR_IN030000UK06")).isTrue();

        // Put a continue request to inboundQueueFromMhs
        inboundQueueFromMhs.sendMessage(continueRequest, outboundConversationId);

        // get all message fragments from gp2gp-messenger observability queue and compare with inbound fragments
        List<SqsMessage> allFragments = gp2gpMessengerQueue.getAllMessageContaining("COPC_IN000001UK01");

        assertThat(allFragments.size()).isEqualTo(2);

        allFragments.forEach(fragment -> assertThat(fragment.contains(outboundConversationId)).isTrue());

        // clear up the queue after test in order not to interfere with other tests
        gp2gpMessengerQueue.deleteMessage(gp2gpMessageUK06);
        allFragments.forEach(gp2gpMessengerQueue::deleteMessage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MCCI_IN010000UK13_POSITIVE", "MCCI_IN010000UK13_NEGATIVE"})
    void shouldPutAcksOnMHSInboundAndUpdateEhrOutDbStatus(String acknowledgement) {
        // given
        String ackMessageId = UUID.randomUUID().toString();
        String ackConversationId = UUID.randomUUID().toString();
        String ackMessage = Resources.readTestResourceFile("acknowledgement/" + acknowledgement).replaceAll(
            "__MESSAGE_ID__", ackMessageId
        );

        SimpleAmqpQueue inboundQueueFromMhs = new SimpleAmqpQueue(config);

        // when
        inboundQueueFromMhs.sendMessage(ackMessage, ackConversationId);

        try(Connection connection = getRemoteConnection(config)) {
            connection.setReadOnly(true); // we've got no reason to write to the database for these E2E tests
            DSLContext context = DSL.using(connection, SQLDialect.POSTGRES);

            Result<Record> result =  await().atMost(30, TimeUnit.SECONDS)
                    .with().pollInterval(2, TimeUnit.SECONDS)
                    .until(() -> getRecords(ackMessageId, context), Result::isNotEmpty);

            // then
            assertTrue(result.isNotEmpty());

            String typeCode = result.getValue(0, ACKNOWLEDGEMENTS.ACKNOWLEDGEMENT_TYPE_CODE);
            String expectedTypeCode = acknowledgement.contains("POSITIVE") ? "AA" : "AR";
            assertThat(typeCode).isEqualTo(expectedTypeCode);

            LOGGER.info("The acknowledgement typeCode of {} is {}.", ackMessageId, typeCode);
        } catch (DataAccessException | SQLException exception) {
            LOGGER.error(exception.getMessage());
            fail();
        }
    }

    private static Result<Record> getRecords(String ackMessageId, DSLContext context) {
        return context
             .select()
             .from(ACKNOWLEDGEMENTS)
             .where(ACKNOWLEDGEMENTS.MESSAGE_ID.eq(UUID.fromString(ackMessageId)))
             .fetch();
    }
}
