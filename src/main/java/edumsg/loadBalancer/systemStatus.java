package edumsg.loadBalancer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class systemStatus {
    public static CountDownLatch latch ;

    // method to reconfigure the threads for running application instance so we give occupied app instances more resources and vice versa
    public static void reconfig_resources (HashMap<String, applicationsInstance> cur_map , String app_type){
        // loop through all running instances of a specific micro-service and reconfigure the resources
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            int cur_app = Integer.parseInt(entry.getKey().split("_")[1]);
            if(entry.getValue().isRun()) {
                if (entry.getValue().calculate_capacity() <= 50) {
                    systemStatus.command_format("maxThreads", app_type, cur_app, "50");
                } else {
                    systemStatus.command_format("maxThreads", app_type, cur_app, "25");
                }
            }
        }
    }

    // method to format the body of command sent to the controller
    public static void command_format (String command, String app_type ,int app_num, String parameters){
        try {
        JsonNodeFactory nf = JsonNodeFactory.instance;
        ObjectMapper mapper = new ObjectMapper ();
        ObjectNode node = nf.objectNode();
        node.put("command", command);
        node.put("app_type", app_type);
        node.put("app_num", app_num);
        node.put("parameters", parameters);
        String json = mapper.writeValueAsString(node);
        send_command(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method to format the body of update class version command
    public static void update_command_format (String class_name, String app_type ,String url){
        try {
            JsonNodeFactory nf = JsonNodeFactory.instance;
            ObjectMapper mapper = new ObjectMapper ();
            ObjectNode node = nf.objectNode();
            node.put("command", "updateClass");
            node.put("class_name", class_name);
            node.put("app_type", app_type);
            node.put("app_num", 1);
            // to update class we send the class path to the controller and the information of all server nodes that run the app containing this class
            node.put("url", url);
            node.put("servers_list", servers_list(app_type));
            String json = mapper.writeValueAsString(node);
            send_command(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // the method to get the list of server nodes that runs the micro-services which contain the updated class
    public static String servers_list (String app_type){
        HashMap<String, applicationsInstance> cur_instance = Calculation.getUserInstances();
        switch (app_type){
            case "USER" :
                cur_instance = Calculation.getUserInstances();
                break;
            case "DM" : cur_instance = Calculation.getDMInstances();
                break;
            case "TWEET" : cur_instance = Calculation.getTweetInstances();
                break;
            case "LIST" : cur_instance = Calculation.getListInstances();
                break;
        }
        ArrayList<String> servers_arr = new ArrayList<>();
        JsonNodeFactory nf = JsonNodeFactory.instance;
        ObjectNode node = nf.objectNode();
        try {
        for (Map.Entry<String, applicationsInstance> entry : cur_instance.entrySet()) {
            ObjectMapper mapper = new ObjectMapper();
            node.put("ip", entry.getValue().getIp());
            node.put("user", entry.getValue().getUser());
            node.put("password", entry.getValue().getPassword());

            String jsonStr = mapper.writeValueAsString(node);
            servers_arr.add(jsonStr);
            System.out.println(jsonStr);
            node.removeAll();
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return String.valueOf(servers_arr);
    }

    // after formatting a request (command) we send it to the controller server
    public static void send_command (String json) throws InterruptedException {
        latch = new CountDownLatch(1);
        HttpSnoopClient.sendToControllerServer(ByteBuffer.wrap(json.getBytes(CharsetUtil.UTF_8)));
        // block until the previous HTTP request complete
        latch.await();
    }
}
