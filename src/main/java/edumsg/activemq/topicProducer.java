package edumsg.activemq;

import javax.jms.*;

public class topicProducer {
    ActiveMQConfig config;

    public topicProducer(ActiveMQConfig config) {
        this.config = config;
    }

    public void produce(String msg) {
        MessageProducer producer = null;
        Session session = null;
        Connection connection = null;
        try {
            connection = config.connect();
            config.start();
            session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);

            Topic topic = session.createTopic(config.getQueueName());

            producer = session.createProducer(topic);
            TextMessage message = session.createTextMessage(msg);


            producer.send(message);

            connection.close();

        } catch (JMSException e) {

        }
    }

}
