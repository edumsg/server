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

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;
import edumsg.concurrent.WorkerPool;
import edumsg.core.CommandsMap;
import edumsg.core.PostgresConnection;
import edumsg.redis.Cache;
import edumsg.redis.UserCache;

import javax.jms.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserMain extends RunnableClasses{
    private static final Logger LOGGER = Logger.getLogger(UserMain.class.getName());
    private static WorkerPool pool = new WorkerPool();
    private static boolean run = true;

    public static void main(String[] args) throws IOException {

        PostgresConnection.initSource();
        CommandsMap.instantiate();
        UserCache.userBgSave();
        Consumer c = null;
        try {
            c = new Consumer(new ActiveMQConfig("USER.INQUEUE"));

            while (run) {
                Message msg = c.receive();
                if (msg == null)
                {
                    throw new JMSException("Error receiving message from ActiveMQ");
                }
                if (msg instanceof TextMessage) {
                    String msgTxt = ((TextMessage) msg).getText();
                    handleMsg(msgTxt, msg.getJMSCorrelationID(),"user",LOGGER,pool);
                }
            }
        }
        catch (JMSException e)
        {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        finally {
            if (c != null)
            {
                MessageConsumer mc;
                if ((mc = c.getConsumer()) != null)
                {
                    try {
                        mc.close();
                    } catch (JMSException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                Session s;
                if ((s = c.getSession()) != null)
                {
                    try {
                        s.close();
                    } catch (JMSException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
                Connection conn;
                if ((conn = c.getConn()) != null)
                {
                    try {
                        conn.close();
                    } catch (JMSException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
    }

    public static void shutdown() {
        run = false;
    }


}
