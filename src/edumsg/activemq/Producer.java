/*
EduMsg is made available under the OSI-approved MIT license.
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
*/

package edumsg.activemq;

import javax.jms.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Producer {
    ActiveMQConfig config;

    public Producer(ActiveMQConfig config) {
        this.config = config;
    }

    public void send(String msg, String correlationID, Logger logger) {
        MessageProducer producer = null;
        Session session = null;
        Connection conn = null;
        try {
            conn = config.connect();
            session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(config.getQueueName());
            producer = session.createProducer(destination);
            Message message = session.createTextMessage(msg);
            message.setJMSCorrelationID(correlationID);
            producer.send(message);
        } catch (JMSException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                } catch (JMSException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
            if (conn != null) {
                try {
                    config.disconnect(conn);
                } catch (JMSException e) {
                    logger.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }
}
