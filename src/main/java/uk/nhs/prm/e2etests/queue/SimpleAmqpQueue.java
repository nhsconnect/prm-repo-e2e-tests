package uk.nhs.prm.e2etests.queue;

import com.swiftmq.amqp.v100.client.*;
import com.swiftmq.amqp.v100.generated.messaging.message_format.ApplicationProperties;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.exception.GenericException;
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

            AMQPMessage msg = new AMQPMessage();
            msg.setApplicationProperties(properties);
            msg.setAmqpValue(new AmqpValue(new AMQPString(messageBody)));

            messageQueueProducer.send(msg);
        } catch (AMQPException | IOException exception) {
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

    private Producer createProducer() {
        String activeMqHostname = queueProperties.getAmqpEndpoint().getHostname();
        int activeMqPort = queueProperties.getAmqpEndpoint().getPort();
        AMQPContext context = new AMQPContext(AMQPContext.CLIENT);
        Connection connection = new Connection(context, activeMqHostname, activeMqPort, messageQueueUsername, messageQueuePassword);
        connection.setSocketFactory(new JSSESocketFactory());

        try {
            connection.connect();
            Session session = connection.createSession(100, 100);
            return session.createProducer("inbound", QoS.AT_MOST_ONCE);
        } catch (IOException | AMQPException | AuthenticationException | UnsupportedProtocolVersionException exception) {
            log.error(exception.getMessage());
            throw new GenericException(this.getClass().getName(), exception.getMessage());
        }
    }
}