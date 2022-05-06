package uk.nhs.prm.deduction.e2e.queue;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import uk.nhs.prm.deduction.e2e.TestConfiguration;

import javax.jms.*;

public class ActiveMqClient {
    TestConfiguration config;
    public ActiveMqClient(TestConfiguration testConfiguration) {
        this.config = testConfiguration;
    }

    String randomOption = "?randomize=false";

    public void postAMessageToAQueue(String queueName, String message) throws JMSException {
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

        final Destination producerDestination = producerSession.createQueue(queueName);

        final MessageProducer producer = producerSession.createProducer(producerDestination);
        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

        TextMessage producerMessage = producerSession.createTextMessage(message);

        producer.send(producerMessage);
        System.out.println("Message sent.");
    }
}
