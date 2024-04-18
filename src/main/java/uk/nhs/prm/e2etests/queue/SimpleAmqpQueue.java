package uk.nhs.prm.e2etests.queue;

import com.swiftmq.amqp.v100.client.*;
import com.swiftmq.amqp.v100.generated.messaging.message_format.ApplicationProperties;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.exception.GenericException;
import uk.nhs.prm.e2etests.model.AmqpEndpoint;
import uk.nhs.prm.e2etests.property.QueueProperties;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import org.springframework.stereotype.Component;
import com.swiftmq.amqp.v100.types.AMQPString;
import com.swiftmq.amqp.v100.types.AMQPType;
import com.swiftmq.net.JSSESocketFactory;
import com.swiftmq.amqp.AMQPContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Log4j2
@Component
public class SimpleAmqpQueue {

    private final QueueProperties queueProperties;
    private final String messageQueueUsername;
    private final String messageQueuePassword;
    private final Producer messageQueueProducer;

    @Autowired
    public SimpleAmqpQueue(QueueProperties queueProperties) {
        this.queueProperties = queueProperties;
        this.messageQueueUsername = queueProperties.getMqAppUsername();
        this.messageQueuePassword = queueProperties.getMqAppPassword();
        this.messageQueueProducer = createProducer();
    }

    public void sendMessage(String messageBody, String correlationId) {
        try {
            Map<AMQPType, AMQPType> map = new HashMap<>();
            map.put(new AMQPString("correlation-id"), new AMQPString(correlationId));
            ApplicationProperties properties = new ApplicationProperties(map);

            AMQPMessage message = new AMQPMessage();
            message.setApplicationProperties(properties);
            message.setAmqpValue(new AmqpValue(new AMQPString(messageBody)));

            messageQueueProducer.send(message);
        } catch (AMQPException | IOException exception) {
            log.error(exception.getMessage());
            throw new GenericException(this.getClass().getName(), exception.getMessage());
        }
    }

    public void sendUnexpectedMessage(String messageBody) {
        try {
            AMQPMessage message = new AMQPMessage();
            message.setAmqpValue(new AmqpValue(new AMQPString(messageBody)));
            messageQueueProducer.send(message);
        } catch (AMQPException exception) {
            log.error(exception.getMessage());
            throw new GenericException(this.getClass().getName(), exception.getMessage());
        }
    }

    public void close() {
        try {
            messageQueueProducer.close();
        } catch (AMQPException exception) {
            throw new GenericException(this.getClass().getName(), exception.getMessage());
        }
    }

    /**
     * Attempt to connect to the amqpEndpoint0. If this fails, attempt to connect to amqpEndpoint1.
     * @return The connected message producer if successful.
     * @throws GenericException if connection to either endpoint fails.
     */
    private Producer createProducer() {
        AmqpEndpoint[] amqpEndpoints = {
                queueProperties.getAmqpEndpoint0(),
                queueProperties.getAmqpEndpoint1()
        };

        for (AmqpEndpoint endpoint : amqpEndpoints) {
            try {
                Producer producer = connectToEndpoint(endpoint);
                if (producer != null) {
                    log.info("Connected to amqp endpoint: {}", endpoint.toString());
                    return producer;
                }
            } catch (Exception e) {
                log.error("Failed to connect to {}: {}", endpoint.toString(), e.getMessage());
            }
        }

        throw new GenericException(this.getClass().getName(), "Failed to connect to any endpoint, this can occur when you are not connected to the VPN. If you are connected to the VPN, try restart the AMQP broker.");
    }

    private Producer connectToEndpoint(AmqpEndpoint endpoint) throws IOException, AMQPException, AuthenticationException, UnsupportedProtocolVersionException {
        AMQPContext context = new AMQPContext(AMQPContext.CLIENT);
        Connection connection = new Connection(context, endpoint.getHostname(), endpoint.getPort(), messageQueueUsername, messageQueuePassword);
        connection.setSocketFactory(new JSSESocketFactory());

        connection.connect();
        Session session = connection.createSession(100, 100);
        return session.createProducer("inbound", QoS.AT_MOST_ONCE);
    }
}