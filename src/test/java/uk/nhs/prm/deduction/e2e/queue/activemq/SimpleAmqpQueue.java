package uk.nhs.prm.deduction.e2e.queue.activemq;

import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.*;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import com.swiftmq.amqp.v100.generated.messaging.message_format.ApplicationProperties;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.types.AMQPString;
import com.swiftmq.amqp.v100.types.AMQPType;
import com.swiftmq.net.JSSESocketFactory;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import java.io.IOException;
import java.util.HashMap;

public class SimpleAmqpQueue {
    private final TestConfiguration config;

    public SimpleAmqpQueue(TestConfiguration config) {
        this.config = config;
    }

    public void sendMessage(String messageBody, String correlationId) {
        try {
            var map = new HashMap<AMQPType, AMQPType>();
            map.put(new AMQPString("correlation-id"), new AMQPString(correlationId));
            var properties = new ApplicationProperties(map);

            var msg = new AMQPMessage();
            msg.setApplicationProperties(properties);
            msg.setAmqpValue(new AmqpValue(new AMQPString(messageBody)));

            var p = createProducer();
            p.send(msg);
            p.close();
        }
        catch (AMQPException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: create producer once, not 1 per message sent
    private Producer createProducer() {
        var activeMqHostname = config.getAmqpEndpoint1();
        var ctx = new AMQPContext(AMQPContext.CLIENT);
        var connection = new Connection(ctx, activeMqHostname, 5671, config.getMqUserName(), config.getMqPassword());
        connection.setSocketFactory(new JSSESocketFactory());

        try {
            connection.connect();
            var session = connection.createSession(100, 100);
            return session.createProducer("inbound", QoS.AT_LEAST_ONCE);
        }
        catch (IOException | AMQPException | AuthenticationException | UnsupportedProtocolVersionException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}