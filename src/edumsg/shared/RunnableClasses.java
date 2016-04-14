package edumsg.shared;

import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.concurrent.WorkerPool;
import edumsg.core.Command;
import edumsg.core.CommandsMap;
import edumsg.redis.Cache;
import edumsg.redis.EduMsgRedis;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import javax.jms.JMSException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RunnableClasses {

    protected static void handleMsg(String msg, String correlationID, String subclass, Logger LOGGER, WorkerPool pool)
            throws IOException, JMSException {
        JsonMapper json = new JsonMapper(msg);
        HashMap<String, String> map = null;
        try {
            map = json.deserialize();
            System.out.println(map.toString());
        } catch (JsonParseException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (JsonMappingException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        }

//        if (map != null) {
//            if (map.get("method").equals("login"))
//            {
//                if (!Cache.userCache.exists("username"))
//                    Cache.userCache.set("username", map.get("username"));
//                else
//                {
//                    if (!Cache.userCache.get("username").equals(map.get("username")))
//                    {
//                        Cache.userCache.flushAll();
//                        Cache.tweetCache.flushAll();
//                        Cache.listCache.flushAll();
//                        Cache.dmCache.flushAll();
//                    }
//                }
//            }
//            Jedis cache = null;
//            switch (subclass.toLowerCase())
//            {
//                case "user": cache = Cache.userCache;
//                    break;
//                case "tweet": cache = Cache.tweetCache;
//                    break;
//                case "list": cache = Cache.listCache;
//                    break;
//                case "dm": cache = Cache.dmCache;
//                    break;
//            }
//            String cachedEntry = cache.get(map.get("method"));
//            if (cachedEntry != null) {
//                System.out.println(cachedEntry);
//                JSONObject cachedEntryJson;
//                try {
//                    cachedEntryJson = new JSONObject(cachedEntry);
//                    String dataStatus = cachedEntryJson.getString("cacheStatus");
//                    if (dataStatus.equals("valid")) {
//                        cachedEntryJson.remove("cacheStatus");
//                        Producer p = new Producer(new ActiveMQConfig(
//                                subclass.toUpperCase() + ".OUTQUEUE"));
//                        p.send(cachedEntryJson.toString(),
//                                correlationID);
//                        System.out.println("Sent from cache");
//                        return;
//                    }
//                } catch (JSONException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (JMSException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }

        if (map != null) {
            map.put("app", subclass.toLowerCase());
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
                        pool.execute(c);
                    } catch (InstantiationException | IllegalAccessException e) {
                        LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }
    }

}
