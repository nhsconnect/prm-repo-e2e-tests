package uk.nhs.prm.e2etests.queue;

import com.swiftmq.amqp.v100.generated.messaging.message_format.ApplicationProperties;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import com.swiftmq.amqp.v100.client.UnsupportedProtocolVersionException;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.prm.e2etests.property.QueueProperties;
import com.swiftmq.amqp.v100.client.AuthenticationException;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.client.AMQPException;
import org.springframework.stereotype.Component;
import com.swiftmq.amqp.v100.client.Connection;
import com.swiftmq.amqp.v100.types.AMQPString;
import com.swiftmq.amqp.v100.client.Producer;
import com.swiftmq.amqp.v100.types.AMQPType;
import com.swiftmq.net.JSSESocketFactory;
import com.swiftmq.amqp.v100.client.QoS;
import com.swiftmq.amqp.AMQPContext;

import java.io.IOException;
import java.util.HashMap;

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
            var map = new HashMap<AMQPType, AMQPType>();
            map.put(new AMQPString("correlation-id"), new AMQPString(correlationId));
            var properties = new ApplicationProperties(map);

            var msg = new AMQPMessage();
            msg.setApplicationProperties(properties);
            msg.setAmqpValue(new AmqpValue(new AMQPString(messageBody)));

            messageQueueProducer.send(msg);
        }
        catch (AMQPException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            messageQueueProducer.close();
        }
        catch (AMQPException e) {
            throw new RuntimeException(e);
        }
    }

    private Producer createProducer() {
        String activeMqHostname = queueProperties.getAmqpEndpoint().getHostname();
        int activeMqPort = queueProperties.getAmqpEndpoint().getPort();
        var context = new AMQPContext(AMQPContext.CLIENT);
        var connection = new Connection(context, activeMqHostname, activeMqPort, messageQueueUsername, messageQueuePassword);
        connection.setSocketFactory(new JSSESocketFactory());

        try {
            connection.connect();
            var session = connection.createSession(100, 100);
            return session.createProducer("inbound", QoS.AT_MOST_ONCE);
        }
        catch (IOException | AMQPException | AuthenticationException | UnsupportedProtocolVersionException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}