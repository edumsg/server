package edumsg.NodeManager;

import edumsg.NodeManager.NettyInstance.MainServerInstance;
import edumsg.redis.*;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class Main {
    public static HashMap<String, Cache> cacheMap = new HashMap<>();
    public static ExecutorService executor = Executors.newFixedThreadPool(15);
    public static int instancesCount = 15;

    static {
        cacheMap.put("user", new UserCache());
        cacheMap.put("dm", new DMCache());
        cacheMap.put("list", new ListCache());
        cacheMap.put("tweet", new TweetCache((UserCache) cacheMap.get("user")));
    }

    public static void main(String[] args) {
        addNewInstance("user");
        addNewInstance("list");
        addNewInstance("tweet");
        addNewInstance("dm");
        addNewInstance("server");
    }

    public static void addNewInstance(String app) {
        if (instancesCount == 0) {
            return;
        }
        if (cacheMap.containsKey(app.toLowerCase())) {
            try {
                executor.submit(new RunnableInstance(app));
                instancesCount--;
            } catch (RejectedExecutionException ex) {
                ex.printStackTrace();
            }
        } else if (app.toLowerCase().equals("server")) {
            try {
                executor.submit(new MainServerInstance());
                instancesCount--;
            } catch (RejectedExecutionException ex) {
                ex.printStackTrace();
            }
        } else {
            // TODO: 24/04/2023 handle error
        }
    }
}
