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

import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

public class Consumer {
	Logger lgr = Logger.getLogger(Consumer.class.getName());
	ActiveMQConfig config;
	Connection conn;

	public Consumer(ActiveMQConfig config) throws JMSException {
		this.config = config;
	}

	public MessageConsumer connect() throws JMSException {
		Connection conn = config.connect();
		this.conn = conn;
		Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

		Destination destination = session.createQueue(config.getQueueName());
		MessageConsumer consumer = session.createConsumer(destination);
		return consumer;
	}

	public void disconnect() throws JMSException {
		config.disconnect(conn);
	}
}