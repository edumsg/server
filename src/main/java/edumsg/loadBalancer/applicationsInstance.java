package edumsg.loadBalancer;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.lang.System.currentTimeMillis;


public class applicationsInstance {
    private String ip;
    private String user;
    private String password;
    private String id;
    private HashMap<Long, Long> app_response_time;
    private int incomplite_req;
    private int max_capacity;
    private boolean run;
    private boolean in_service;
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public applicationsInstance(String id,String identifiers) throws InterruptedException {
        if(identifiers == null) {
            server_props();
        }else{
            JSONObject Json = new JSONObject(identifiers);
            this.ip = Json.getString("ip");
            this.user = Json.getString("user");
            this.password = Json.getString("password");
            System.out.println("identifiers..." + ip +" "+user+" "+password);
        }
        this.id = id;
        this.app_response_time = new HashMap<>();
        this.incomplite_req = 0;
        this.max_capacity = 1000;
        this.run = true;
        this.in_service = false;
        this.runnable();
    }
    public void add_to_instance (long response_time){
        app_response_time.put(currentTimeMillis(),response_time);
        System.out.println("res in milliseconds... "+response_time);
    }

    // this method will automatically capture the requests completed in the last 5 minutes to calculate the avg res time.
    public void update_resTime() throws InterruptedException {
        for (Map.Entry<Long, Long> entry : app_response_time.entrySet()) {
            if (((currentTimeMillis() - entry.getKey()) / 1000) > 300) {
                app_response_time.remove(entry.getKey());
            }
        }
        //notify the system that this instance capacity above 50%
        if(!isIn_service() && this.calculate_capacity() > 50)
            setIn_service(true);
        Thread.sleep(10000);
        // each 10 sec recall this method
        this.update_resTime();
    }

    // calculate the avg response time for app instance by calculating the avg res time foe all
    // requests done in last 5 min
    public double calculate_avg (){
        double total = 0;
        if(app_response_time.size() > 0) {
            int i = 1;
            for (Map.Entry<Long, Long> entry : app_response_time.entrySet()) {
                System.out.println("entry" + i + "..."+entry.getValue());
                total = total + entry.getValue();
                i = i+1;
            }
            total =  total/app_response_time.size();
        }
        return total;
    }

    // calculate how app instance is occupied and return percentage
    public int calculate_capacity(){
        return (this.getIncomplite_req()/ this.max_capacity)*100;

    }
    // read the config file for the local app instance
    public void server_props(){
        File configFile = new File("ip.properties");
        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            this.ip = props.getProperty("ip");
            this.user = props.getProperty("user");
            this.password = props.getProperty("password");

            reader.close();
        } catch (FileNotFoundException ex) {
            // file does not exist
        } catch (IOException ex) {
            // I/O error
        }
    }

    // assign a thread for methods that always run in the background
    public void runnable (){
        Runnable r = new Runnable() {
            public void run() {
                try {
                    update_resTime();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        executor.submit(r);
    }


    public String getId() {
        return id;
    }
    public void increase (){
        incomplite_req  = incomplite_req + 1 ;
    }
    public void decrease (){
        incomplite_req  = incomplite_req - 1 ;
    }

    public int getIncomplite_req() {
        return incomplite_req;
    }

    public String getIp() {
        return ip;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public boolean isRun() {
        return run;
    }

    public void setRun(boolean run) {
        this.run = run;
    }

    public boolean isIn_service() {
        return in_service;
    }

    public void setIn_service(boolean in_service) {
        this.in_service = in_service;
    }
}
