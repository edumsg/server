package edumsg.activemq;

import javax.jms.*;

public class subscriber {
    ActiveMQConfig config;
    Connection conn;
    MessageConsumer consumer;
    Session session;

    // the constructor for crating a subscriber when we use a topic over the middle-ware connection
    public subscriber(ActiveMQConfig config, MessageListener listener) {
        this.config = config;
        try {
            conn = config.connect();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(config.getQueueName());
            consumer = session.createConsumer(topic);
            consumer.setMessageListener(listener);
            conn.start();
        } catch (JMSException e) {

        }
    }
}
