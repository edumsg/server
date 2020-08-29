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
import java.util.concurrent.TimeUnit;

public class systemStatus {
    public static CountDownLatch latch ;

    public static void send () throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);

        /*command_format("getVersion" ,"LIST", 1,"GetListCommand");
        update_command_format("GetListCommand","LIST","C:\\Users\\OS\\Desktop\\Edumsg-comp\\GetListCommand");
        command_format("getVersion" ,"LIST", 1,"GetListCommand");*/

        /*command_format("initDBConnections","LIST",1,"27");
        command_format("maxDBConnections","LIST",1,"18");
        command_format("maxThreads","LIST",1,"30");
        command_format("getVersion" ,"LIST", 1,"GetListCommand");
        command_format("initDBConnections","LIST",1,"27");
        command_format("maxDBConnections","LIST",1,"18");
        command_format("maxThreads","LIST",1,"30");
        command_format("log_path","LIST",1,"C:\\Users\\OS\\Desktop\\Edumsg-comp\\logs2");
        command_format("maxDBConnections","LIST",1,"19");
        command_format("getVersion" ,"LIST", 1,"DeleteDmCommand");
        command_format("maxDBConnections","LIST",1,"19");
        update_command_format("GetListCommand","LIST","C:\\Users\\OS\\Desktop\\Edumsg-comp\\GetListCommand");
        command_format("getVersion" ,"LIST", 1,"GetListCommand");
        command_format("maxThreads","LIST",1,"23");
        command_format("stop","LIST",1,"");
        command_format("maxThreads","LIST",1,"23");
        command_format("start","LIST",1,"");
        command_format("initDBConnections","LIST",1,"27");
        command_format("maxDBConnections","LIST",1,"18");
        command_format("shutdown","LIST",1,"");*/
        command_format("newInstance","LIST",2,"0");
    }
    // the method to reconfigure the threads for running application instance
    // so we give occupied app instances more resources and vice versa
    public static void reconfig_resources (HashMap<String, applicationsInstance> cur_map , String app_type){
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            int cur_app = Integer.parseInt(entry.getKey().split("_")[1]);
            if (entry.getValue().calculate_capacity() <= 50) {
                systemStatus.command_format("maxThreads",app_type,cur_app,"100");
            }
            else{
                systemStatus.command_format("maxThreads",app_type,cur_app,"200");
            }
        }
    }
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
    // the method to update class  or acquire class version
    public static void update_command_format (String class_name, String app_type ,String url){
        try {
            JsonNodeFactory nf = JsonNodeFactory.instance;
            ObjectMapper mapper = new ObjectMapper ();
            ObjectNode node = nf.objectNode();
            node.put("command", "updateClass");
            node.put("class_name", class_name);
            node.put("app_type", app_type);
            node.put("app_num", 1);
            // to update class we send the class path to the controller and the information of all servers that run the app containing this class
            node.put("url", url);
            node.put("servers_list", servers_list(app_type));
            String json = mapper.writeValueAsString(node);
            send_command(json);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // the metgod to get info about all running instances of specific app
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
        ArrayList<String> servers_arr = new ArrayList<String>();
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
    public static void send_command (String json) throws InterruptedException {
        latch = new CountDownLatch(1);
        HttpSnoopClient.sendToControllerServer(ByteBuffer.wrap(json.getBytes(CharsetUtil.UTF_8)));
        // block until the previous HTTP request complete
        latch.await();
    }
}
