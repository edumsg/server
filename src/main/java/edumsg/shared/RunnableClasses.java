package edumsg.shared;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.concurrent.WorkerPool;
import edumsg.core.Command;
import edumsg.core.CommandsMap;
import edumsg.logger.MyLogger;
import edumsg.redis.*;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import edumsg.core.PostgresConnection;


import javax.jms.JMSException;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edumsg.shared.controllerResponse.controllerHandleError;

public abstract class RunnableClasses {

    protected static void handleMsg(String msg, String correlationID, String subclass, Logger LOGGER, WorkerPool pool , int cur_instance) {
        JsonMapper json = new JsonMapper(msg);
        HashMap<String, String> map = null;
        try {
            map = json.deserialize();
            System.out.println("RunnableClasses Class :: " + map.toString());
            LOGGER.log(Level.SEVERE,LOGGER.getName());
        } catch (JsonParseException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (JsonMappingException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        }

        if (map != null && map.get("session_id") != null) {
            Jedis cache = null;
            switch (subclass.toLowerCase())
            {
                case "user": cache = UserCache.userCache;
                    break;
                case "tweet": cache = TweetsCache.tweetCache;
                    break;
                case "list": cache = ListCache.listCache;
                    break;
                case "dm": cache = DMCache.dmCache;
                    break;
            }

            String cachedEntry = cache.get(map.get("method") + ":" + map.get("session_id"));
            if (cachedEntry != null) {
                System.out.println("RunnableClasses Class :: Cached Entry: " + cachedEntry);
                JSONObject cachedEntryJson;
                try {
                    cachedEntryJson = new JSONObject(cachedEntry);
                    String dataStatus = cachedEntryJson.getString("cacheStatus");
                    if (dataStatus.equals("valid")) {
                        cachedEntryJson.remove("cacheStatus");
                        Producer p = new Producer(new ActiveMQConfig(
                                subclass.toUpperCase() +"_"+ cur_instance + ".OUTQUEUE"));
                        p.send(cachedEntryJson.toString(),
                                correlationID, LOGGER);
                        System.out.println("Sent from cache");
                        return;
                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

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

    // the method that handles the commands coming through the controller queues
    protected static void handleControllerMsg(String msg, String correlationID, String queue, Logger LOGGER
            , WorkerPool pool , PostgresConnection db , MyLogger MyLogger, int cur_instance) throws JsonProcessingException {
        JSONObject jsonCtrl = new JSONObject(msg);
        String cmd = jsonCtrl.getString("command");
        String parameters = jsonCtrl.getString("parameters");
        try {
            switch(cmd) {
                case "maxThreads":
                    pool.setMaxThreads(Integer.parseInt(parameters));
                    break;
                case "maxDBConnections":
                    String max = parameters;
                    db.setDbMaxConnections(max);
                    break;
                case"initDBConnections":
                    String init = parameters;
                    db.setDbInitConnections(init);
                    break;
                case"getVersion":
                    // this command has special handling method
                    break;
                case"logPath":
                    MyLogger.setLog_path(parameters);
                    break;
                case"start":
                    switch(queue.toUpperCase()){
                        case "USER" :
                            UserMain.start();
                            break;
                        case "DM" :
                            DMMain.start();
                            break;
                        case "TWEET" :
                            TweetMain.start();
                            break;
                        case "LIST" :
                            ListMain.start();
                            break;
                    }
                    break;
                case"stop":
                    switch(queue.toUpperCase()){
                        case "USER" :
                            UserMain.stop();
                            break;
                        case "DM" :
                            DMMain.stop();
                            break;
                        case "TWEET" :
                            TweetMain.stop();
                            break;
                        case "LIST" :
                            ListMain.stop();
                            break;
                    }
                    break;
                case"shutdown":
                    switch(queue.toUpperCase()){
                        case "USER" :
                            UserMain.exit();
                            break;
                        case "DM" :
                            DMMain.exit();
                            break;
                        case "TWEET" :
                            TweetMain.exit();
                            break;
                        case "LIST" :
                            ListMain.exit();
                            break;
                    }
                    break;
                default:
                    throw new JMSException("Wrong Command");
            }
            // check that the command applied successfully and format the response to be sent to the controller
            controllerResponse.checkCommand(cmd,parameters,queue,pool,db,MyLogger,correlationID , cur_instance , LOGGER);

        }catch(Exception e) {
            // in case an error arise send to to the controller server details about this error
            controllerHandleError(queue,cur_instance,cmd,e.getMessage(),correlationID,LOGGER);
        }

    }
}
