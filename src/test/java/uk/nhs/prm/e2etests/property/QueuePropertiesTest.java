package uk.nhs.prm.e2etests.property;

import uk.nhs.prm.e2etests.exception.InvalidAmqpEndpointException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.nhs.prm.e2etests.ExampleAssumedRoleArn;
import uk.nhs.prm.e2etests.services.SsmService;
import uk.nhs.prm.e2etests.model.AmqpEndpoint;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueuePropertiesTest {
    // TEST CONSTANTS
    private static final String AMQP_ENDPOINT = "amqp+ssl://b-09f25472-2c58-4386-ad2c-675ce15efbd6-1.mq.eu-west-2.amazonaws.com:5671";
    private static final String INVALID_ENDPOINT = "this is not a valid endpoint";

    // MOCKING
    @Mock
    private SsmService ssmService;

    @Mock
    private NhsProperties nhsProperties;

    @Mock
    private ExampleAssumedRoleArn exampleAssumedRoleArn;

    @InjectMocks
    private QueueProperties queueProperties;

    @Test
    void Given_ValidAmqpEndpoint_When_GetAmqpEndpointIsCalled_Then_ExpectSuccessfulMatch() {
        // when
        when(ssmService.getSsmParameterValue(any()))
                .thenReturn(AMQP_ENDPOINT);

        AmqpEndpoint result = queueProperties.getAmqpEndpoint();

        // then
        assertEquals(AMQP_ENDPOINT, result.toString());
    }

    @Test
    void Given_InvalidAmqpEndpoint_When_GetAmqpEndpointIsCalled_Then_ExpectInvalidAmqpEndpointExceptionToBeThrown() {
        // when
        when(ssmService.getSsmParameterValue(any()))
                .thenReturn(INVALID_ENDPOINT);

        // then
        assertThrows(InvalidAmqpEndpointException.class, () -> queueProperties.getAmqpEndpoint());
    }
}