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

package edumsg.netty;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;

public class NettyNotifier extends Thread {

	private EduMsgNettyServerHandler serverHandler;
	private String responseBody;
	private String queueName;

	public NettyNotifier(EduMsgNettyServerHandler serverHandler,
			String queueName) {
		this.serverHandler = serverHandler;
		this.setQueueName(queueName);
	}

	@Override
	public void run() {
			try {
				Consumer c = new Consumer(new ActiveMQConfig(getQueueName()
						.toUpperCase() + ".OUTQUEUE"));
				MessageConsumer consumer = c.connect();
				consumer.setMessageListener(new MessageListener() {
					@Override
					public void onMessage(Message message) {
						try {
							String msgTxt = ((TextMessage) message).getText();
							System.out.println("thread" + getResponseBody());
							setResponseBody(msgTxt);
							serverHandler.setResponseBody(msgTxt);
							sleep(1000);
							synchronized (serverHandler) {
								serverHandler.notify();
								System.out
										.println("netty notified by notifier");
							}
						} catch (JMSException | InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	public EduMsgNettyServerHandler getServerHandler() {
		return serverHandler;
	}

	public void setServerHandler(EduMsgNettyServerHandler serverHandler) {
		this.serverHandler = serverHandler;
	}

	public String getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(String responseBody) {
		this.responseBody = responseBody;
	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
}
