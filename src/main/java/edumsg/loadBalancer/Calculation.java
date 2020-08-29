package edumsg.loadBalancer;


import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Calculation {

    private static HashMap<String, Long> userRequests = new HashMap<>();
    private static HashMap<String, Long> DMRequests = new HashMap<>();
    private static HashMap<String, Long> tweetRequests = new HashMap<>();
    private static HashMap<String, Long> listRequests = new HashMap<>();

    public static HashMap<String, applicationsInstance> userInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> DMInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> tweetInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> listInstances = new HashMap<>();

    private static ExecutorService executor = Executors.newCachedThreadPool();


    public static void initial_instances () throws InterruptedException {
        /*userInstances.clear();
        DMInstances.clear();
        tweetInstances.clear();
        listInstances.clear();*/
        new_instance("USER",null);
        new_instance("DM",null);
        new_instance("TWEET",null);
        new_instance("LIST",null);
        new_instance("SERVER",null);
        runnable("USER");
        runnable("DM");
        runnable("TWEET");
        runnable("LIST");
    }

    public static void send_time(DefaultHttpRequest request) {
        String req_id = request.headers().get("id");
        String app_type = request.headers().get("App_Type");
        int app_num = request.headers().getInt("App_Num");
        String app_id =app_type +"_"+ app_num;
        applicationsInstance cur_app;
        HashMap<String, applicationsInstance> cur_map;

        switch (app_type) {
            case "USER":
                userRequests.put(req_id, System.currentTimeMillis());
                cur_app = userInstances.get(app_id);
                cur_map = userInstances;
                break;
            case "DM":
                DMRequests.put(req_id, System.currentTimeMillis());
                cur_app = DMInstances.get(app_id);
                cur_map = DMInstances;
                break;
            case "TWEET":
                tweetRequests.put(req_id, System.currentTimeMillis());
                cur_app = tweetInstances.get(app_id);
                cur_map = tweetInstances;
                break;
            case "LIST":
                listRequests.put(req_id, System.currentTimeMillis());
                cur_app = listInstances.get(app_id);
                cur_map = listInstances;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        cur_app.increase();
    }

    public static void reveive_time(DefaultHttpResponse response, long current_time) {
        String res_id =response.headers().get("id");
        String app_type = response.headers().get("App_Type");
        int app_num = Integer.parseInt(response.headers().get("App_Num"));
        String app_id = app_type +"_"+ app_num;
        applicationsInstance cur_app;
        HashMap<String, Long> req_map = null;

        switch (app_type) {
            case "USER":
                req_map = userRequests;
                cur_app = userInstances.get(app_id);
                break;
            case "DM":
                req_map = DMRequests;
                cur_app = DMInstances.get(app_id);
                break;
            case "TWEET":
                req_map = tweetRequests;
                cur_app = tweetInstances.get(app_id);
                break;
            case "LIST":
                req_map = listRequests;
                cur_app = listInstances.get(app_id);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        long res_time = (current_time - (req_map.get(res_id)));
        req_map.remove(res_id);
        cur_app.add_to_instance(res_time);
        cur_app.decrease();

    }

    public static int forward(String app_type) {
        int forward_to = 1;
        double min = 0;
        HashMap<String, applicationsInstance> cur_map;
        switch (app_type) {
            case "USER":
                cur_map = userInstances;
                break;
            case "DM":
                cur_map = DMInstances;
                break;
            case "TWEET":
                cur_map = tweetInstances;
                break;
            case "LIST":
                cur_map = listInstances;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        // pick the 1st running instance to be min
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            if(entry.getValue().isRun()) {
                min = entry.getValue().calculate_avg();
                break;
            }
        }
        // loop in the one of the 4 apps instances list to pick the one with the
        // min average response time.
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            // check first if the micro-service instance is running
            if(entry.getValue().isRun()) {
                System.out.println(entry.getValue().calculate_avg() +" "+ entry.getKey());
                if (entry.getValue().calculate_avg() <= min) {
                    forward_to = Integer.parseInt(entry.getKey().split("_")[1]);
                }
            }
        }
        System.out.println("forward_to instance..."+forward_to);
        return forward_to;
    }

    public static void new_instance(String app_type , String identifiers) throws InterruptedException {
        int last_index;
        switch (app_type.toUpperCase()) {
            case "USER":
                last_index = userInstances.size();
                String user_id = app_type + "_" + (last_index + 1);
                applicationsInstance user_app = new applicationsInstance(user_id,identifiers);
                userInstances.put(user_id, user_app);
                break;
            case "DM":
                last_index = DMInstances.size();
                String DM_id = app_type + "_" + (last_index + 1);
                applicationsInstance DM_app = new applicationsInstance(DM_id,identifiers);
                DMInstances.put(DM_id, DM_app);
                break;
            case "TWEET":
                last_index = tweetInstances.size();
                String tweet_id = app_type + "_" + (last_index + 1);
                applicationsInstance tweet_app = new applicationsInstance(tweet_id,identifiers);
                tweetInstances.put(tweet_id, tweet_app);
                break;
            case "LIST":
                last_index = listInstances.size();
                String list_id = app_type + "_" + (last_index + 1);
                applicationsInstance list_app = new applicationsInstance(list_id,identifiers);
                listInstances.put(list_id, list_app);
                break;
            case "SERVER":
                String host;
                if(identifiers == null){
                    host = "172.17.165.225";
                }else{
                    JSONObject Json = new JSONObject(identifiers);
                    host = Json.getString("ip");
                }
                serverInstances server = new serverInstances(host);
                HttpSnoopClient.add_server_instance(host,server);
                break;

        }

    }
    public static void runnable(String app_type){
        Runnable r = () -> {
            try {
                refactor_resource(app_type);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        executor.submit(r);
    }
    public static boolean check_capacity (String app_type){
        HashMap<String, applicationsInstance> cur_map;
        switch (app_type) {
            case "USER":
                cur_map = userInstances;
                break;
            case "DM":
                cur_map = DMInstances;
                break;
            case "TWEET":
                cur_map = tweetInstances;
                break;
            case "LIST":
                cur_map = listInstances;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            if (entry.getValue().calculate_capacity() < 75) {
                return false;
            }
        }
        return true;
    }
    public static void refactor_resource(String app_type) throws InterruptedException {

        HashMap<String, applicationsInstance> cur_map;
        switch (app_type) {
            case "USER":
                cur_map = userInstances;
                break;
            case "DM":
                cur_map = DMInstances;
                break;
            case "TWEET":
                cur_map = tweetInstances;
                break;
            case "LIST":
                cur_map = listInstances;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        // if there is more than one instance for specific app are running stop the one with capacity less than 25%
        if (cur_map.size() > 1) {
            for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                if (entry.getValue().calculate_capacity() < 25) {
                    if (entry.getValue().isIn_service()) {
                        int app_num = Integer.parseInt(entry.getKey().split("_")[1]);
                        systemStatus.command_format("stop", app_type, app_num, "");
                    }
                }
            }
        }
        if (check_capacity(app_type)) {
            if (cur_map.size() >= 10) {
                systemStatus.reconfig_resources(cur_map, app_type);
            } else {
                boolean flag = false;
                for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                    // check if there is a suspended machines to put it in work again
                    if (!entry.getValue().isRun() && !flag) {
                        int app_num = Integer.parseInt(entry.getKey().split("_")[1]);
                        systemStatus.command_format("start", app_type, app_num, "");
                        flag = true;
                    }
                }
                if(flag)
                systemStatus.command_format("newInstance", app_type, (cur_map.size() + 1), "0");
            }
        }
        Thread.sleep(10000);
        // each 10 sec recall this method
        refactor_resource(app_type);

    }
    public static void reflect_command(String app_id, boolean status){
        switch ((app_id.split("_")[0]).toUpperCase()){
            case "USER" :
                userInstances.get(app_id).setRun(status);
                break;
            case "DM" :
                DMInstances.get(app_id).setRun(status);
                break;
            case "TWEET" :
                tweetInstances.get(app_id).setRun(status);
                break;
            case "LIST" :
                listInstances.get(app_id).setRun(status);
                break;

        }
    }

    public static HashMap<String, applicationsInstance> getUserInstances() {
        return userInstances;
    }

    public static HashMap<String, applicationsInstance> getDMInstances() {
        return DMInstances;
    }

    public static HashMap<String, applicationsInstance> getTweetInstances() {
        return tweetInstances;
    }

    public static HashMap<String, applicationsInstance> getListInstances() {
        return listInstances;
    }

}
