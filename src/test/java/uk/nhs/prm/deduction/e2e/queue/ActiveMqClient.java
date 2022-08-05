package uk.nhs.prm.deduction.e2e.queue;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import javax.jms.*;
import java.nio.charset.StandardCharsets;

public class ActiveMqClient {
    TestConfiguration config;
    public ActiveMqClient(TestConfiguration testConfiguration) {
        this.config = testConfiguration;
    }

    String randomOption = "?randomize=false";

    private Session getSession() throws JMSException {
        String url =  String.format("failover:(%s,%s)%s", config.getActiveMqEndpoint0(),config.getActiveMqEndpoint1(), randomOption);
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
        connectionFactory.setUserName(config.getMqUserName());
        connectionFactory.setPassword(config.getMqPassword());
        final PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        pooledConnectionFactory.setMaxConnections(4);

        final Connection producerConnection = pooledConnectionFactory.createConnection();
        producerConnection.start();

        final Session producerSession = producerConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        return producerSession;
    }

    private MessageProducer getMessageProducer(Session producerSession, String queueName) throws JMSException {
        final Destination producerDestination = producerSession.createQueue(queueName);
        final MessageProducer producer = producerSession.createProducer(producerDestination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        return producer;
    }

    public void postBrokenMessageToAQueue(String queueName, String message) throws JMSException {
        final Session producerSession = getSession();
        final MessageProducer producer = getMessageProducer(producerSession, queueName);
        var producerMessage = producerSession.createTextMessage(message);

        producer.send(producerMessage);
        System.out.println("Message sent.");
    }

    public void postMessageToAQueue(String queueName, String message) throws JMSException {
        final Session producerSession = getSession();
        final MessageProducer producer = getMessageProducer(producerSession, queueName);

        var producerMessage = producerSession.createBytesMessage();
        var bytesArray = message.getBytes(StandardCharsets.UTF_8);
        producerMessage.writeBytes(bytesArray);
        producerMessage.reset();

        producer.send(producerMessage);
        System.out.println("Message sent.");
    }
}
