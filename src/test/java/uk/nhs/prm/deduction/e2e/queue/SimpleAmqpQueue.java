package uk.nhs.prm.deduction.e2e.queue;

import com.swiftmq.amqp.AMQPContext;
import com.swiftmq.amqp.v100.client.*;
import com.swiftmq.amqp.v100.generated.messaging.message_format.AmqpValue;
import com.swiftmq.amqp.v100.generated.messaging.message_format.ApplicationProperties;
import com.swiftmq.amqp.v100.messaging.AMQPMessage;
import com.swiftmq.amqp.v100.types.AMQPString;
import com.swiftmq.amqp.v100.types.AMQPType;
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
        var randomOption = "?randomize=false";
        var activeMqHostname =  String.format("failover:(%s,%s)%s", config.getActiveMqEndpoint0(),config.getActiveMqEndpoint1(), randomOption);

        var ctx = new AMQPContext(AMQPContext.CLIENT);
        var connection = new Connection(ctx, activeMqHostname, 5672, config.getMqUserName(), config.getMqPassword());
        try {
            connection.connect();
            var session = connection.createSession(100, 100);
            return session.createProducer("inbound", QoS.AT_LEAST_ONCE);
        }
        catch (IOException | AMQPException | AuthenticationException | UnsupportedProtocolVersionException e) {
            throw new RuntimeException(e);
        }
    }
}