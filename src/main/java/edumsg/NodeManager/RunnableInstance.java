package edumsg.NodeManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Consumer;
import edumsg.activemq.Producer;
import edumsg.activemq.subscriber;
import edumsg.concurrent.WorkerPool;
import edumsg.core.Command;
import edumsg.core.CommandsMap;
import edumsg.core.PostgresConnection;
import edumsg.logger.MyLogger;
import edumsg.redis.Cache;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.command.ActiveMQDestination;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunnableInstance implements Runnable, MessageListener {
    private final Logger LOGGER = Logger.getLogger(RunnableInstance.class.getName());
    private final int cur_instance;
    private WorkerPool pool = new WorkerPool();
    private PostgresConnection db = new PostgresConnection();
    private edumsg.logger.MyLogger MyLogger = new MyLogger();
    private Consumer consumer;
    private Consumer cons_ctrl;
    private boolean run = true;
    private String app;
    private Cache cache;

    public RunnableInstance(String app, int current_instance) {
        this.app = app;
        this.cache = Main.cacheMap.get(app.toLowerCase());
        this.cur_instance = current_instance;
    }

    // send the response of command execution process to the controller
    public void controllerSubmit(String response, String cmd, String correlationID) throws JsonProcessingException {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app + "_" + cur_instance);
        node.put("msg", response);
        node.put("command", cmd);
        node.put("status", "ok");
        node.put("code", "200");

        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + "_" + cur_instance + "_CONTROLLER.OUTQUEUE"));
        p.send(mapper.writeValueAsString(node), correlationID, LOGGER);
    }

    // format and send a response in case of an error arise
    public void controllerHandleError(String method, String errorMsg,
                                      String correlationID) throws JsonProcessingException {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app + "_" + cur_instance);
        node.put("method", method);
        node.put("status", "Bad Request");
        node.put("code", "400");
        node.put("message", errorMsg);

        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + "_" + cur_instance + "_CONTROLLER.OUTQUEUE"));
        p.send(mapper.writeValueAsString(node), correlationID, LOGGER);
    }

    // the method that handles the commands coming through the controller queues
    protected void handleControllerMsg(String msg, String correlationID) throws JsonProcessingException {
        JSONObject jsonCtrl = new JSONObject(msg);
        String cmd = jsonCtrl.getString("command");
        String parameters = jsonCtrl.getString("parameters");
        try {
            switch (cmd) {
                case "maxThreads":
                    pool.setMaxThreads(Integer.parseInt(parameters));
                    break;
                case "maxDBConnections":
                    String max = parameters;
                    db.setDbMaxConnections(max);
                    break;
                case "initDBConnections":
                    String init = parameters;
                    db.setDbInitConnections(init);
                    break;
                case "getVersion":
                    // this command has special handling method
                    break;
                case "logPath":
                    MyLogger.setLog_path(parameters);
                    break;
                case "start":
                    this.start();
                    break;
                case "stop":
                    this.stop();
                    break;
                case "shutdown":
                    this.exit();
                    break;
                default:
                    throw new JMSException("Wrong Command");
            }
            // check that the command applied successfully and format the response to be sent to the controller
            this.checkCommand(cmd, parameters, correlationID);

        } catch (Exception e) {
            // in case an error arise send to to the controller server details about this error
            controllerHandleError(cmd, e.getMessage(), correlationID);
        }

    }

    // check that the command applied successfully and format the response to be sent to the controller
    protected void checkCommand(String cmd, String parameters, String correlationID) throws JMSException, JsonProcessingException {
        double version;
        String response = null;
        String error = null;

        switch (cmd) {
            case "maxThreads":
                if (Integer.parseInt(parameters) == pool.getMaxThreads()) {
                    response = " {Max Threads Updated Successfully }";
                } else {
                    error = "{failed to update max threads}";
                }
                break;
            case "maxDBConnections":
                if (parameters.equals(db.getDbMaxConnections())) {
                    response = " {Max DB Connections Updated Successfully}";
                } else {
                    error = "{failed to update max DB connections}";
                }
                break;
            case "initDBConnections":
                if (parameters.equals(db.getDbInitConnections())) {
                    response = " {initial DB Connections Updated Successfully}";
                } else {
                    error = "{failed to update initial DB connections}";
                }
                break;
            case "getVersion":
                try {
                    // get the class version number from the class version exists in the commands map
                    String command_key = CommandsMap.map("class edumsg.core.commands." + app.toLowerCase() + "." + parameters);
                    version = (double) CommandsMap.getClass(command_key).getMethod("getClassVersion").invoke(null);
                    response = "{command version :" + version + "}";
                } catch (Exception e) {
                    error = e.getMessage();
                }
                break;
            case "logPath":
                if (MyLogger.getLog_path().equals(parameters)) {
                    response = " {log file path Updated Successfully}";
                } else {
                    error = "{failed to update log file path}";
                }
                break;
            case "start":
                if (this.isRun())
                    response = "{The command applied successfully}";
                else
                    error = "{Failed to apply the command}";
                break;
            case "stop":
                if (!this.isRun())
                    response = "{The command applied successfully}";
                else
                    error = "{Failed to apply the command}";
                break;
            default:
                throw new JMSException("Wrong Command");
        }
        if (error == null) {
            this.controllerSubmit(response, cmd, correlationID);
        } else
            this.controllerHandleError(cmd, error, correlationID);
    }

    @Override
    public void run() {
        // set the initial parameters for user application
        db.initSource();
        CommandsMap.instantiate();
        cache.BgSave();
        // set the initial logger path for the micro-service in the local disk
        MyLogger.initialize(LOGGER, System.getProperty("user.dir"));

        // assign the consumers for all queues and topics that will serve the user application
        consumer = new Consumer(new ActiveMQConfig(app.toUpperCase() + "_" + cur_instance + ".INQUEUE"), this);
        cons_ctrl = new Consumer(new ActiveMQConfig(app.toUpperCase() + "_" + cur_instance + "_CONTROLLER.INQUEUE"), this);
        new subscriber(new ActiveMQConfig(app.toUpperCase()), this);
        System.out.println("App Running Successfully!");
    }

    @Override
    public void onMessage(Message message) {
        try {
            String msgTxt = ((TextMessage) message).getText();
            // the destination queue of a message decide the behaviour of the user application to handle this msg
            if (message.getJMSDestination().toString().contains("topic")) {
                // messages coming through topic determine update command
                // TODO: 23/04/2023 implement update command
            } else {
                if (message.getJMSDestination().toString().contains("CONTROLLER")) {
                    // msg coming from the controller queues
                    this.handleControllerMsg(msgTxt, message.getJMSCorrelationID());
                } else {
                    // msg coming from the end-user queues
                    this.handleMsg(msgTxt, message.getJMSCorrelationID());
                }
            }

        } catch (Exception e) {
            try {
                this.controllerHandleError("server error", e.getMessage(), message.getJMSCorrelationID());
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            } catch (JMSException ex) {
                throw new RuntimeException(ex);
            }
            e.printStackTrace();
        }
    }

    public void stop() throws JMSException {
        // to stop the app from listening to new messages we disconnect the consumer and delete the queue
        consumer.getConsumer().close();
        Connection conn = consumer.getConn();
        ((ActiveMQConnection) conn).destroyDestination((ActiveMQDestination) consumer.getQueue());
        run = false;
    }

    public void start() {
        // restart the app by create new queue
        consumer = new Consumer(new ActiveMQConfig(app.toUpperCase() + "_" + cur_instance + ".INQUEUE"), this);
        run = true;
    }

    public void exit() throws JMSException, JsonProcessingException {
        // send the response first then we close activemq conn before we peacefully exit the app
        this.controllerSubmit(app.toLowerCase() + " app shutdown successfully", "shut down", null);
        cons_ctrl.getConsumer().close();
        Connection conn = cons_ctrl.getConn();
        ((ActiveMQConnection) conn).destroyDestination((ActiveMQDestination) cons_ctrl.getQueue());
        cons_ctrl.getConn().close();
        System.exit(0);
    }

    public boolean isRun() {
        return run;
    }

    protected void handleMsg(String msg, String correlationID) {
        JsonMapper json = new JsonMapper(msg);
        HashMap<String, String> map = null;
        try {
            map = json.deserialize();
            System.out.println("RunnableClasses Class :: " + map.toString());
            LOGGER.log(Level.SEVERE, LOGGER.getName());
        } catch (JsonParseException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (JsonMappingException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        } catch (IOException e1) {
            LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
        }

        if (map != null && map.get("session_id") != null) {
            Jedis cache = this.cache.jedisCache;


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
                                app.toUpperCase() + "_" + cur_instance + ".OUTQUEUE"));
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
            map.put("app", app.toLowerCase());
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
