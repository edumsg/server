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

import edumsg.core.config;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

public class ActiveMQConfig {
    private String url = getUrl();
    private Connection connection;
    private String queueName;

    public ActiveMQConfig(String queueName) {
        this.queueName = queueName;
    }

    private String getUrl() {
        int PORT = 61616;
        try {
            String activeMQenv = (System.getenv("ACTIVEMQ_PORT"));
            if (activeMQenv != null) {
                PORT = Integer.parseInt(activeMQenv);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("No ActiveMq Env Set");
        }
        String host = config.getMain_host();
        return "failover://tcp://" + host + ":" + PORT;
    }

    public Connection connect() throws JMSException {
        System.err.println("ActiveMQConfig Class ::" + queueName);
        if (connection == null) {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connection = connectionFactory.createConnection();
        }
        return connection;
    }

    public void start() throws JMSException {
        connection.start();
    }

    public void disconnect(Connection connection) throws JMSException {
        connection.close();
    }

    public String getQueueName() {
        return this.queueName;
    }
}
