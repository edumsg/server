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

import com.fasterxml.jackson.core.JsonProcessingException;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;
import edumsg.activemq.subscriber;
import edumsg.concurrent.WorkerPool;
import edumsg.core.CommandsMap;
import edumsg.core.PostgresConnection;
import edumsg.core.config;
import edumsg.logger.MyLogger;
import edumsg.redis.DMCache;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQDestination;

import javax.jms.*;
import java.util.logging.Logger;

public class DMMain extends RunnableClasses implements MessageListener {
    private static final Logger LOGGER = Logger.getLogger(DMMain.class.getName());
    private static PostgresConnection db = new PostgresConnection();
    private static WorkerPool pool = new WorkerPool();
    private static edumsg.logger.MyLogger MyLogger = new MyLogger();
    private static int cur_instance;
    private static Consumer consumer;
    private static Consumer cons_ctrl;
    private static boolean run = true;

    public DMMain() {
    }

    public static void main(String[] args) throws Exception {
        // set the initial parameters for dm application
        db.initSource();
        CommandsMap.instantiate();
        DMCache.dmBgSave();
        // set the initial logger path for the micro-service in the local disk
        MyLogger.initialize(LOGGER, "C:\\Users\\ziads\\Desktop\\Bachelor\\logs");
        cur_instance = config.getInstance_num();

        // assign the consumers for all queues and topics that will serve the dm application
        consumer = new Consumer(new ActiveMQConfig("DM_" + cur_instance + ".INQUEUE"), "DM");
        cons_ctrl = new Consumer(new ActiveMQConfig("DM_" + cur_instance + "_CONTROLLER.INQUEUE"), "DM");
        new subscriber(new ActiveMQConfig("DM"), "DM");

    }

    public static void stop() throws JMSException {
        // to stop the app from listening to new messages we close the consumer and delete the queue
        consumer.getConsumer().close();
        Connection conn = consumer.getConn();
        ((ActiveMQConnection) conn).destroyDestination((ActiveMQDestination) consumer.getQueue());
        run = false;
    }

    public static void start() {
        // restart the app by create new queue
        consumer = new Consumer(new ActiveMQConfig("DM_" + cur_instance + ".INQUEUE"), "DM");
        run = true;

    }

    public static void exit() throws JMSException, JsonProcessingException {
        // send the response first then we close activemq conn before we peacefully exit the app
        controllerResponse.controllerSubmit("DM", cur_instance, "DM app shutdown successfully", "shut down", null, LOGGER);
        cons_ctrl.getConsumer().close();
        Connection conn = cons_ctrl.getConn();
        ((ActiveMQConnection) conn).destroyDestination((ActiveMQDestination) cons_ctrl.getQueue());
        cons_ctrl.getConn().close();
        System.exit(0);
    }

    public static boolean isRun() {
        return run;
    }

    // once any one of the queues or topics for the dm app receive a msg this method will be called.
    @Override
    public void onMessage(Message message) {
        try {
            String msgTxt = ((TextMessage) message).getText();
            if (message.getJMSDestination().toString().contains("topic")) {
                updateClass.setup(msgTxt);
            } else {
                if (message.getJMSDestination().toString().contains("CONTROLLER")) {
                    handleControllerMsg(msgTxt, message.getJMSCorrelationID(), "dm", LOGGER, pool, db, MyLogger, cur_instance);
                } else {
                    handleMsg(msgTxt, message.getJMSCorrelationID(), "dm", LOGGER, pool, cur_instance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
