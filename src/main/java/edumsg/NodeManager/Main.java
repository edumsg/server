package edumsg.NodeManager;

import edumsg.NodeManager.NettyInstance.MainServerInstance;
import edumsg.redis.*;

import java.util.HashMap;

public class Main {
    public static HashMap<String, Cache> cacheMap = new HashMap<String, Cache>();
    public static HashMap<String, RunnableInstance> runnableInstanceHashMap;

    static {
        cacheMap.put("user", new UserCache());
        cacheMap.put("dm", new DMCache());
        cacheMap.put("list", new ListCache());
        cacheMap.put("tweet", new TweetCache((UserCache) cacheMap.get("user")));
    }

    public static void main(String[] args) {
        new RunnableInstance("user");
        new RunnableInstance("dm");
        new RunnableInstance("tweet");
        new RunnableInstance("list");
        new MainServerInstance();
    }

    public static void addNewInstance(String app) {

    }
}
