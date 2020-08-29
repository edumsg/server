package edumsg.activemq;

import edumsg.shared.DMMain;
import edumsg.shared.ListMain;
import edumsg.shared.TweetMain;
import edumsg.shared.UserMain;

import javax.jms.*;

public class subscriber {
    ActiveMQConfig config;
    Connection conn;
    MessageConsumer consumer;
    Session session;
    public subscriber(ActiveMQConfig config , String key) {
        this.config = config;
        try {
            conn = config.connect();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(config.getQueueName());
            consumer = session.createConsumer(topic);
            switch (key){
                case "USER" :
                    consumer.setMessageListener(new UserMain());
                    break;
                case "TWEET" :
                    consumer.setMessageListener(new TweetMain());
                    break;
                case "DM" :
                    consumer.setMessageListener(new DMMain());
                    break;
                case "LIST" : consumer.setMessageListener(new ListMain());
                    break;
            }
            conn.start();
        } catch (JMSException e) {

        }
    }
}
