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

package edumsg.shared;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
import org.json.JSONObject;

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;
import edumsg.activemq.Producer;
import edumsg.concurrent.WorkerPool;
import edumsg.core.Command;
import edumsg.core.CommandsMap;
import edumsg.core.PostgresConnection;
import edumsg.redis.EduMsgRedis;

public class DMMain extends RunnableClasses {
    private static final Logger LOGGER = Logger.getLogger(DMMain.class
            .getName());
    private static WorkerPool pool = new WorkerPool(10);
    private static boolean run = true;

    public static void main(String[] args) {
        PostgresConnection.initSource();
        CommandsMap.instantiate();
        try {
            Consumer c = new Consumer(new ActiveMQConfig("DM.INQUEUE"));
            MessageConsumer consumer = c.connect();

            while (run) {
                Message msg = consumer.receive();
                if (msg instanceof TextMessage) {
                    String msgTxt = ((TextMessage) msg).getText();
                    handleMsg(msgTxt, msg.getJMSCorrelationID(),"dm",LOGGER,pool);
                }
            }

            c.disconnect();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void shutdown() {
        run = false;
    }


}
