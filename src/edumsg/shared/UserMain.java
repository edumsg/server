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
import edumsg.activemq.Producer;
import edumsg.concurrent.WorkerPool;
import edumsg.database.Command;
import edumsg.database.CommandsMap;
import edumsg.database.PostgresConnection;
import edumsg.redis.EduMsgRedis;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserMain {
    private static final Logger LOGGER = Logger.getLogger(UserMain.class
            .getName());
    private static WorkerPool pool = new WorkerPool(10);
    private static boolean run = true;

    public static void main(String[] args) throws JsonParseException,
            JsonMappingException, IOException {
        PostgresConnection.initSource();
        CommandsMap.instantiate();
        try {
            Consumer c = new Consumer(new ActiveMQConfig("USER.INQUEUE"));
            MessageConsumer consumer = c.connect();

            while (run) {
                Message msg = consumer.receive();
                if (msg instanceof TextMessage) {
                    String msgTxt = ((TextMessage) msg).getText();
                    handleMsg(msgTxt, msg.getJMSCorrelationID());
                }
            }

            c.disconnect();
        } catch (JMSException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public static void shutdown() {
        run = false;
    }

    private static void handleMsg(String msg, String correlationID)
            throws JsonParseException, JsonMappingException, IOException,
            JMSException {
        JsonMapper json = new JsonMapper(msg);
        HashMap<String, String> map = null;
        try {
            map = json.deserialize();
        } catch (JsonParseException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (JsonMappingException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        }

        if (map != null) {
            String cachedEntry = EduMsgRedis.redisCache.get(map.get("method"));
            if (cachedEntry != null) {
                System.out.println(cachedEntry);
                JSONObject cachedEntryJson;
                try {
                    cachedEntryJson = new JSONObject(cachedEntry);
                    String dataStatus = cachedEntryJson.getString("cacheStatus");
                    if (dataStatus.equals("valid")) {
                        Producer p = new Producer(new ActiveMQConfig(
                                "USER.OUTQUEUE"));
                        p.send(cachedEntryJson.get("response").toString(),
                                correlationID);
                        System.out.println("sent from cache");
                        return;
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        if (map != null) {
            map.put("app", "user");
            if (map.containsKey("method")) {
                map.put("correlation_id", correlationID);
                Class<?> cmdClass = CommandsMap.queryClass(map.get("method"));
                if (cmdClass == null) {
                    LOGGER.log(Level.SEVERE,
                            "Invalid Request. Class \"" + map.get("method")
                                    + "\" Not Found");
                } else {
                    try {
                        Command c = (Command) cmdClass.newInstance();
                        c.setMap(map);
                        pool.execute((Runnable) c);
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
    }
}
