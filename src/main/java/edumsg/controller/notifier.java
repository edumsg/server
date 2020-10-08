package edumsg.controller;

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

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.concurrent.Callable;

import static java.lang.Long.parseLong;


public class notifier implements Callable<String> {

    private EduMsgControllerHandler controllerHandler;
    private String queueName;


    public notifier(EduMsgControllerHandler controllerHandler,
                         String queueName) {
        this.controllerHandler = controllerHandler;
        this.setQueueName(queueName);
    }

    @Override
    public String call() {
        try {

            ActiveMQConfig activeMQConfig = new ActiveMQConfig(getQueueName()
                    .toUpperCase() + "_CONTROLLER.OUTQUEUE");
            Consumer consumer = new Consumer(activeMQConfig, parseLong(controllerHandler.getCorrelationId()));
            // wait until the response sent from the micro-services
            Message message = consumer.getConsumer().receive();
            String msgTxt = ((TextMessage) message).getText();
            consumer.getConsumer().close();
            consumer.getSession().close();
            consumer.getConn().close();
            return msgTxt;

        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}

