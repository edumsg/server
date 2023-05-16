package edumsg.loadBalancer;


import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Calculation {

    public static HashMap<String, applicationsInstance> userInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> DMInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> tweetInstances = new HashMap<>();
    public static HashMap<String, applicationsInstance> listInstances = new HashMap<>();
    private static HashMap<String, Long> userRequests = new HashMap<>();
    private static HashMap<String, Long> DMRequests = new HashMap<>();
    private static HashMap<String, Long> tweetRequests = new HashMap<>();
    private static HashMap<String, Long> listRequests = new HashMap<>();
    private static ExecutorService executor = Executors.newCachedThreadPool();

    // after running the system for the first time we will create the objects for the initial micro-services
    public static void initial_instances() {

        new_instance("USER", null);
        new_instance("DM", null);
        new_instance("TWEET", null);
        new_instance("LIST", null);
        new_instance("SERVER", null);
        runnable("USER");
        runnable("DM");
        runnable("TWEET");
        runnable("LIST");
        runnable("SERVER");
        runnable("UPDATE");
    }

    //this method will bw called after sending each request to the main server to get some info about this request
    public static void send_time(DefaultHttpRequest request) {
        String req_id = request.headers().get("id");
        String app_type = request.headers().get("App_Type");
        int app_num = request.headers().getInt("App_Num");
        String app_id = app_type + "_" + app_num;
        applicationsInstance cur_app;
        switch (app_type) {
            case "USER":
                userRequests.put(req_id, System.currentTimeMillis());
                cur_app = userInstances.get(app_id);
                break;
            case "DM":
                DMRequests.put(req_id, System.currentTimeMillis());
                cur_app = DMInstances.get(app_id);
                break;
            case "TWEET":
                tweetRequests.put(req_id, System.currentTimeMillis());
                cur_app = tweetInstances.get(app_id);
                break;
            case "LIST":
                listRequests.put(req_id, System.currentTimeMillis());
                cur_app = listInstances.get(app_id);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + app_type);
        }
        cur_app.increase();
        System.err.println("concurrent req..." + cur_app.getIncomplite_req());
    }

    //this method will be called after receiving each response from the main server to get some info about this response
    public static void reveive_time(DefaultHttpResponse response, long current_time) {
        String res_id = response.headers().get("id");
        String app_type = response.headers().get("App_Type");
        int app_num = Integer.parseInt(response.headers().get("App_Num"));
        String app_id = app_type + "_" + app_num;
        applicationsInstance cur_app;
        HashMap<String, Long> req_map;
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

    // against sending each HTTP request forward method will bw called to choose the micro-service with the least response time
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
            if (entry.getValue().isRun()) {
                min = entry.getValue().calculate_avg();
                break;
            }
        }
        // loop in the one of the Micro-Services instances list to pick the one with the min average response time.
        for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
            // check first if the micro-service instance is running
            if (entry.getValue().isRun()) {
                //System.out.println(entry.getValue().calculate_avg() +" ... "+ entry.getKey());
                if (entry.getValue().calculate_avg() <= min) {
                    min = entry.getValue().calculate_avg();
                    forward_to = Integer.parseInt(entry.getKey().split("_")[1]);
                }
            }
        }
        return forward_to;
    }

    // when we run a new micro-service this method will be called to initialize the object for this instance
    public static void new_instance(String app_type, String ip) {
        int last_index;
        switch (app_type.toUpperCase()) {
            case "USER":
                last_index = userInstances.size();
                String user_id = app_type.toUpperCase() + "_" + (last_index + 1);
                applicationsInstance user_app = new applicationsInstance(user_id, ip);
                userInstances.put(user_id, user_app);
                break;
            case "DM":
                last_index = DMInstances.size();
                String DM_id = app_type.toUpperCase() + "_" + (last_index + 1);
                applicationsInstance DM_app = new applicationsInstance(DM_id, ip);
                DMInstances.put(DM_id, DM_app);
                break;
            case "TWEET":
                last_index = tweetInstances.size();
                String tweet_id = app_type.toUpperCase() + "_" + (last_index + 1);
                applicationsInstance tweet_app = new applicationsInstance(tweet_id, ip);
                tweetInstances.put(tweet_id, tweet_app);
                break;
            case "LIST":
                last_index = listInstances.size();
                String list_id = app_type.toUpperCase() + "_" + (last_index + 1);
                applicationsInstance list_app = new applicationsInstance(list_id, ip);
                listInstances.put(list_id, list_app);
                break;
            case "SERVER":
                String host;
                if (ip == null) {
                    host = "127.0.0.1";
                } else {
                    host = ip;
                }
                applicationsInstance server = new applicationsInstance(host);
                HttpSnoopClient.add_server_instance(server);
                break;

        }
    }

    // assign a thread for the methods that always run in the background
    public static void runnable(String key) {
        Runnable r = () -> {
            try {
                if (key.equals("SERVER")) {
                    mainServer_scalability(key);
                } else {
                    if (key.equals("UPDATE")) {
                        check_for_updates();
                    } else {
                        apps_scalability(key);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        executor.submit(r);
    }


    // this method will check the capacity of all running micro-service instances to check the load
    public static boolean check_capacity(String app_type) {
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
            case "SERVER":
                cur_map = HttpSnoopClient.serverInstances;
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

    // a method that always runs on the background of the system to check the load of the micro-services
    // and send to the controller in case of high/low load
    public static void apps_scalability(String app_type) throws InterruptedException {
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

        // if there is more than one instance for specific app are running send to the controller to stop the one with capacity less than 25%
        if (cur_map.size() > 1) {
            for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                if (entry.getValue().calculate_capacity() < 25) {
                    if (entry.getValue().isIn_service() && entry.getValue().isRun()) {
                        int app_num = Integer.parseInt(entry.getKey().split("_")[1]);
                        systemStatus.command_format("stop", app_type, app_num, "");
                    }
                }
            }
        }
        // in case of load more than 75% either run a suspended micro-service or migrate a new one
        if (check_capacity(app_type)) {
            // trying first to run a suspended micro-service if exist
            boolean find = false;
            for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                // check if there is a suspended machines to put it in work again
                if (!entry.getValue().isRun() && !find) {
                    int app_num = Integer.parseInt(entry.getKey().split("_")[1]);
                    systemStatus.command_format("start", app_type, app_num, "");
                    find = true; // just when we put one machine back in service we will break the loop
                }
            }

            // if there is no suspended micro-service then migrate a new one
            if (!find) {
                // if we reach the limit of micro-services that we can run then we will reconfig the resources for the existing micro-services
                // i assume that the maximum num of a micro-service is 10
                if (cur_map.size() >= 10) {
                    systemStatus.reconfig_resources(cur_map, app_type);
                } else {
                    systemStatus.command_format("newInstance", app_type, (cur_map.size() + 1), "0");
                }
            }
        }

        Thread.sleep(1000 * 60);
        // each 60 sec recall this method
        apps_scalability(app_type);
    }

    public static void mainServer_scalability(String app_type) throws InterruptedException {

        HashMap<String, applicationsInstance> cur_map = HttpSnoopClient.serverInstances;

        // if there is more than one instance of the main server are running stop the one with capacity less than 25%
        if (cur_map.size() > 1) {
            for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                if (entry.getValue().calculate_capacity() < 25) {
                    if (entry.getValue().isIn_service() && entry.getValue().isRun()) {
                        entry.getValue().setRun(false);
                    }
                }
            }
        }

        // in case of load more than 75% either run a suspended main server or migrate a new one
        if (check_capacity(app_type)) {
            boolean find = false;
            for (Map.Entry<String, applicationsInstance> entry : cur_map.entrySet()) {
                // check if there is a suspended machines to put it in work again
                if (!entry.getValue().isRun() && !find) {
                    entry.getValue().setRun(true);
                    find = true; // just when we put one machine back in service we will break the loop
                }
            }
            if (!find) {
                systemStatus.command_format("newInstance", "SERVER", (cur_map.size() + 1), "0");
            }
        }
        Thread.sleep(1000 * 60);
        // each 60 sec recall this method
        mainServer_scalability(app_type);

    }

    // after applying the command and receiving a response from the contoller we reflect the command to update the system status
    public static void reflect_command(String app_id, boolean status) {
        app_id = app_id.toUpperCase();
        switch ((app_id.split("_")[0])) {
            case "USER":
                userInstances.get(app_id).setRun(status);
                break;
            case "DM":
                DMInstances.get(app_id).setRun(status);
                break;
            case "TWEET":
                tweetInstances.get(app_id).setRun(status);
                break;
            case "LIST":
                listInstances.get(app_id).setRun(status);
                break;

        }
    }

    // a method that check "update" directory in the local disk to handle update class version command
    // each time we find .java file there we complete the uodating process and then delete it
    public static void check_for_updates() throws InterruptedException {
        File file = new File("C:\\Users\\OS\\Desktop\\Edumsg-comp\\update");
        if (file.list().length != 0) {
            String class_name = (file.listFiles()[0].getName()).split("\\.")[0];
            String app_type = (file.listFiles()[0].getName()).split("\\.")[1];
            file.listFiles()[0].renameTo(new File(file, class_name + ".java"));
            System.out.println(class_name);
            System.out.println(app_type);
            systemStatus.update_command_format(class_name, app_type, file.getPath() + "\\" + class_name);
        }
        Thread.sleep(1000 * 120);
        // each 2 min recall check_for_updates
        check_for_updates();
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
