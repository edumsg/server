package edumsg.loadBalancer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.lang.System.currentTimeMillis;

// java class to create object for each one the micro-services
public class applicationsInstance {
    private String ip;
    private String user;
    private String password;
    private String id;
    private HashMap<Long, Long> app_response_time; // app_response_time used to save the response time for all completed requests through the previous 5 min
    private int incomplite_req;
    private int max_capacity;
    private boolean run;
    private boolean in_service;
    // parameters that belongs to the main server instances specially.
    private Channel channel;
    private URI uri;

    private static ExecutorService executor = Executors.newCachedThreadPool();

    // the constructor for the 4 applications
    public applicationsInstance(String id,String identifiers) {
        if(identifiers == null) {
            server_props();
        }else{
            JSONObject Json = new JSONObject(identifiers);
            this.ip = Json.getString("ip");
            this.user = Json.getString("user");
            this.password = Json.getString("password");
        }
        this.id = id;
        this.app_response_time = new HashMap<>();
        this.incomplite_req = 0;
        this.max_capacity = 100; // the max number that a miro-service can handle concurrently
        this.run = true;
        this.in_service = false;
        this.runnable();
    }

    // the constructor for the main server
    public applicationsInstance (String ip){
        this.ip = ip;
        this.uri = URI.create(System.getProperty("uri", "http://"+ip+":8080/"));
        this.max_capacity = 100;
        this.run = true;
        this.in_service = false;
        this.create_channel();
    }
    // for each new migrated main-server node we will create a separate channel to be connect it with the load balancer
    public void create_channel(){
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new HttpSnoopClientInitializer(null));

            // Make the connection attempt.
            channel = b.connect(ip, 8080).sync().channel();
            System.err.println("Channel established...");


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // once receiving a HTTP response we will save the response time for the request
    public void add_to_instance (long response_time){
        app_response_time.put(currentTimeMillis(),response_time);
        //System.out.println("res in milliseconds... "+response_time);
    }

    // this method will automatically capture the requests completed in the last 5 minutes to calculate the avg res time.
    // we will assign a thread for this method to run at the background
    public void update_resTime() throws InterruptedException {
        for (Map.Entry<Long, Long> entry : app_response_time.entrySet()) {
            if (((currentTimeMillis() - entry.getKey()) / 1000) > 300) {
                app_response_time.remove(entry.getKey()); // remove this entry once it exceeds 5 min
            }
        }
        //notify the system that this instance capacity above 50%
        if(!isIn_service() && this.calculate_capacity() > 50)
            setIn_service(true);
        Thread.sleep(30000);
        // each 30 sec recall this method
        this.update_resTime();
    }

    // calculate the avg response time for app instance by calculating the avg res time for all the requests completed in the last 5 min
    public double calculate_avg (){
        double total = 0;
        if(app_response_time.size() > 0) {
            for (Map.Entry<Long, Long> entry : app_response_time.entrySet()) {
                //System.out.println("entry" + i + "..."+entry.getValue());
                total = total + entry.getValue();
            }
            total =  total/app_response_time.size();
        }
        return total;
    }

    // calculate the current capacity of app instance by calculating the percent of incomplete req to the max capacity
    public int calculate_capacity(){
       /* if(this.id.contains("USER")) {
            System.out.println("capacity..." + (this.getIncomplite_req()*100)/this.max_capacity + "%");
        }*/
        return (this.getIncomplite_req()*100)/this.max_capacity;

    }

    // read the config file for the micro-services instance that will run locally
    public void server_props(){
        File configFile = new File("local.properties");
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

    // assign a thread for the methods that always run in the background
    public void runnable (){
        Runnable r = () -> {
            try {
                update_resTime();
            } catch (InterruptedException e) {
                e.printStackTrace();
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
    public Channel getChannel() {
        return channel;
    }

    public URI getUri() {
        return uri;
    }
}
