package edumsg.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edumsg.activemq.ActiveMQConfig;
import edumsg.activemq.Producer;
import edumsg.concurrent.WorkerPool;
import edumsg.core.CommandsMap;
import edumsg.core.PostgresConnection;
import edumsg.logger.MyLogger;

import javax.jms.JMSException;
import java.util.logging.Logger;


public class controllerResponse {
    // check that the command applied successfully and format the response to be sent to the controller
    public static void checkCommand(String cmd , String parameters , String app_type , WorkerPool pool , PostgresConnection db , MyLogger MyLogger,
                                    String correlationID , int cur_instance , Logger LOGGER) throws JMSException, JsonProcessingException {
        double version;
        String response = null;
        String error = null;

        switch (cmd) {
            case "maxThreads":
                if(Integer.parseInt(parameters) == pool.getMaxThreads()){
                    response = " {Max Threads Updated Successfully }";
                }
                else{
                    error = "failed to update max threads";
                }
                break;
            case "maxDBConnections":
                if(parameters.equals(db.getDbMaxConnections())){
                    response = " {Max DB Connections Updated Successfully}";
                }
                else{
                    error = "failed to update max DB connections";
                }
                break;
            case "initDBConnections":
                if(parameters.equals(db.getDbInitConnections())){
                    response = " {initial DB Connections Updated Successfully}";
                }
                else{
                    error = "failed to update initial DB connections";
                }
                break;
            case"getVersion":
                try {
                    // get the class version number from the class version exists in the commands map
                    String command_key = CommandsMap.map("class edumsg.core.commands." + app_type.toLowerCase() + "." + parameters);
                    version = (double) CommandsMap.getClass(command_key).getMethod("getClassVersion").invoke(null);
                    response = "{command version :" + version + "}";
                }catch (Exception e){
                    error = e.getMessage();
                }
            break;
            case"logPath":
                if(MyLogger.getLog_path().equals(parameters)){
                    response = " {log file path Updated Successfully}";
                }
                else{
                    error = "failed to update log file path";
                }
                break;
            case"start":
                switch(app_type.toUpperCase()){
                    case "USER" :
                        if(UserMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "DM" :
                        if(DMMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "TWEET" :
                        if(TweetMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "LIST" :
                        if(ListMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                }
                break;
            case"stop":
                switch(app_type.toUpperCase()){
                    case "USER" :
                        if(!UserMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "DM" :
                        if(!DMMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "TWEET" :
                        if(!TweetMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                    case "LIST" :
                        if(!ListMain.isRun())
                            response = "{The command applied successfully}";
                        else
                            error = "{Failed to apply the command}";
                        break;
                }
                break;
            default:
                throw new JMSException("Wrong Command");
        }
        if(error == null) {
            controllerSubmit(app_type, cur_instance,response,cmd, correlationID, LOGGER);
        } else
            controllerHandleError(app_type,cur_instance,cmd,error,correlationID,LOGGER);
    }
    // format and send a response in case of an error arise
    public static void controllerHandleError(String app,int cur_instance , String method, String errorMsg,
                                             String correlationID, Logger logger) throws JsonProcessingException {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app +"_"+ cur_instance);
        node.put("method", method);
        node.put("status", "Bad Request");
        node.put("code", "400");
        node.put("message", errorMsg);

        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + "_"+ cur_instance+"_CONTROLLER.OUTQUEUE"));
        p.send(mapper.writeValueAsString(node), correlationID, logger);
    }

    // send the response of command execution process to the controller
    public static void controllerSubmit(String app,int cur_instance, String response,String cmd, String correlationID,
                                        Logger logger) throws JsonProcessingException {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        MyObjectMapper mapper = new MyObjectMapper();
        ObjectNode node = nf.objectNode();
        node.put("app", app +"_"+ cur_instance);
        node.put("msg", response);
        node.put("command", cmd);
        node.put("status", "ok");
        node.put("code", "200");

        Producer p = new Producer(new ActiveMQConfig(app.toUpperCase()
                + "_"+ cur_instance+"_CONTROLLER.OUTQUEUE"));
        p.send(mapper.writeValueAsString(node), correlationID, logger);
    }

}
