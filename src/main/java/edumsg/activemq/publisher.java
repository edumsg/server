package edumsg.activemq;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.logging.Level;

public class publisher {
    ActiveMQConfig config;

    public publisher(ActiveMQConfig config) {
        this.config = config;
    }
    // publisher can post a message in a topic
    public void publish (String msg){
        MessageProducer publisher = null;
        Session session = null;
        Connection connection = null;
        try {
            connection = config.connect();
            config.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(config.getQueueName());
            publisher = session.createProducer(topic);
            TextMessage message = session.createTextMessage(msg);
            publisher.send(message);
            connection.close();
        } catch (JMSException e) {

        }
    }

}
